package historicalpassports.block;

import historicalpassports.passport.PassportService;
import historicalpassports.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

/** Escrow, deliberately NOT exposed through an inventory menu or hopper capability. */
public final class InspectionDeskEntity extends BlockEntity {
    private ItemStack document = ItemStack.EMPTY;
    private UUID depositor;
    public InspectionDeskEntity(BlockPos pos, BlockState state) { super(ModRegistry.DESK_ENTITY.get(), pos, state); }
    public ItemStack document() { return document; }
    public UUID depositor() { return depositor; }
    public boolean submit(ItemStack stack, UUID owner) {
        if (!document.isEmpty() || stack.isEmpty()) return false;
        document = stack.split(1); depositor = owner; setChanged(); return true;
    }
    public void returnToOwner() {
        if (level == null || level.isClientSide || document.isEmpty() || depositor == null) return;
        ItemStack stack = document; UUID owner = depositor;
        document = ItemStack.EMPTY; depositor = null; setChanged();
        PassportService.returnDocument(level.getServer(), owner, stack);
    }
    @Override protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t); t.put("document", document.save(new CompoundTag()));
        if (depositor != null) t.putUUID("depositor", depositor);
    }
    @Override public void load(CompoundTag t) {
        super.load(t); document = ItemStack.of(t.getCompound("document")); depositor = t.hasUUID("depositor") ? t.getUUID("depositor") : null;
    }
}
