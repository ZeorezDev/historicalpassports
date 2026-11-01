package historicalpassports.passport;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public record Inspection(UUID officer, String officerName, long time, boolean approved, boolean entry, String checkpoint, String reason) {
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("officer", officer); t.putString("officerName", officerName); t.putLong("time", time);
        t.putBoolean("approved", approved); t.putBoolean("entry", entry); t.putString("checkpoint", checkpoint); t.putString("reason", reason);
        return t;
    }
    public static Inspection load(CompoundTag t) {
        return new Inspection(t.getUUID("officer"), t.getString("officerName"), t.getLong("time"), t.getBoolean("approved"), t.getBoolean("entry"), t.getString("checkpoint"), t.getString("reason"));
    }
}
