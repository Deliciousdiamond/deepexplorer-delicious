package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Small local bookmark store; no account, no network. */
public final class BookmarkStore {
    private static final String PREFS = "deepexplorer_bookmarks";
    private static final String KEY = "items";
    private static final int MAX = 100;

    public static final class Item {
        public final String title;
        public final String url;

        Item(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    private BookmarkStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Item> list(Context context) {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(context).getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String url = object.optString("url");
                if (url == null || url.isEmpty()) continue;
                out.add(new Item(object.optString("title", url), url));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static boolean contains(Context context, String url) {
        if (url == null || url.isEmpty()) return false;
        for (Item item : list(context)) {
            if (url.equals(item.url)) return true;
        }
        return false;
    }

    /** @return true when the page was added, false when an existing bookmark was removed. */
    public static boolean toggle(Context context, String title, String url) {
        if (url == null || url.isEmpty()) return false;
        List<Item> items = list(context);
        for (int i = 0; i < items.size(); i++) {
            if (url.equals(items.get(i).url)) {
                items.remove(i);
                save(context, items);
                return false;
            }
        }
        String safeTitle = title == null || title.trim().isEmpty() ? url : title.trim();
        items.add(0, new Item(safeTitle, url));
        while (items.size() > MAX) items.remove(items.size() - 1);
        save(context, items);
        return true;
    }

    public static void remove(Context context, String url) {
        if (url == null) return;
        List<Item> items = list(context);
        for (int i = items.size() - 1; i >= 0; i--) {
            if (url.equals(items.get(i).url)) items.remove(i);
        }
        save(context, items);
    }

    private static void save(Context context, List<Item> items) {
        JSONArray array = new JSONArray();
        for (Item item : items) {
            try {
                JSONObject object = new JSONObject();
                object.put("title", item.title);
                object.put("url", item.url);
                array.put(object);
            } catch (Exception ignored) {
            }
        }
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }
}
