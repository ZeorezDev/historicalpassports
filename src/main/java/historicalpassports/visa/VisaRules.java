package historicalpassports.visa;

import com.google.gson.*;
import historicalpassports.data.Countries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Server-owned roleplay configuration, read once per world start. */
public final class VisaRules {
    public record Limits(int validity,int stay,int entries){}
    private static final Map<String,Limits> limits=new HashMap<>();
    public static Limits limits(String type){return limits.getOrDefault(type,new Limits(36500,36500,10000));}
    public static void load(MinecraftServer server){
        limits.clear();Path file=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/historicalpassports-visa-rules.json");
        try {
            if(!Files.exists(file)){Files.createDirectories(file.getParent());JsonObject root=new JsonObject(),types=new JsonObject();for(String type:VisaData.TYPES){JsonObject t=new JsonObject();t.addProperty("max_validity_days",36500);t.addProperty("max_stay_days",36500);t.addProperty("max_entries",10000);types.add(type,t);}root.add("types",types);root.add("policy_overrides",new JsonObject());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(root));}
            JsonObject root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for(var entry:root.getAsJsonObject("types").entrySet()){if(!VisaData.TYPES.contains(entry.getKey()))throw new IllegalArgumentException("Unknown visa type");var t=entry.getValue().getAsJsonObject();int days=t.get("max_validity_days").getAsInt(),stay=t.get("max_stay_days").getAsInt(),count=t.get("max_entries").getAsInt();if(days<1||days>36500||stay<1||stay>36500||count<1||count>10000)throw new IllegalArgumentException("Invalid visa limits");limits.put(entry.getKey(),new Limits(days,stay,count));}
            var data=VisaData.get(server);for(var e:root.getAsJsonObject("policy_overrides").entrySet()){String[] pair=e.getKey().split(":");String value=e.getValue().getAsString();if(pair.length!=2||Countries.get(pair[0])==null||Countries.get(pair[1])==null||!VisaData.POLICIES.contains(value))throw new IllegalArgumentException("Invalid policy override: "+e.getKey());data.policies.put(e.getKey(),value);}data.setDirty();
        }catch(Exception e){throw new IllegalStateException("Cannot load visa rules: "+file,e);}
    }
}
