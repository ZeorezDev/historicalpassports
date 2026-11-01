package historicalpassports.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import historicalpassports.HistoricalPassports;
import historicalpassports.config.PassportConfig;
import historicalpassports.data.*;
import historicalpassports.network.PassportNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import historicalpassports.registry.ModRegistry;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Document canvas scales uniformly, including mouse hitboxes and editable controls. */
public final class PassportScreen extends Screen {
    private static final int W = 440, H = 360, INK = 0xff3e4036, MUTED = 0xff7a7968;
    private CompoundTag data;
    private int page, visaIndex;
    private EditBox destination;
    private float scale = 1, originX, originY;
    private EditBox checkpoint, reason;
    private boolean entry = true;
    private long stampAt;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withZone(ZoneOffset.UTC);
    public PassportScreen(CompoundTag data) { super(Component.translatable("hp.title")); this.data = data; }
    public String documentId() { return data.getString("id"); }
    private static ResourceLocation texture(String path) { return new ResourceLocation(HistoricalPassports.ID, "textures/" + path + ".png"); }
    private net.minecraft.network.chat.MutableComponent tr(String key) { return Component.translatable(key); }
    public void update(CompoundTag next) {
        data = next;
        if (data.getBoolean("stamped")) {
            page = 1; stampAt = System.currentTimeMillis();
            if (PassportConfig.SOUNDS.get() && minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.STAMP_SOUND.get(), 1f, 1f));
        }
        rebuildWidgets();
    }
    @Override protected void init() {
        scale = Math.min(1.5f, Math.min((width-12f)/W, (height-12f)/H));
        originX = (width-W*scale)/2; originY = (height-H*scale)/2;
        String checkpointValue = checkpoint == null ? "" : checkpoint.getValue(), reasonValue = reason == null ? "" : reason.getValue();
        String[] tabs = {"identity", "stamps", "history", "visas"};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            Button b = button("hp.tab." + tabs[i], 8 + i*108, 2, 104, () -> { page = index; rebuildWidgets(); }); b.active = page != i;
        }
        if (page == 1 || page == 2) {
            button("hp.prev", 24, 225, 72, () -> requestPage(data.getInt("offset")-4)).active = data.getInt("offset") > 0;
            button("hp.next", 344, 225, 72, () -> requestPage(data.getInt("offset")+4)).active = data.getInt("offset")+4 < data.getInt("total");
        }
        if(page==3){button("hp.prev",24,225,72,()->{visaIndex--;rebuildWidgets();}).active=visaIndex>0;button("hp.next",344,225,72,()->{visaIndex++;rebuildWidgets();}).active=visaIndex+1<data.getList("visas",10).size();}
        if (data.getBoolean("canStamp")) {
            button(entry ? "hp.entry" : "hp.exit", 10, 269, 85, () -> { entry = !entry; rebuildWidgets(); });
            checkpoint = new EditBox(font, 102, 269, 154, 18, tr("hp.checkpoint")); checkpoint.setMaxLength(64); checkpoint.setValue(checkpointValue); addRenderableWidget(checkpoint);
            reason = new EditBox(font, 264, 269, 165, 18, tr("hp.reason")); reason.setMaxLength(128); reason.setValue(reasonValue); addRenderableWidget(reason);
            button("hp.approve", 102, 294, 154, () -> stamp(true)).active = data.getString("status").equals("valid");
            button("hp.deny", 264, 294, 165, () -> stamp(false));
        }
        if(data.getBoolean("canStamp")){destination=new EditBox(font,190,318,40,18,tr("visa.country"));destination.setMaxLength(2);destination.setValue(data.getString("destination"));addRenderableWidget(destination);button("visa.check",235,318,105,()->PassportNetwork.CHANNEL.sendToServer(new PassportNetwork.Action(data.getUUID("session"),3,false,true,destination.getValue(),"",0)));}
        if (data.getBoolean("desk")) button("hp.return", 10, 318, 170, () -> { send(2, false, 0); onClose(); });
        button("hp.close", 350, 318, 80, this::onClose);
    }
    private Button button(String key, int x, int y, int w, Runnable click) {
        return addRenderableWidget(Button.builder(tr(key), b -> click.run()).bounds(x,y,w,18).build());
    }
    private void stamp(boolean approved) { if (checkpoint != null && !checkpoint.getValue().isBlank()) send(1, approved, 0); else if (checkpoint != null) setFocused(checkpoint); }
    private void requestPage(int offset) { send(0, false, Math.max(0,offset)); }
    private void send(int operation, boolean approved, int offset) {
        if (!data.hasUUID("session")) return;
        PassportNetwork.CHANNEL.sendToServer(new PassportNetwork.Action(data.getUUID("session"), operation, approved, entry,
            checkpoint == null ? "" : checkpoint.getValue(), reason == null ? "" : reason.getValue(), offset));
    }
    private void label(GuiGraphics g, String key, String value, int x, int y, int w) {
        g.drawString(font,tr(key),x,y,MUTED,false);
        wrapped(g, value, x, y+11, w, 2, INK);
    }
    private void wrapped(GuiGraphics g, String value, int x, int y, int w, int maxLines, int color) {
        var lines = font.split(Component.literal(value), w);
        for(int i=0;i<Math.min(lines.size(),maxLines);i++) g.drawString(font,lines.get(i),x,y+i*10,color,false);
    }
    private String date(String key) { return LocalDate.ofEpochDay(data.getLong(key)).toString(); }
    private String countryName(Country c) {
        String lang = PassportConfig.LANGUAGE.get();
        if (lang.equals("auto")) lang = minecraft.getLanguageManager().getSelected();
        return lang.equals("tr_tr") ? c.name_tr() : c.name_en();
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.pose().pushPose(); g.pose().translate(originX,originY,0);g.pose().scale(scale,scale,1);
        int mx=(int)((mouseX-originX)/scale),my=(int)((mouseY-originY)/scale);
        g.fill(0,252,W,H,0xd9192928);
        RenderSystem.enableBlend();
        g.blit(texture("gui/booklet"),0,26,0,0,W,224,W,224);
        Country c = Countries.get(data.getString("country"));
        if (c != null) {
            var countryLines = font.split(Component.literal(countryName(c)), 190);
            for (int i=0;i<Math.min(2,countryLines.size());i++)
                g.drawString(font,countryLines.get(i),110-font.width(countryLines.get(i))/2,40+i*10,INK,false);
            g.drawString(font,tr("hp.title"),330-font.width(tr("hp.title"))/2,40,INK,false);
            if (page == 0) identity(g,c);
            else if (page == 1 || page == 2) history(g);
            else visas(g);
        }
        if (data.getBoolean("canStamp")) {
            g.drawString(font,tr("hp.checkpoint"),102,257,0xffd2c49e,false);
            g.drawString(font,tr("hp.reason"),264,257,0xffd2c49e,false);
        }
        if(!data.getString("destination").isEmpty())g.drawString(font,font.plainSubstrByWidth(data.getString("destination")+": "+tr("visa.check."+data.getString("visaCheck")).getString(),420),10,344,0xffead8ac,false);
        super.render(g,mx,my,partialTick);
        if (page == 1 || page == 2) historyTooltip(g, mx, my);
        if(page==3 && mx>=22 && mx<=206 && my>=65 && my<=216 && !data.getList("visas",10).isEmpty()) {var v=data.getList("visas",10).getCompound(visaIndex);var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();for(String value:List.of(v.getString("id"),v.getString("passport"),tr("visa.purpose").getString()+": "+v.getString("purpose"),tr("visa.officer").getString()+": "+v.getString("officer")))lines.addAll(font.split(Component.literal(value),230));g.renderTooltip(font,lines,mx,my);}
        g.pose().popPose();
    }
    private void visas(GuiGraphics g) {
        ListTag all=data.getList("visas",10);if(all.isEmpty()){wrapped(g,tr("visa.empty").getString(),30,85,175,5,INK);return;}
        visaIndex=Math.max(0,Math.min(visaIndex,all.size()-1));var v=all.getCompound(visaIndex);
        g.blit(texture("gui/visa_label"),22,65,0,0,184,151,184,151);
        String type=v.getString("type");int color=type.equals("diplomatic")?0xff8b6d24:0xff000000 | (type.hashCode() & 0x7f7f7f);g.fill(26,69,202,86,color);
        g.drawString(font,font.plainSubstrByWidth(tr("visa.type."+type).getString(),166),30,74,0xfffaf5de,false);
        Country c=Countries.get(v.getString("country"));if(c!=null){g.blit(texture("item/passport_"+c.key()),30,92,0,0,32,32,32,32);wrapped(g,countryName(c),68,96,130,2,INK);}
        label(g,"visa.number",v.getString("id"),30,130,168);label(g,"visa.status",tr("visa.status."+v.getString("status")).getString(),30,173,168);
        java.util.Locale locale=minecraft.getLanguageManager().getSelected().equals("tr_tr")?java.util.Locale.forLanguageTag("tr-TR"):java.util.Locale.US;
        var fmt=DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT).withLocale(locale);
        String[] keys={"holder","passport","issued_date","validity","entries","stay","officer"};
        String[] values={v.getString("holder"),v.getString("passport"),LocalDate.ofEpochDay(v.getLong("issued")).format(fmt),LocalDate.ofEpochDay(v.getLong("start")).format(fmt)+" - "+LocalDate.ofEpochDay(v.getLong("end")).format(fmt),v.getInt("used")+" / "+(v.getInt("entries")==-1?"∞":v.getInt("entries")),""+v.getInt("stay"),v.getString("officer")};
        for(int i=0;i<keys.length;i++){g.drawString(font,tr("visa."+keys[i]),238,62+i*23,MUTED,false);g.pose().pushPose();g.pose().translate(238,72+i*23,0);g.pose().scale(.65f,.65f,1);g.drawString(font,font.plainSubstrByWidth(values[i],268),0,0,INK,false);g.pose().popPose();}
    }
    private void identity(GuiGraphics g, Country c) {
        RenderSystem.enableBlend();
        g.blit(texture("item/passport_"+c.key()),120,62,0,0,80,80,80,80);
        ResourceLocation skin = null;
        if (data.hasUUID("holder") && minecraft.getConnection() != null) {
            var info = minecraft.getConnection().getPlayerInfo(data.getUUID("holder")); if (info != null) skin = info.getSkinLocation();
        }
        g.fill(28,65,94,131,0xffa59778);
        if (skin == null) g.blit(texture("gui/portrait"),31,68,0,0,60,60,60,60);
        else {
            g.blit(skin,31,68,60,60,8,8,8,8,64,64); g.blit(skin,31,68,60,60,40,8,8,8,64,64);
        }
        boolean official = data.contains("id");
        if (!official) { wrapped(g,tr("hp.sample.info").getString(),238,80,175,10,INK); }
        else {
            label(g,"hp.field.holder",data.getString("holderName"),27,145,180);
            label(g,"hp.field.nationality",countryName(c),27,177,180);
            label(g,"hp.field.number",data.getString("id"),238,63,176);
            label(g,"hp.field.issued",date("issuedDay"),238,103,176);
            label(g,"hp.field.expires",date("expiresDay"),238,133,176);
            label(g,"hp.field.authority",data.getString("authority"),238,163,176);
            g.drawString(font,tr("hp.field.uuid"),238,195,MUTED,false);
            g.pose().pushPose();g.pose().translate(238,207,0);g.pose().scale(.7f,.7f,1);
            g.drawString(font,data.getUUID("holder").toString(),0,0,INK,false);g.pose().popPose();
        }
        String status=data.getString("status"); int color=status.equals("valid")?0xff296947:0xff963d38;
        wrapped(g,tr("hp.status."+status).getString(),27,211,180,2,color);
        if (official && !data.getBoolean("expirationEnabled")) { g.pose().pushPose();g.pose().translate(238,231,0);g.pose().scale(.7f,.7f,1);g.drawString(font,tr("hp.expiration.disabled"),0,0,MUTED,false);g.pose().popPose(); }
    }
    private void history(GuiGraphics g) {
        ListTag records=data.getList("history",Tag.TAG_COMPOUND);
        if (records.isEmpty()) wrapped(g,tr("hp.history.empty").getString(),30,90,170,5,INK);
        for (int i=0;i<records.size();i++) {
            CompoundTag r=records.getCompound(i);int x=i%2==0?28:242,y=66+(i/2)*78;
            boolean ok=r.getBoolean("approved"); int ink=ok?0xff2e714f:0xffa13b38;
            if(page==1) {
                g.pose().pushPose();g.pose().translate(x+80,y+22,0);
                float angle = i%2==0 ? -4f : 3f;
                if(i==records.size()-1 && PassportConfig.ANIMATIONS.get()) {
                    float remaining=Math.max(0,1-(System.currentTimeMillis()-stampAt)/400f);float zoom=1+remaining*.3f;g.pose().scale(zoom,zoom,1);
                }
                g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
                String languageSuffix = minecraft.getLanguageManager().getSelected().equals("tr_tr") ? "_tr" : "";
                RenderSystem.enableBlend();g.blit(texture("gui/"+(ok?"approved":"denied")+languageSuffix),-64,-24,0,0,128,48,128,48);
                String date = DATE.format(Instant.ofEpochMilli(r.getLong("time")));
                g.drawString(font, date, -font.width(date)/2, 9, ink, false);
                g.pose().popPose();
                g.drawString(font,tr(r.getBoolean("entry")?"hp.entry":"hp.exit"),x,y+49,ink,false);
                g.drawString(font,font.plainSubstrByWidth(r.getString("officerName"),112),x+55,y+49,INK,false);
                g.drawString(font,font.plainSubstrByWidth(r.getString("checkpoint"),166),x,y+61,MUTED,false);
            } else {
                g.drawString(font,tr(ok?"hp.approved":"hp.denied").append(" / ").append(tr(r.getBoolean("entry")?"hp.entry":"hp.exit")),x,y,ink,false);
                g.drawString(font,DATE.format(Instant.ofEpochMilli(r.getLong("time")))+" · "+r.getString("officerName"),x,y+12,INK,false);
                g.pose().pushPose();g.pose().translate(x,y+24,0);g.pose().scale(.65f,.65f,1);g.drawString(font,r.getUUID("officer").toString(),0,0,MUTED,false);g.pose().popPose();
                wrapped(g,r.getString("checkpoint"),x,y+34,165,2,INK);wrapped(g,r.getString("reason"),x,y+55,165,2,MUTED);
            }
        }
        int total=data.getInt("total"),start=data.getInt("offset");
        g.drawCenteredString(font,Component.translatable("hp.history.count",total==0?0:start+1,Math.min(start+4,total),total),220,230,INK);
    }
    private void historyTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ListTag history = data.getList("history", Tag.TAG_COMPOUND);
        for (int i = 0; i < history.size(); i++) {
            int x = i % 2 == 0 ? 28 : 242, y = 66 + (i / 2) * 78;
            if (mouseX < x || mouseX > x + 166 || mouseY < y || mouseY > y + 72) continue;
            CompoundTag r = history.getCompound(i);
            List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
            for (String value : List.of(Instant.ofEpochMilli(r.getLong("time")).toString(),
                tr("hp.officer").getString() + ": " + r.getString("officerName"), r.getUUID("officer").toString(),
                r.getString("checkpoint"), r.getString("reason"))) lines.addAll(font.split(Component.literal(value), 200));
            g.renderTooltip(font, lines, mouseX, mouseY);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button) { return super.mouseClicked((x-originX)/scale,(y-originY)/scale,button); }
    @Override public boolean mouseReleased(double x,double y,int button) { return super.mouseReleased((x-originX)/scale,(y-originY)/scale,button); }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) { return super.mouseDragged((x-originX)/scale,(y-originY)/scale,button,dx/scale,dy/scale); }
    @Override public boolean mouseScrolled(double x,double y,double delta) { return super.mouseScrolled((x-originX)/scale,(y-originY)/scale,delta); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() { if(checkpoint!=null)checkpoint.tick();if(reason!=null)reason.tick(); }
}
