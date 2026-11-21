package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Built-in search engines plus the user's own URL templates, persisted as JSON. */
final class EngineStore {
    private static final String FILE = "deepexplorer_engines";
    private static final String KEY_CUSTOM = "custom";

    private static final int[] COLORS = {
            0xFF6D28D9, 0xFF0F766E, 0xFFB45309, 0xFFBE123C,
            0xFF1D4ED8, 0xFF4D7C0F, 0xFF7C3AED, 0xFF0E7490
    };

    private EngineStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static List<SearchEngine> custom(Context context) {
        List<SearchEngine> result = new ArrayList<>();
        try {
            String raw = prefs(context).getString(KEY_CUSTOM, "");
            if (raw == null || raw.isEmpty()) return result;
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String name = item.optString("name", "");
                String url = item.optString("url", "");
                if (id.isEmpty() || name.isEmpty() || url.isEmpty()) continue;
                result.add(SearchEngine.custom(id, name, url, colorFor(name)));
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    static void save(Context context, List<SearchEngine> engines) {
        try {
            JSONArray array = new JSONArray();
            for (SearchEngine engine : engines) {
                if (engine == null || !engine.custom) continue;
                JSONObject item = new JSONObject();
                item.put("id", engine.id);
                item.put("name", engine.name);
                item.put("url", engine.queryUrl);
                array.put(item);
            }
            prefs(context).edit().putString(KEY_CUSTOM, array.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    static List<SearchEngine> all(Context context) {
        List<SearchEngine> result = new ArrayList<>(SearchEngine.ALL);
        result.addAll(custom(context));
        return result;
    }

    static SearchEngine byId(Context context, String id) {
        List<SearchEngine> all = all(context);
        if (id != null) {
            for (SearchEngine engine : all) {
                if (engine.id.equals(id)) return engine;
            }
        }
        return all.get(0);
    }

    static SearchEngine add(Context context, String name, String template) {
        List<SearchEngine> custom = custom(context);
        String id = "custom_" + System.currentTimeMillis();
        SearchEngine engine = SearchEngine.custom(id, name, template, colorFor(name));
        custom.add(engine);
        save(context, custom);
        return engine;
    }

    static void remove(Context context, String id) {
        List<SearchEngine> custom = custom(context);
        for (int i = 0; i < custom.size(); i++) {
            if (custom.get(i).id.equals(id)) {
                custom.remove(i);
                break;
            }
        }
        save(context, custom);
    }

    private static int colorFor(String name) {
        int hash = name == null ? 0 : name.hashCode();
        return COLORS[Math.abs(hash) % COLORS.length];
    }
}
