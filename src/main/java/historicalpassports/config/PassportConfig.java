package historicalpassports.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PassportConfig {
    public static final ForgeConfigSpec SERVER, CLIENT;
    public static final ForgeConfigSpec.BooleanValue EXPIRATION, DESK, HISTORICAL, OFFICER_ONLY, SOUNDS, ANIMATIONS;
    public static final ForgeConfigSpec.IntValue VALIDITY_DAYS, MAX_HISTORY;
    public static final ForgeConfigSpec.ConfigValue<String> LANGUAGE;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        EXPIRATION = b.comment("Expiration uses real UTC calendar days.").define("expirationEnabled", true);
        VALIDITY_DAYS = b.defineInRange("defaultValidityDays", 3650, 1, 36500);
        MAX_HISTORY = b.comment("Reject further inspections at this limit; existing history is never deleted.").defineInRange("maximumInspectionHistory", 1000, 1, 10000);
        DESK = b.define("inspectionDeskEnabled", true);
        HISTORICAL = b.define("allowHistoricalPassports", false);
        OFFICER_ONLY = b.comment("If false, operators may also stamp. Unprivileged players NEVER may.").define("officerOnlyStamping", true);
        SERVER = b.build();
        b = new ForgeConfigSpec.Builder();
        SOUNDS = b.define("stampSounds", true);
        ANIMATIONS = b.define("stampAnimations", true);
        LANGUAGE = b.comment("auto uses Minecraft's selected language; en_us or tr_tr overrides booklet country names.").define("defaultLanguage", "auto", v -> v instanceof String s && java.util.Set.of("auto", "en_us", "tr_tr").contains(s));
        CLIENT = b.build();
    }
    private PassportConfig() {}
}
