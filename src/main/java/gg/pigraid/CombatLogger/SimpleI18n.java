package gg.pigraid.CombatLogger;

import cn.nukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Simple i18n wrapper for PM1E
 */
public class SimpleI18n {
    private final Plugin plugin;
    private final Map<String, Map<String, String>> languages = new HashMap<>();

    public SimpleI18n(Plugin plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    private void loadLanguages() {
        String[] locales = {"en_US"};
        for (String locale : locales) {
            try (InputStream stream = plugin.getClass().getResourceAsStream("/language/" + locale + ".lang")) {
                if (stream != null) {
                    Map<String, String> translations = parseLanguageFile(stream);
                    languages.put(locale, translations);
                }
            } catch (Exception e) {
                plugin.getLogger().error("Failed to load language: " + locale, e);
            }
        }
    }

    private Map<String, String> parseLanguageFile(InputStream stream) {
        Map<String, String> translations = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator > 0) {
                    String key = line.substring(0, separator).trim();
                    String value = line.substring(separator + 1).trim();
                    translations.put(key, value);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().error("Failed to parse language file", e);
        }
        return translations;
    }

    public String tr(Locale locale, String key, Object... params) {
        String localeStr = (locale != null) ? locale.toString() : "en_US";
        Map<String, String> lang = languages.getOrDefault(localeStr, languages.get("en_US"));
        if (lang == null) {
            return key;
        }
        String message = lang.getOrDefault(key, key);
        if (params.length > 0) {
            // Replace {%0}, {%1}, etc. with parameters
            for (int i = 0; i < params.length; i++) {
                message = message.replace("{%" + i + "}", String.valueOf(params[i]));
            }
        }
        return message;
    }
}
