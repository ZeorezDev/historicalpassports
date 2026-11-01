package historicalpassports.visa;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class VisaDeskBlock extends Block {
    public VisaDeskBlock(Properties p){super(p);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!l.isClientSide && hand==InteractionHand.MAIN_HAND) VisaDesk.open((ServerPlayer)p,p.isShiftKeyDown()&&VisaData.get(l.getServer()).officers.containsKey(p.getUUID()));
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
