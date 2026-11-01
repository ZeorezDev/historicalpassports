package historicalpassports.data;

import historicalpassports.passport.PassportRecord;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class PassportSavedData extends SavedData {
    private final Map<String, PassportRecord> passports = new LinkedHashMap<>();
    private final Map<UUID, String> officers = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> mail = new HashMap<>();
    public static PassportSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PassportSavedData::load, PassportSavedData::new, "historicalpassports");
    }
    public PassportRecord find(String id) { return passports.get(id); }
    public void add(PassportRecord r) {
        if (passports.putIfAbsent(r.id, r) != null) throw new IllegalArgumentException("Duplicate identity");
        setDirty();
    }
    public boolean isOfficer(UUID id) { return officers.containsKey(id); }
    public Map<UUID, String> officers() { return Collections.unmodifiableMap(officers); }
    public void officer(UUID id, String name, boolean grant) { if (grant) officers.put(id, name); else officers.remove(id); setDirty(); }
    public void mail(UUID owner, ItemStack stack) { mail.computeIfAbsent(owner, id -> new ArrayList<>()).add(stack.copy()); setDirty(); }
    public List<ItemStack> mailbox(UUID owner) { return mail.computeIfAbsent(owner, id -> new ArrayList<>()); }
    @Override public CompoundTag save(CompoundTag root) {
        root.putInt("schema", 1);
        ListTag docs = new ListTag(); passports.values().forEach(p -> docs.add(p.save(true))); root.put("passports", docs);
        ListTag roles = new ListTag(); officers.forEach((id, name) -> { CompoundTag t = new CompoundTag(); t.putUUID("id", id); t.putString("name", name); roles.add(t); }); root.put("officers", roles);
        ListTag letters = new ListTag(); mail.forEach((id, stacks) -> stacks.forEach(stack -> { CompoundTag t = new CompoundTag(); t.putUUID("owner", id); t.put("item", stack.save(new CompoundTag())); letters.add(t); })); root.put("mail", letters);
        return root;
    }
    public static PassportSavedData load(CompoundTag root) {
        PassportSavedData d = new PassportSavedData();
        root.getList("passports", Tag.TAG_COMPOUND).forEach(t -> { PassportRecord r = PassportRecord.load((CompoundTag)t); d.passports.put(r.id, r); });
        root.getList("officers", Tag.TAG_COMPOUND).forEach(v -> { CompoundTag t = (CompoundTag)v; d.officers.put(t.getUUID("id"), t.getString("name")); });
        root.getList("mail", Tag.TAG_COMPOUND).forEach(v -> { CompoundTag t = (CompoundTag)v; d.mail.computeIfAbsent(t.getUUID("owner"), id -> new ArrayList<>()).add(ItemStack.of(t.getCompound("item"))); });
        return d;
    }
}
