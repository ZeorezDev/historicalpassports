package historicalpassports.passport;

import net.minecraft.nbt.*;
import java.util.*;

public final class PassportRecord {
    public final String id, country, holderName, authority;
    public final UUID holder, seal;
    public final long issuedDay, expiresDay;
    private boolean revoked;
    private final List<Inspection> history = new ArrayList<>();
    public PassportRecord(String id, String country, UUID holder, String holderName, String authority, long issuedDay, long expiresDay, UUID seal) {
        this.id = id; this.country = country; this.holder = holder; this.holderName = holderName; this.authority = authority;
        this.issuedDay = issuedDay; this.expiresDay = expiresDay; this.seal = seal;
    }
    public boolean revoked() { return revoked; }
    public void revoke() { revoked = true; }
    public List<Inspection> history() { return Collections.unmodifiableList(history); }
    public boolean append(Inspection inspection, int limit) {
        if (history.size() >= limit) return false;
        history.add(inspection); return true;
    }
    public String status(long today, boolean expiration) { return revoked ? "revoked" : expiration && today >= expiresDay ? "expired" : "valid"; }
    public CompoundTag save(boolean includeSeal) {
        CompoundTag t = new CompoundTag();
        t.putString("id", id); t.putString("country", country); t.putUUID("holder", holder); t.putString("holderName", holderName);
        t.putString("authority", authority); t.putLong("issuedDay", issuedDay); t.putLong("expiresDay", expiresDay); t.putBoolean("revoked", revoked);
        if (includeSeal) t.putUUID("seal", seal);
        ListTag list = new ListTag(); history.forEach(i -> list.add(i.save())); t.put("history", list);
        return t;
    }
    public static PassportRecord load(CompoundTag t) {
        PassportRecord r = new PassportRecord(t.getString("id"), t.getString("country"), t.getUUID("holder"), t.getString("holderName"), t.getString("authority"), t.getLong("issuedDay"), t.getLong("expiresDay"), t.getUUID("seal"));
        r.revoked = t.getBoolean("revoked"); t.getList("history", Tag.TAG_COMPOUND).forEach(i -> r.history.add(Inspection.load((CompoundTag)i)));
        return r;
    }
}
