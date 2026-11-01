package historicalpassports.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Build-time JSON catalog. Extra files are listed in countries/index.json on both sides. */
public final class Countries {
    public static final Map<String, Country> ALL = load();
    private static Map<String, Country> load() {
        Gson gson = new Gson();
        Map<String, Country> result = new LinkedHashMap<>();
        try {
            String[] paths = gson.fromJson(reader("index.json"), String[].class);
            for (String path : paths) {
                try (Reader r = reader(path)) {
                    List<Country> entries = gson.fromJson(r, new TypeToken<List<Country>>() {}.getType());
                    for (Country c : entries) {
                        if (c.code() == null || !c.code().matches("[A-Z][A-Z0-9_]{1,7}") || c.name_en() == null || c.name_tr() == null || c.validity_days() < 0)
                            throw new IllegalStateException("Invalid country in " + path);
                        if (result.putIfAbsent(c.code(), c) != null) throw new IllegalStateException("Duplicate country: " + c.code());
                    }
                }
            }
            if (result.values().stream().filter(c -> !c.historical()).count() < 195) throw new IllegalStateException("Missing baseline countries");
            return Collections.unmodifiableMap(result);
        } catch (IOException e) { throw new IllegalStateException("Cannot load passport country catalog", e); }
    }
    private static Reader reader(String path) {
        InputStream in = Countries.class.getResourceAsStream("/data/historicalpassports/countries/" + path);
        if (in == null) throw new IllegalStateException("Missing country resource: " + path);
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }
    public static Country get(String code) { return ALL.get(code.toUpperCase(Locale.ROOT)); }
    private Countries() {}
}
