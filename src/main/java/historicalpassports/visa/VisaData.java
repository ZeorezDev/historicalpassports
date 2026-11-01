package historicalpassports.visa;

import historicalpassports.data.*;
import historicalpassports.passport.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Separate additive store: old passport saves require no migration. */
public final class VisaData extends SavedData {
    public static final List<String> TYPES = List.of("tourist","business","student","work","transit","diplomatic","official","special");
    public static final List<String> POLICIES = List.of("required","free","prohibited","special");
    public final Map<String, CompoundTag> visas = new LinkedHashMap<>(), applications = new LinkedHashMap<>();
    public final Map<UUID,String> officers = new LinkedHashMap<>();
    public final Map<String,String> policies = new TreeMap<>();
    public static VisaData get(MinecraftServer s) { return s.overworld().getDataStorage().computeIfAbsent(VisaData::load,VisaData::new,"historicalpassports_visas"); }
    public String policy(String from,String to) { return policies.getOrDefault(from+":"+to,"free"); }
    public String status(CompoundTag v,long day) {
        if(v.getBoolean("revoked")) return "revoked";
        if(day<v.getLong("start")) return "not_yet_valid";
        if(day>v.getLong("end")) return "expired";
        if(v.getInt("entries")!=-1 && v.getInt("used")>=v.getInt("entries")) return "exhausted";
        return "valid";
    }
    public List<CompoundTag> forPassport(String id) { return visas.values().stream().filter(v->v.getString("passport").equals(id)).toList(); }
    public CompoundTag eligible(String id,String country,boolean special) {
        return forPassport(id).stream().filter(v->v.getString("country").equals(country) && status(v,PassportService.today()).equals("valid") && (!special || v.getString("type").equals("special"))).findFirst().orElse(null);
    }
    public String check(PassportRecord r,String country) {
        if(r==null || !r.status(PassportService.today(),historicalpassports.config.PassportConfig.EXPIRATION.get()).equals("valid")) return "passport_invalid";
        if(Countries.get(country)==null) return "country_invalid";
        String policy=policy(r.country,country);
        if(policy.equals("prohibited")) return "prohibited";
        if(policy.equals("free")) return "free";
        if(eligible(r.id,country,policy.equals("special"))!=null) return "valid";
        var matching=forPassport(r.id).stream().filter(v->v.getString("country").equals(country)&&(!policy.equals("special")||v.getString("type").equals("special"))).toList();
        return matching.isEmpty()?"visa_invalid":status(matching.get(matching.size()-1),PassportService.today());
    }
    public CompoundTag issue(PassportRecord r,String country,String type,long start,int days,int entries,int stay,String officer,String purpose) {
        if(r==null || !r.status(PassportService.today(),historicalpassports.config.PassportConfig.EXPIRATION.get()).equals("valid") || Countries.get(country)==null || !TYPES.contains(type) || days<1 || days>36500 || stay<1 || stay>days || (entries!=-1 && (entries<1 || entries>10000)) || start<PassportService.today() || start>PassportService.today()+36500 || forPassport(r.id).size()>=128) throw new IllegalArgumentException();
        var limit=VisaRules.limits(type);if(days>limit.validity()||stay>limit.stay()||(entries!=-1&&entries>limit.entries()))throw new IllegalArgumentException();
        CompoundTag v=new CompoundTag(); v.putString("id","V-"+UUID.randomUUID()); v.putString("passport",r.id); v.putString("holder",r.holderName); v.putString("country",country); v.putString("type",type); v.putString("officer",officer); v.putString("purpose",purpose);
        v.putLong("issued",PassportService.today());v.putLong("start",start);v.putLong("end",start+days-1);v.putInt("entries",entries);v.putInt("stay",stay);
        visas.put(v.getString("id"),v);setDirty();return v;
    }
    @Override public CompoundTag save(CompoundTag t) {
        t.putInt("schema",1);ListTag vs=new ListTag(),as=new ListTag(),os=new ListTag();visas.values().forEach(v->vs.add(v.copy()));applications.values().forEach(v->as.add(v.copy()));
        officers.forEach((id,name)->{CompoundTag o=new CompoundTag();o.putUUID("id",id);o.putString("name",name);os.add(o);});
        CompoundTag ps=new CompoundTag();policies.forEach(ps::putString);t.put("visas",vs);t.put("applications",as);t.put("officers",os);t.put("policies",ps);return t;
    }
    public static VisaData load(CompoundTag t) {
        VisaData d=new VisaData();t.getList("visas",10).forEach(x->{CompoundTag v=((CompoundTag)x).copy();d.visas.put(v.getString("id"),v);});t.getList("applications",10).forEach(x->{CompoundTag v=((CompoundTag)x).copy();d.applications.put(v.getString("id"),v);});
        t.getList("officers",10).forEach(x->{CompoundTag v=(CompoundTag)x;d.officers.put(v.getUUID("id"),v.getString("name"));});CompoundTag ps=t.getCompound("policies");ps.getAllKeys().forEach(k->d.policies.put(k,ps.getString(k)));return d;
    }
}
