package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Quote source for the home widget.
 *
 * <p>Online quotes come from Hitokoto (一言), the largest Chinese quote collection,
 * restricted to literature, poetry and philosophy. The last few fetched quotes are
 * cached locally, and a small built-in set keeps the card useful offline or when the
 * service is unreachable.</p>
 */
final class QuoteRepository {
    static final class Quote {
        final String text;
        final String from;
        final String who;

        Quote(String text, String from, String who) {
            this.text = text;
            this.from = from == null ? "" : from;
            this.who = who == null ? "" : who;
        }

        String attribution() {
            if (!who.isEmpty() && !from.isEmpty()) return "—— " + who + "《" + from + "》";
            if (!who.isEmpty()) return "—— " + who;
            if (!from.isEmpty()) return "—— " + from;
            return "";
        }
    }

    private static final String FILE = "deepexplorer_quotes";
    private static final String KEY_CACHE = "cache";
    private static final String KEY_INDEX = "index";
    private static final int MAX_CACHE = 24;
    private static final long MIN_INTERVAL_MS = 1500L;
    private static final long RETRY_AFTER_MS = 45_000L;

    private static final String[] FALLBACK_TEXT = {
            "路漫漫其修远兮，吾将上下而求索。",
            "纸上得来终觉浅，绝知此事要躬行。",
            "不问终点，全力以赴。",
            "弱小和无知不是生存的障碍，傲慢才是。",
            "凡是过往，皆为序章。",
            "山高路远，看世界，也找自己。",
            "慢慢来，比较快。",
            "去做，去错，去改。",
            "今天也要好好吃饭。",
            "海压竹枝低复举，风吹山角晦还明。",
            "真正的发现之旅，不在于寻找新风景，而在于拥有新眼光。",
            "且将新火试新茶，诗酒趁年华。"
    };

    private final Context context;
    private final ExecutorService io;
    private final Handler main;
    private final List<Quote> cache = new ArrayList<>();
    private final List<Quote> fallback = new ArrayList<>();

    private int index;
    private boolean fetching;
    private boolean failed;
    private long lastAttemptAt;

    QuoteRepository(Context context, ExecutorService io, Handler main) {
        this.context = context.getApplicationContext();
        this.io = io;
        this.main = main;
        for (String text : FALLBACK_TEXT) fallback.add(new Quote(text, "", ""));
        load();
    }

    /** Current quote; falls back to the built-in list while the network cache is empty. */
    Quote current() {
        if (!cache.isEmpty()) return cache.get(Math.abs(index) % cache.size());
        if (!fallback.isEmpty()) return fallback.get(Math.abs(index) % fallback.size());
        return new Quote("", "", "");
    }

    /** Advances the rotation and opportunistically fetches one more online quote. */
    Quote next() {
        index++;
        saveIndex();
        prefetch();
        return current();
    }

    /** Fetches in the background when the cache is small; safe to call often. */
    void prefetch() {
        if (fetching || cache.size() >= MAX_CACHE) return;
        long now = System.currentTimeMillis();
        if (failed && now - lastAttemptAt < RETRY_AFTER_MS) return;
        if (now - lastAttemptAt < MIN_INTERVAL_MS) return;
        fetching = true;
        lastAttemptAt = now;
        try {
            io.execute(new Runnable() {
                @Override
                public void run() {
                    final Quote quote = fetchOne();
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            fetching = false;
                            if (quote == null) {
                                failed = true;
                                return;
                            }
                            failed = false;
                            add(quote);
                        }
                    });
                }
            });
        } catch (RejectedExecutionException e) {
            fetching = false;
        }
    }

    private void add(Quote quote) {
        if (quote == null || quote.text.isEmpty()) return;
        for (Quote existing : cache) {
            if (existing.text.equals(quote.text)) return;
        }
        cache.add(quote);
        while (cache.size() > MAX_CACHE) cache.remove(0);
        saveCache();
    }

    private Quote fetchOne() {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            // Hitokoto caches identical URLs, so a timestamp keeps every fetch fresh.
            URL url = new URL("https://v1.hitokoto.cn/?encode=json&max_length=70"
                    + "&c=d&c=i&c=k&_=" + System.currentTimeMillis());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "DeepExplorer/1.9.2 Android");
            input = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int n;
            while (total < 64 * 1024 && (n = input.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                total += n;
            }
            JSONObject object = new JSONObject(new String(out.toByteArray(), "UTF-8"));
            String text = object.optString("hitokoto", "").trim();
            if (text.isEmpty()) return null;
            String from = object.isNull("from") ? "" : object.optString("from", "").trim();
            String who = object.isNull("from_who") ? "" : object.optString("from_who", "").trim();
            return new Quote(text, from, who);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (input != null) try { input.close(); } catch (Throwable ignored) { }
            if (connection != null) try { connection.disconnect(); } catch (Throwable ignored) { }
        }
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private void load() {
        try {
            String raw = prefs().getString(KEY_CACHE, "");
            if (raw != null && !raw.isEmpty()) {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length() && cache.size() < MAX_CACHE; i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item == null) continue;
                    String text = item.optString("t", "");
                    if (text.isEmpty()) continue;
                    cache.add(new Quote(text, item.optString("f", ""), item.optString("w", "")));
                }
            }
            index = Math.max(0, prefs().getInt(KEY_INDEX, 0));
        } catch (Throwable ignored) {
        }
    }

    private void saveCache() {
        try {
            JSONArray array = new JSONArray();
            for (Quote quote : cache) {
                JSONObject item = new JSONObject();
                item.put("t", quote.text);
                item.put("f", quote.from);
                item.put("w", quote.who);
                array.put(item);
            }
            prefs().edit().putString(KEY_CACHE, array.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    private void saveIndex() {
        prefs().edit().putInt(KEY_INDEX, index).apply();
    }
}
