package historicalpassports.visa;

import historicalpassports.data.*;
import historicalpassports.passport.*;
import historicalpassports.network.PassportNetwork;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class VisaDesk {
    private record Session(UUID nonce,net.minecraft.core.BlockPos pos,String dimension,long expires,boolean officer){}
    private static final Map<UUID,Session> sessions=new HashMap<>();
    public static void open(ServerPlayer p,boolean officer){
        if(officer && !VisaData.get(p.server).officers.containsKey(p.getUUID()))return;
        var s=new Session(UUID.randomUUID(),p.blockPosition(),p.level().dimension().location().toString(),p.server.overworld().getGameTime()+12000,officer);sessions.put(p.getUUID(),s);snapshot(p,s,0,"");
    }
    private static void snapshot(ServerPlayer p,Session s,int index,String result){
        var data=VisaData.get(p.server);CompoundTag t=new CompoundTag();t.putString("screen","visa");t.putUUID("session",s.nonce);t.putBoolean("officer",s.officer);t.putString("result",result);
        if(s.officer){var all=data.applications.values().stream().filter(a->a.getString("status").equals("pending")).toList();int i=Math.max(0,Math.min(index,all.size()-1));t.putInt("index",i);t.putInt("total",all.size());if(!all.isEmpty())t.put("application",all.get(i).copy());}
        else {var r=PassportService.verify(p.server,p.getMainHandItem());if(r!=null&&r.holder.equals(p.getUUID())){t.putString("passport",r.id);t.putString("holder",r.holderName);t.putString("country",r.country);}}
        PassportNetwork.send(p,t);
    }
    public static void action(ServerPlayer p,UUID nonce,int op,String country,String type,String purpose,int days,int entries,int stay,int startDelay,String application,int index){
        Session s=sessions.get(p.getUUID());if(s==null||!s.nonce.equals(nonce)||!p.isAlive()||p.server.overworld().getGameTime()>s.expires||!s.dimension.equals(p.level().dimension().location().toString())||p.blockPosition().distSqr(s.pos)>64)return;
        var data=VisaData.get(p.server);if(s.officer&&!data.officers.containsKey(p.getUUID()))return;
        if(op==0){snapshot(p,s,index,"");return;}
        String result="invalid";country=country.toUpperCase(Locale.ROOT);
        if(!s.officer && op==1){var r=PassportService.verify(p.server,p.getMainHandItem());
            if(r!=null&&r.holder.equals(p.getUUID())&&r.status(PassportService.today(),true).equals("valid")&&Countries.get(country)!=null&&VisaData.TYPES.contains(type)&&stay>0&&stay<=36500 && data.applications.values().stream().filter(a->a.getString("passport").equals(r.id)&&a.getString("status").equals("pending")).count()<8){
                CompoundTag a=new CompoundTag();a.putString("id","A-"+UUID.randomUUID());a.putString("passport",r.id);a.putString("holder",r.holderName);a.putString("nationality",r.country);a.putString("country",country);a.putString("type",type);a.putString("purpose",purpose);a.putInt("stay",stay);a.putLong("date",PassportService.today());a.putString("status","pending");data.applications.put(a.getString("id"),a);data.setDirty();result="pending";
            }
        }else if(s.officer&&(op==2||op==3)){
            var a=data.applications.get(application);
            if(a!=null&&a.getString("status").equals("pending")){
                try {if(op==2){var r=PassportSavedData.get(p.server).find(a.getString("passport"));if(r==null||!r.status(PassportService.today(),true).equals("valid"))throw new IllegalArgumentException();var v=data.issue(r,a.getString("country"),type,PassportService.today()+startDelay,days,entries,stay,p.getGameProfile().getName(),a.getString("purpose"));a.putString("visa",v.getString("id"));}
                    a.putString("status",op==2?"approved":"denied");a.putString("reason",purpose);a.putString("officer",p.getGameProfile().getName());a.putLong("decision",PassportService.today());data.setDirty();result=op==2?"approved":"denied";
                }catch(IllegalArgumentException ignored){}
            }
        }
        // Accepted or failed mutation consumes its session nonce; duplicate packets cannot repeat it.
        Session next=new Session(UUID.randomUUID(),s.pos,s.dimension,s.expires,s.officer);sessions.put(p.getUUID(),next);snapshot(p,next,index,result);
    }
}
