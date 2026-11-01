package historicalpassports.visa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import historicalpassports.data.*;
import historicalpassports.passport.*;
import java.util.*;
import static net.minecraft.commands.Commands.*;

public final class VisaCommands {
    private static boolean officer(CommandSourceStack s){return s.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && VisaData.get(s.getServer()).officers.containsKey(p.getUUID());}
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,String> word(String name){return argument(name,StringArgumentType.word());}
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,String> country(String name){return word(name).suggests((c,b)->SharedSuggestionProvider.suggest(Countries.ALL.keySet(),b));}
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,String> type(){return word("type").suggests((c,b)->SharedSuggestionProvider.suggest(VisaData.TYPES,b));}
    private static String str(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c,String n){return StringArgumentType.getString(c,n);}
    private static int num(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c,String n){return IntegerArgumentType.getInteger(c,n);}
    private static int tell(CommandSourceStack s,String key,Object...args){s.sendSuccess(()->Component.translatable("visa."+key,args),false);return 1;}
    public static void register(CommandDispatcher<CommandSourceStack> d){
        var root=literal("visa");
        root.then(literal("help").executes(c->tell(c.getSource(),"help")));
        root.then(literal("officer").requires(s->s.hasPermission(2))
            .then(literal("grant").then(argument("player",EntityArgument.player()).executes(c->{var p=EntityArgument.getPlayer(c,"player");var data=VisaData.get(c.getSource().getServer());data.officers.put(p.getUUID(),p.getGameProfile().getName());data.setDirty();c.getSource().getServer().getCommands().sendCommands(p);return tell(c.getSource(),"saved");})))
            .then(literal("revoke").then(word("player").executes(c->{var data=VisaData.get(c.getSource().getServer());String name=str(c,"player");data.officers.entrySet().removeIf(e->e.getValue().equalsIgnoreCase(name)||e.getKey().toString().equals(name));data.setDirty();c.getSource().getServer().getPlayerList().getPlayers().forEach(p->c.getSource().getServer().getCommands().sendCommands(p));return tell(c.getSource(),"saved");})))
            .then(literal("list").executes(c->tell(c.getSource(),"result",VisaData.get(c.getSource().getServer()).officers.toString()))));
        root.then(literal("issue").requires(VisaCommands::officer).then(word("passport").then(country("country").then(type().then(argument("days",IntegerArgumentType.integer(1,36500)).then(argument("entries",IntegerArgumentType.integer(-1,10000)).then(argument("stay",IntegerArgumentType.integer(1,36500)).executes(c->{
            var s=c.getSource();try{var r=PassportSavedData.get(s.getServer()).find(str(c,"passport"));var v=VisaData.get(s.getServer()).issue(r,str(c,"country").toUpperCase(Locale.ROOT),str(c,"type"),PassportService.today(),num(c,"days"),num(c,"entries"),num(c,"stay"),s.getTextName(),"");return tell(s,"issued",v.getString("id"));}catch(IllegalArgumentException e){return tell(s,"invalid");}
        }))))))));
        root.then(literal("revoke").requires(VisaCommands::officer).then(word("visa").executes(c->{var data=VisaData.get(c.getSource().getServer());var v=data.visas.get(str(c,"visa"));if(v==null)return tell(c.getSource(),"invalid");v.putBoolean("revoked",true);data.setDirty();c.getSource().getServer().getPlayerList().getPlayers().forEach(p->c.getSource().getServer().getCommands().sendCommands(p));return tell(c.getSource(),"saved");})));
        root.then(literal("inspect").then(word("visa").executes(c->{var s=c.getSource();var data=VisaData.get(s.getServer());var v=data.visas.get(str(c,"visa"));if(v==null)return tell(s,"invalid");var r=PassportSavedData.get(s.getServer()).find(v.getString("passport"));if(!officer(s)&&!s.hasPermission(2)&&!(s.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && r!=null&&r.holder.equals(p.getUUID())))return tell(s,"permission");return tell(s,"result",v.getString("id")+" / "+v.getString("country")+" / "+Component.translatable("visa.status."+data.status(v,PassportService.today())).getString());})));
        root.then(literal("check").requires(s->officer(s)||s.hasPermission(2)||s.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && historicalpassports.permission.OfficerPermissions.canStamp(p)).then(word("passport").then(country("country").executes(c->{var s=c.getSource();return tell(s,"check."+VisaData.get(s.getServer()).check(PassportSavedData.get(s.getServer()).find(str(c,"passport")),str(c,"country").toUpperCase(Locale.ROOT)));}))));
        root.then(literal("policy").requires(s->s.hasPermission(2))
            .then(literal("set").then(country("from").then(country("to").then(word("policy").suggests((c,b)->SharedSuggestionProvider.suggest(VisaData.POLICIES,b)).executes(c->{var data=VisaData.get(c.getSource().getServer());String from=str(c,"from").toUpperCase(Locale.ROOT),to=str(c,"to").toUpperCase(Locale.ROOT),policy=str(c,"policy");if(Countries.get(from)==null||Countries.get(to)==null||!VisaData.POLICIES.contains(policy))return tell(c.getSource(),"invalid");data.policies.put(from+":"+to,policy);data.setDirty();c.getSource().getServer().getPlayerList().getPlayers().forEach(p->c.getSource().getServer().getCommands().sendCommands(p));return tell(c.getSource(),"saved");})))))
            .then(literal("list").then(country("country").executes(c->{var data=VisaData.get(c.getSource().getServer());String to=str(c,"country").toUpperCase(Locale.ROOT);data.policies.forEach((k,v)->{if(k.endsWith(":"+to))tell(c.getSource(),"result",k+" = "+v);});return tell(c.getSource(),"default_free");}))));
        root.then(literal("applications").requires(VisaCommands::officer).executes(c->{var p=c.getSource().getPlayerOrException();VisaDesk.open(p,true);return 1;}));
        d.register(root);
    }
}
