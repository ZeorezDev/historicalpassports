package historicalpassports.data;

public record Country(String code, String name_en, String name_tr, String official_name_en,
                      String passport_name, String color, String visual_details, int validity_days, boolean historical) {
    public String key() { return code.toLowerCase(java.util.Locale.ROOT); }
}
