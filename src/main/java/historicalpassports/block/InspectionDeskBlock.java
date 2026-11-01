package historicalpassports.block;

import historicalpassports.config.PassportConfig;
import historicalpassports.passport.PassportService;
import historicalpassports.permission.OfficerPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class InspectionDeskBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(1, 0, 1, 4, 12, 15), Block.box(12, 0, 1, 15, 12, 15));
    public InspectionDeskBlock(Properties p) { super(p); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InspectionDeskEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer p) || !(level.getBlockEntity(pos) instanceof InspectionDeskEntity desk)) return InteractionResult.PASS;
        // Returns remain available even after administrators disable new submissions.
        if (p.isShiftKeyDown() && !desk.document().isEmpty()) {
            if (p.getUUID().equals(desk.depositor()) || OfficerPermissions.canStamp(p) || p.hasPermissions(2)) desk.returnToOwner();
            else PassportService.message(p, "hp.error.permission");
            return InteractionResult.CONSUME;
        }
        if (!PassportConfig.DESK.get()) { PassportService.message(p, "hp.error.desk_disabled"); return InteractionResult.CONSUME; }
        if (desk.document().isEmpty()) {
            var record = PassportService.verify(p.server, p.getItemInHand(hand));
            if (record == null || !record.holder.equals(p.getUUID())) { PassportService.message(p, "hp.error.submit_owner"); return InteractionResult.CONSUME; }
            desk.submit(p.getItemInHand(hand), p.getUUID());
            p.sendSystemMessage(Component.translatable("hp.desk.submitted"));
        }
        if (p.getUUID().equals(desk.depositor()) || OfficerPermissions.canReadRegistry(p)) PassportService.openDesk(p, desk);
        else PassportService.message(p, "hp.error.permission");
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState old, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!old.is(next.getBlock()) && level.getBlockEntity(pos) instanceof InspectionDeskEntity desk) desk.returnToOwner();
        super.onRemove(old, level, pos, next, moving);
    }
}
