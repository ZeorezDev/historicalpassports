package historicalpassports.registry;

import historicalpassports.HistoricalPassports;
import historicalpassports.block.*;
import historicalpassports.data.Countries;
import historicalpassports.item.PassportItem;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.*;

public final class ModRegistry {
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HistoricalPassports.ID);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> STAMP_SOUND = SOUNDS.register("stamp", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(HistoricalPassports.ID, "stamp")));
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HistoricalPassports.ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HistoricalPassports.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HistoricalPassports.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HistoricalPassports.ID);
    public static final Map<String, RegistryObject<PassportItem>> PASSPORTS = new LinkedHashMap<>();
    public static final RegistryObject<InspectionDeskBlock> DESK = BLOCKS.register("inspection_desk", () -> new InspectionDeskBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3).noOcclusion()));
    public static final RegistryObject<Item> DESK_ITEM = ITEMS.register("inspection_desk", () -> new BlockItem(DESK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<InspectionDeskEntity>> DESK_ENTITY = ENTITIES.register("inspection_desk", () -> BlockEntityType.Builder.of(InspectionDeskEntity::new, DESK.get()).build(null));
    public static final RegistryObject<Block> VISA_DESK = BLOCKS.register("visa_application_desk", () -> new historicalpassports.visa.VisaDeskBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3).noOcclusion()));
    public static final RegistryObject<Item> VISA_DESK_ITEM = ITEMS.register("visa_application_desk", () -> new BlockItem(VISA_DESK.get(),new Item.Properties()));
    static {
        Countries.ALL.forEach((code, country) -> PASSPORTS.put(code, ITEMS.register("passport_" + country.key(), () -> new PassportItem(country))));
    }
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("passports", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.historicalpassports"))
        .icon(() -> new ItemStack(PASSPORTS.get("TR").get()))
        .displayItems((params, output) -> { PASSPORTS.values().forEach(p -> output.accept(p.get())); output.accept(DESK_ITEM.get()); output.accept(VISA_DESK_ITEM.get()); }).build());
    public static void register(IEventBus bus) { ITEMS.register(bus); BLOCKS.register(bus); ENTITIES.register(bus); TABS.register(bus); SOUNDS.register(bus); }
    private ModRegistry() {}
}
