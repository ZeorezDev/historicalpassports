package historicalpassports.network;

import historicalpassports.HistoricalPassports;
import historicalpassports.passport.PassportService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.UUID;
import java.util.function.Consumer;

public final class PassportNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(HistoricalPassports.ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    // Installed exclusively by the client lifecycle subscriber; no common class references client types.
    public static Consumer<CompoundTag> clientReceiver = t -> {};
    public record Snapshot(CompoundTag data) {}
    public record Action(UUID session, int operation, boolean approved, boolean entry, String checkpoint, String reason, int offset) {
        public static Action decode(FriendlyByteBuf b) { return new Action(b.readUUID(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readUtf(64), b.readUtf(128), b.readVarInt()); }
        public void encode(FriendlyByteBuf b) { b.writeUUID(session); b.writeVarInt(operation); b.writeBoolean(approved); b.writeBoolean(entry); b.writeUtf(checkpoint, 64); b.writeUtf(reason, 128); b.writeVarInt(offset); }
    }
    public record VisaAction(UUID nonce,int op,String country,String type,String purpose,int days,int entries,int stay,int startDelay,String application,int index) {}
    public static void register() {
        CHANNEL.messageBuilder(VisaAction.class,2,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeUUID(m.nonce);b.writeVarInt(m.op);b.writeUtf(m.country,2);b.writeUtf(m.type,20);b.writeUtf(m.purpose,128);b.writeVarInt(m.days);b.writeVarInt(m.entries);b.writeVarInt(m.stay);b.writeVarInt(m.startDelay);b.writeUtf(m.application,64);b.writeVarInt(m.index);})
            .decoder(b->new VisaAction(b.readUUID(),b.readVarInt(),b.readUtf(2),b.readUtf(20),b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(64),b.readVarInt()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)historicalpassports.visa.VisaDesk.action(p,m.nonce,m.op,m.country,m.type,m.purpose,m.days,m.entries,m.stay,m.startDelay,m.application,m.index);}).add();
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m, b) -> b.writeNbt(m.data)).decoder(b -> new Snapshot(b.readNbt()))
            .consumerMainThread((m, ctx) -> { if (m.data != null) clientReceiver.accept(m.data); }).add();
        CHANNEL.messageBuilder(Action.class, 1, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Action::encode).decoder(Action::decode)
            .consumerMainThread((m, ctx) -> { ServerPlayer p = ctx.get().getSender(); if (p != null) PassportService.action(p, m.session, m.operation, m.approved, m.entry, m.checkpoint, m.reason, m.offset); }).add();
    }
    public static void send(ServerPlayer p, CompoundTag data) {
        if (p.connection != null && p.connection.connection.isConnected()) CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Snapshot(data));
    }
    private PassportNetwork() {}
}
