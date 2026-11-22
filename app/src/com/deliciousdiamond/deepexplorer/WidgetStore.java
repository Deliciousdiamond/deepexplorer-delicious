package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Persistence for the optional home-screen widgets.
 *
 * <p>The set of widget types is fixed and ordered; each entry only stores whether it is
 * enabled plus the few strings it needs (city, link title, note body, ...). Keeping it
 * to a flat JSON array means no database and no migration code for 1.9.</p>
 */
final class WidgetStore {
    static final String WEATHER = "weather";
    static final String ADDRESS = "address";
    static final String NOTE = "note";
    static final String QUOTE = "quote";

    static final String[] ORDER = {WEATHER, ADDRESS, NOTE, QUOTE};

    private static final String FILE = "deepexplorer_widgets";
    private static final String KEY_ITEMS = "items";

    static final class Item {
        String type;
        boolean enabled;
        String title = "";
        String value = "";

        Item(String type) {
            this.type = type;
        }
    }

    private WidgetStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static Item[] defaults(Context context) {
        Item[] items = new Item[ORDER.length];
        for (int i = 0; i < ORDER.length; i++) items[i] = new Item(ORDER[i]);

        Item weather = items[0];
        weather.title = context.getString(R.string.weather_default_city);

        Item address = items[1];
        address.title = "GitHub";
        address.value = "https://github.com";

        Item note = items[2];

        Item quote = items[3];
        // One quiet default card makes the feature discoverable without any setup.
        quote.enabled = true;

        return items;
    }

    static Item[] load(Context context) {
        Item[] items = defaults(context);
        try {
            String raw = prefs(context).getString(KEY_ITEMS, "");
            if (raw == null || raw.isEmpty()) return items;
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                Item item = find(items, object.optString("type"));
                if (item == null) continue;
                item.enabled = object.optBoolean("enabled", item.enabled);
                item.title = object.optString("title", item.title);
                item.value = object.optString("value", item.value);
            }
        } catch (Throwable ignored) {
        }
        return items;
    }

    static void save(Context context, Item[] items) {
        if (items == null) return;
        try {
            JSONArray array = new JSONArray();
            for (Item item : items) {
                if (item == null) continue;
                JSONObject object = new JSONObject();
                object.put("type", item.type);
                object.put("enabled", item.enabled);
                object.put("title", item.title == null ? "" : item.title);
                object.put("value", item.value == null ? "" : item.value);
                array.put(object);
            }
            prefs(context).edit().putString(KEY_ITEMS, array.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    static Item find(Item[] items, String type) {
        if (items == null || type == null) return null;
        for (Item item : items) {
            if (item != null && type.equals(item.type)) return item;
        }
        return null;
    }
}
