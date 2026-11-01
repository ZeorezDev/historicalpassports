package historicalpassports.client.gui;

import historicalpassports.network.PassportNetwork;
import historicalpassports.visa.VisaData;
import historicalpassports.config.PassportConfig;
import historicalpassports.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Fixed document coordinates with uniformly transformed pointer events. */
public final class VisaScreen extends Screen {
    private CompoundTag data;
    private EditBox country,purpose,days,entries,stay,delay;
    private int type;
    private float scale,ox,oy;
    private long decisionTime;
    private String decision="";
    public VisaScreen(CompoundTag t){super(Component.translatable("visa.application"));data=t;}
    private Component tr(String k){return Component.translatable("visa."+k);}
    public void update(CompoundTag t){data=t;String r=t.getString("result");if(r.equals("approved")||r.equals("denied")){decision=r;decisionTime=System.currentTimeMillis();if(PassportConfig.SOUNDS.get())minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.STAMP_SOUND.get(),1,1));}rebuildWidgets();}
    private EditBox field(String key,String value,int x,int y,int w,int max){var e=new EditBox(font,x,y,w,18,tr(key));e.setMaxLength(max);e.setValue(value);addRenderableWidget(e);return e;}
    private Button button(String key,int x,int y,int w,Runnable r){return addRenderableWidget(Button.builder(tr(key),b->r.run()).bounds(x,y,w,18).build());}
    @Override protected void init(){
        scale=Math.min(1.5f,Math.min((width-8f)/440,(height-8f)/330));ox=(width-440*scale)/2;oy=(height-330*scale)/2;
        boolean officer=data.getBoolean("officer");var a=data.getCompound("application");type=Math.max(0,VisaData.TYPES.indexOf(a.getString("type")));
        country=field("country",a.getString("country"),24,101,110,2);country.setEditable(!officer);
        button("type."+VisaData.TYPES.get(type),160,101,252,()->{type=(type+1)%VisaData.TYPES.size();((Button)children().get(1)).setMessage(tr("type."+VisaData.TYPES.get(type)));});
        purpose=field("purpose",officer?"":a.getString("purpose"),24,143,388,128);
        stay=field("stay",a.contains("stay")?""+a.getInt("stay"):"30",24,187,84,5);
        days=field("days","90",124,187,84,5);days.setEditable(officer);
        entries=field("entries","1",224,187,84,5);entries.setEditable(officer);
        delay=field("delay","0",324,187,88,5);delay.setEditable(officer);
        if(officer){button("approve",24,240,185,()->send(2,0)).active=!a.isEmpty();button("deny",224,240,188,()->send(3,0)).active=!a.isEmpty();button("prev",24,280,80,()->send(0,-1));button("next",114,280,80,()->send(0,1));}
        else button("submit",24,240,388,()->send(1,0));
        button("close",324,280,88,this::onClose);
    }
    private int number(EditBox e){try{return Integer.parseInt(e.getValue());}catch(NumberFormatException ex){return Integer.MIN_VALUE;}}
    private void send(int op,int step){PassportNetwork.CHANNEL.sendToServer(new PassportNetwork.VisaAction(data.getUUID("session"),op,country.getValue(),VisaData.TYPES.get(type),purpose.getValue(),number(days),number(entries),number(stay),number(delay),data.getCompound("application").getString("id"),data.getInt("index")+step));}
    @Override public void render(GuiGraphics g,int x,int y,float dt){renderBackground(g);g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale(scale,scale,1);g.fill(0,0,440,330,0xffe5ddbc);g.fill(10,10,430,34,0xff244d50);g.drawString(font,tr(data.getBoolean("officer")?"review":"application"),24,18,0xfff8eccb,false);
        var a=data.getBoolean("officer")?data.getCompound("application"):data;
        g.drawString(font,font.plainSubstrByWidth(a.getString("holder")+" / "+a.getString("passport"),388),24,46,0xff303c3a,false);
        java.util.Locale locale=minecraft.getLanguageManager().getSelected().equals("tr_tr")?java.util.Locale.forLanguageTag("tr-TR"):java.util.Locale.US;
        if(a.contains("date")){String date=java.time.LocalDate.ofEpochDay(a.getLong("date")).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT).withLocale(locale));g.drawString(font,a.getString("nationality")+" / "+date+" / "+(data.getInt("index")+1)+" / "+data.getInt("total"),24,78,0xff303c3a,false);}
        String details=data.getBoolean("officer")?a.getString("purpose"):a.getString("country");g.drawString(font,font.plainSubstrByWidth(details,388),24,65,0xff303c3a,false);
        String[] labels={"country","type","purpose","stay","days","entries","delay"};int[] xs={24,160,24,24,124,224,324},ys={87,87,129,173,173,173,173};for(int i=0;i<labels.length;i++)g.drawString(font,tr(labels[i]),xs[i],ys[i],0xff344b4a,false);
        if(!data.getString("result").isEmpty())g.drawString(font,tr(data.getString("result")),24,216,0xff66452d,false);
        g.drawString(font,tr("unlimited"),24,311,0xff344b4a,false);
        super.render(g,(int)((x-ox)/scale),(int)((y-oy)/scale),dt);
        if(!decision.isEmpty()&&System.currentTimeMillis()-decisionTime<1500){float z=PassportConfig.ANIMATIONS.get()?1+Math.max(0,1-(System.currentTimeMillis()-decisionTime)/350f)*.3f:1;g.pose().pushPose();g.pose().translate(300,70,0);g.pose().scale(z,z,1);String suffix=minecraft.getLanguageManager().getSelected().equals("tr_tr")?"_tr":"";g.blit(new ResourceLocation("historicalpassports","textures/gui/"+decision+suffix+".png"),-64,-24,0,0,128,48,128,48);g.pose().popPose();}g.pose().popPose();}
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked((x-ox)/scale,(y-oy)/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased((x-ox)/scale,(y-oy)/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged((x-ox)/scale,(y-oy)/scale,b,dx/scale,dy/scale);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){for(var e: new EditBox[]{country,purpose,days,entries,stay,delay})if(e!=null)e.tick();}
}
