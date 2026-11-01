package historicalpassports.passport;

import historicalpassports.block.InspectionDeskEntity;
import historicalpassports.config.PassportConfig;
import historicalpassports.data.*;
import historicalpassports.item.PassportItem;
import historicalpassports.network.PassportNetwork;
import historicalpassports.permission.OfficerPermissions;
import historicalpassports.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

public final class PassportService {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    public static final int PAGE_SIZE = 4;
    private static final class Session {
        final UUID nonce = UUID.randomUUID();
        final String id, dimension;
        final BlockPos desk;
        final boolean remote;
        String destination = "";
        long expires, lastAction;
        Session(ServerPlayer p, String id, BlockPos desk, boolean remote) {
            this.id = id; this.desk = desk; this.remote = remote; dimension = p.level().dimension().location().toString();
            expires = p.server.overworld().getGameTime() + 12000;
        }
    }
    public static long today() { return LocalDate.now(ZoneOffset.UTC).toEpochDay(); }
    public static ItemStack issue(MinecraftServer server, UUID holder, String name, Country country, String authority) {
        int days = country.validity_days() == 0 ? PassportConfig.VALIDITY_DAYS.get() : country.validity_days();
        String id = country.code() + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        PassportRecord r = new PassportRecord(id, country.code(), holder, name, authority, today(), today() + days, UUID.randomUUID());
        PassportSavedData.get(server).add(r);
        ItemStack stack = new ItemStack(ModRegistry.PASSPORTS.get(country.code()).get());
        CompoundTag tag = new CompoundTag(); tag.putString("id", r.id); tag.putUUID("seal", r.seal);
        tag.putString("holderName", r.holderName); tag.putLong("expiresDay", r.expiresDay); stack.addTagElement("Passport", tag);
        return stack;
    }
    public static PassportRecord verify(MinecraftServer server, ItemStack stack) {
        if (!(stack.getItem() instanceof PassportItem item)) return null;
        CompoundTag tag = stack.getTagElement("Passport");
        if (tag == null || !tag.hasUUID("seal")) return null;
        PassportRecord r = PassportSavedData.get(server).find(tag.getString("id"));
        return r != null && r.country.equals(item.country.code()) && r.seal.equals(tag.getUUID("seal")) ? r : null;
    }
    public static void openItem(ServerPlayer p, ItemStack stack) {
        PassportRecord r = verify(p.server, stack);
        if (r == null) {
            close(p.getUUID()); CompoundTag sample = new CompoundTag(); sample.putString("country", ((PassportItem)stack.getItem()).country.code());
            sample.putString("status", stack.getTagElement("Passport") == null ? "sample" : "unverified");
            PassportNetwork.send(p, sample); return;
        }
        open(p, r, null, false);
    }
    public static void openDesk(ServerPlayer p, InspectionDeskEntity desk) {
        PassportRecord r = verify(p.server, desk.document());
        if (r == null) { message(p, "hp.error.invalid"); return; }
        open(p, r, desk.getBlockPos(), false);
    }
    public static void openRegistry(ServerPlayer p, PassportRecord r) { open(p, r, null, true); }
    private static void open(ServerPlayer p, PassportRecord r, BlockPos desk, boolean remote) {
        Session s = new Session(p, r.id, desk, remote); SESSIONS.put(p.getUUID(), s); snapshot(p, s, r, 0, false);
    }
    private static boolean accessible(ServerPlayer p, Session s) {
        if (!p.isAlive() || !p.level().dimension().location().toString().equals(s.dimension)) return false;
        if (s.remote) return OfficerPermissions.canReadRegistry(p);
        if (s.desk != null) {
            if (!p.serverLevel().hasChunkAt(s.desk) || p.distanceToSqr(s.desk.getX() + .5, s.desk.getY() + .5, s.desk.getZ() + .5) > 64) return false;
            if (!(p.level().getBlockEntity(s.desk) instanceof InspectionDeskEntity desk)) return false;
            PassportRecord r = verify(p.server, desk.document());
            return r != null && r.id.equals(s.id) && (p.getUUID().equals(desk.depositor()) || OfficerPermissions.canReadRegistry(p));
        }
        // Find the same document again: changing slots is safe, handing it away invalidates access.
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            PassportRecord r = verify(p.server, p.getInventory().getItem(i));
            if (r != null && r.id.equals(s.id)) return true;
        }
        return false;
    }
    public static void action(ServerPlayer p, UUID nonce, int operation, boolean approved, boolean entry, String checkpoint, String reason, int offset) {
        Session s = SESSIONS.get(p.getUUID());
        long tick = p.server.overworld().getGameTime();
        if (s == null || !s.nonce.equals(nonce) || tick > s.expires || !accessible(p, s)) { message(p, "hp.error.session"); return; }
        if (tick - s.lastAction < 5) return;
        s.lastAction = tick;
        PassportSavedData data = PassportSavedData.get(p.server); PassportRecord r = data.find(s.id);
        if (r == null) return;
        if(operation==3) { String country=checkpoint.toUpperCase(Locale.ROOT);if(Countries.get(country)!=null){s.destination=country;snapshot(p,s,r,0,false);}return; }
        if (operation == 0) { snapshot(p, s, r, Math.max(0, offset), false); return; }
        if (operation == 2) {
            if (s.desk != null && p.level().getBlockEntity(s.desk) instanceof InspectionDeskEntity desk &&
                (p.getUUID().equals(desk.depositor()) || OfficerPermissions.canStamp(p) || p.hasPermissions(2))) {
                desk.returnToOwner(); close(p.getUUID()); message(p, "hp.desk.returned");
            }
            return;
        }
        if (operation != 1 || s.remote || !OfficerPermissions.canStamp(p)) { message(p, "hp.error.permission"); return; }
        if (s.desk != null && !PassportConfig.DESK.get()) { message(p, "hp.error.desk_disabled"); return; }
        if (approved && !r.status(today(), PassportConfig.EXPIRATION.get()).equals("valid")) { message(p, "hp.error.invalid"); return; }
        var visas=historicalpassports.visa.VisaData.get(p.server);
        if(approved && entry && s.destination.isEmpty() && !visas.policies.isEmpty()){message(p,"visa.check.country_invalid");return;}
        if(approved && entry && !s.destination.isEmpty()) {String check=visas.check(r,s.destination);if(!check.equals("valid")&&!check.equals("free")){message(p,"visa.check."+check);return;}}
        checkpoint = clean(checkpoint, 64); reason = clean(reason, 128);
        if (checkpoint.isBlank()) { message(p, "hp.error.checkpoint"); return; }
        if (s.desk != null) checkpoint += " @ " + s.dimension + " " + s.desk.toShortString();
        if (!r.append(new Inspection(p.getUUID(), p.getGameProfile().getName(), System.currentTimeMillis(), approved, entry, checkpoint, reason), PassportConfig.MAX_HISTORY.get())) {
            message(p, "hp.error.history_full"); return;
        }
        if(approved && entry && !s.destination.isEmpty() && !visas.policy(r.country,s.destination).equals("free")) {
            var v=visas.eligible(r.id,s.destination,visas.policy(r.country,s.destination).equals("special"));
            if(v!=null){v.putInt("used",v.getInt("used")+1);v.putLong("stayUntil",Math.min(v.getLong("end"),today()+v.getInt("stay")-1));visas.setDirty();}
        }
        data.setDirty();
        // Rotate after every accepted decision: replaying a packet cannot add another stamp.
        Session next = new Session(p, s.id, s.desk, false); next.destination=s.destination; next.lastAction = tick; SESSIONS.put(p.getUUID(), next);
        snapshot(p, next, r, (r.history().size() - 1) / PAGE_SIZE * PAGE_SIZE, true);
    }
    private static String clean(String value, int limit) { return value.replaceAll("[\\p{Cntrl}§]", "").strip().substring(0, Math.min(value.replaceAll("[\\p{Cntrl}§]", "").strip().length(), limit)); }
    private static void snapshot(ServerPlayer p, Session s, PassportRecord r, int offset, boolean stamped) {
        CompoundTag t = new CompoundTag();
        t.putUUID("session", s.nonce); t.putString("id", r.id); t.putString("country", r.country); t.putUUID("holder", r.holder);
        t.putString("holderName", r.holderName); t.putString("authority", r.authority); t.putLong("issuedDay", r.issuedDay); t.putLong("expiresDay", r.expiresDay);
        t.putString("status", r.status(today(), PassportConfig.EXPIRATION.get())); t.putBoolean("expirationEnabled", PassportConfig.EXPIRATION.get());
        t.putBoolean("canStamp", !s.remote && OfficerPermissions.canStamp(p)); t.putBoolean("desk", s.desk != null); t.putBoolean("stamped", stamped);
        int start = Math.min(offset / PAGE_SIZE * PAGE_SIZE, Math.max(0, (r.history().size() - 1) / PAGE_SIZE * PAGE_SIZE));
        t.putInt("offset", start); t.putInt("total", r.history().size());
        ListTag history = new ListTag(); r.history().stream().skip(start).limit(PAGE_SIZE).forEach(i -> history.add(i.save())); t.put("history", history);
        var vd=historicalpassports.visa.VisaData.get(p.server);ListTag vs=new ListTag();vd.forPassport(r.id).forEach(v->{var copy=v.copy();copy.putString("status",vd.status(v,today()));vs.add(copy);});t.put("visas",vs);
        t.putString("destination",s.destination);if(!s.destination.isEmpty())t.putString("visaCheck",vd.check(r,s.destination));
        PassportNetwork.send(p, t);
    }
    public static void returnDocument(MinecraftServer server, UUID owner, ItemStack stack) {
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) insertWithoutCreativeDeletion(p, stack);
        if (!stack.isEmpty()) PassportSavedData.get(server).mail(owner, stack);
    }
    public static void deliverMail(ServerPlayer p) {
        PassportSavedData d = PassportSavedData.get(p.server);
        Iterator<ItemStack> it = d.mailbox(p.getUUID()).iterator(); boolean changed = false;
        while (it.hasNext()) { ItemStack s = it.next(); int before = s.getCount(); insertWithoutCreativeDeletion(p, s); changed |= s.getCount() != before; if (s.isEmpty()) it.remove(); }
        if (changed) { d.setDirty(); message(p, "hp.desk.returned"); }
    }
    public static void message(ServerPlayer p, String key) { p.sendSystemMessage(Component.translatable(key)); }
    private static void insertWithoutCreativeDeletion(ServerPlayer p, ItemStack stack) {
        int slot = p.getInventory().getFreeSlot();
        if (slot >= 0) { p.getInventory().setItem(slot, stack.copy()); stack.setCount(0); p.getInventory().setChanged(); }
    }
    public static void close(UUID player) { SESSIONS.remove(player); }
    private PassportService() {}
}
