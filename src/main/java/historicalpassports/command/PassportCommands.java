package historicalpassports.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import historicalpassports.config.PassportConfig;
import historicalpassports.data.*;
import historicalpassports.passport.*;
import historicalpassports.permission.OfficerPermissions;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import static net.minecraft.commands.Commands.*;

public final class PassportCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("passport")
            .executes(c -> help(c.getSource()))
            .then(literal("help").executes(c -> help(c.getSource())))
            .then(literal("countries").executes(c -> { c.getSource().sendSuccess(() -> Component.literal(String.join(", ", Countries.ALL.keySet())), false); return 1; }))
            .then(literal("collect").executes(c -> { PassportService.deliverMail(c.getSource().getPlayerOrException()); return 1; }))
            .then(literal("issue").requires(s -> s.hasPermission(2)).then(argument("player", EntityArgument.player()).then(argument("country", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Countries.ALL.keySet(), b)).executes(c -> {
                    Country country = Countries.get(StringArgumentType.getString(c, "country"));
                    if (country == null || (country.historical() && !PassportConfig.HISTORICAL.get())) { c.getSource().sendFailure(Component.translatable("hp.error.country")); return 0; }
                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                    var stack = PassportService.issue(p.server, p.getUUID(), p.getGameProfile().getName(), country, c.getSource().getTextName());
                    String id = stack.getTagElement("Passport").getString("id");
                    PassportService.returnDocument(p.server, p.getUUID(), stack);
                    c.getSource().sendSuccess(() -> Component.translatable("hp.command.issued", p.getGameProfile().getName(), id), true); return 1;
                }))))
            .then(literal("revoke").requires(s -> s.hasPermission(2)).then(argument("passport_id", StringArgumentType.word()).executes(c -> {
                PassportSavedData data = PassportSavedData.get(c.getSource().getServer()); PassportRecord r = data.find(StringArgumentType.getString(c, "passport_id"));
                if (r == null) { c.getSource().sendFailure(Component.translatable("hp.error.not_found")); return 0; }
                r.revoke(); data.setDirty(); c.getSource().sendSuccess(() -> Component.translatable("hp.command.revoked", r.id), true); return 1;
            })))
            .then(literal("inspect").requires(s -> s.hasPermission(2) || s.getEntity() instanceof ServerPlayer p && OfficerPermissions.canReadRegistry(p))
                .then(argument("passport_id", StringArgumentType.word()).executes(c -> {
                    PassportRecord r = PassportSavedData.get(c.getSource().getServer()).find(StringArgumentType.getString(c, "passport_id"));
                    if (r == null) { c.getSource().sendFailure(Component.translatable("hp.error.not_found")); return 0; }
                    PassportService.openRegistry(c.getSource().getPlayerOrException(), r); return 1;
                })))
            .then(literal("officer").requires(s -> s.hasPermission(2))
                .then(literal("grant").then(argument("player", EntityArgument.player()).executes(c -> role(c.getSource(), EntityArgument.getPlayer(c, "player"), true))))
                .then(literal("revoke").then(argument("player", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(PassportSavedData.get(c.getSource().getServer()).officers().values(), b)).executes(c -> {
                    PassportSavedData data = PassportSavedData.get(c.getSource().getServer()); String name = StringArgumentType.getString(c, "player");
                    var id = data.officers().entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(name) || e.getKey().toString().equalsIgnoreCase(name)).map(java.util.Map.Entry::getKey).findFirst();
                    if (id.isEmpty()) { c.getSource().sendFailure(Component.translatable("hp.error.not_found")); return 0; }
                    data.officer(id.get(), name, false); c.getSource().sendSuccess(() -> Component.translatable("hp.command.officer_revoked", name), true); return 1;
                })))
                .then(literal("list").executes(c -> { var data = PassportSavedData.get(c.getSource().getServer()); c.getSource().sendSuccess(() -> Component.translatable("hp.command.officers", data.officers().toString()), false); return 1; }))));
    }
    private static int role(CommandSourceStack s, ServerPlayer p, boolean grant) {
        PassportSavedData.get(s.getServer()).officer(p.getUUID(), p.getGameProfile().getName(), grant);
        s.sendSuccess(() -> Component.translatable(grant ? "hp.command.officer_granted" : "hp.command.officer_revoked", p.getGameProfile().getName()), true); return 1;
    }
    private static int help(CommandSourceStack s) { s.sendSuccess(() -> Component.translatable("hp.command.help"), false); return 1; }
    private PassportCommands() {}
}
