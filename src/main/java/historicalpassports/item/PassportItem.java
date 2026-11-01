package historicalpassports.item;

import historicalpassports.data.Country;
import historicalpassports.passport.PassportService;
import historicalpassports.config.PassportConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;

public final class PassportItem extends Item {
    public final Country country;
    public PassportItem(Country country) { super(new Properties().stacksTo(1)); this.country = country; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer p) PassportService.openItem(p, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        CompoundTag tag = stack.getTagElement("Passport");
        if (tag == null) return;
        var record = PassportService.verify(p.server, stack);
        // Display-only cache. Issuance, inspection and approval NEVER trust these hints.
        tag.putString("statusHint", record == null ? "unverified" : record.status(PassportService.today(), PassportConfig.EXPIRATION.get()));
        if (record != null) { tag.putString("holderName", record.holderName); tag.putLong("expiresDay", record.expiresDay); }
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("country.historicalpassports." + country.key()).withStyle(ChatFormatting.GOLD));
        CompoundTag tag = stack.getTagElement("Passport");
        if (tag == null || !tag.hasUUID("seal")) { lines.add(Component.translatable("hp.status.sample").withStyle(ChatFormatting.GRAY)); return; }
        lines.add(Component.translatable("hp.field.holder").append(": " + tag.getString("holderName")));
        lines.add(Component.translatable("hp.field.number").append(": " + tag.getString("id")));
        try { lines.add(Component.translatable("hp.field.expires").append(": " + LocalDate.ofEpochDay(tag.getLong("expiresDay")))); }
        catch (java.time.DateTimeException e) { lines.add(Component.translatable("hp.status.unverified")); }
        if (tag.contains("statusHint")) lines.add(Component.translatable("hp.tooltip.last_status", Component.translatable("hp.status." + tag.getString("statusHint"))).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("hp.tooltip.verify").withStyle(ChatFormatting.DARK_GRAY));
    }
}
