package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, private settings store. The API key never leaves the device. */
public final class SettingsStore {
    private static final String FILE = "deepexplorer";
    private static final String KEY_API = "api_key";
    private static final String KEY_AI_PROVIDER = "ai_provider";
    private static final String KEY_AI_BASE = "ai_base_url";
    private static final String KEY_AI_MODEL = "ai_custom_model";
    private static final String KEY_AI = "ai_mode";
    private static final String KEY_MODEL = "model";
    private static final String KEY_ENGINE = "search_engine";
    private static final String KEY_THEME = "theme";
    private static final String KEY_FONT = "font";
    private static final String KEY_BG_TYPE = "bg_type";
    private static final String KEY_BG_URI = "bg_uri";
    private static final String KEY_DOWNLOAD_IDS = "download_ids";
    private static final String KEY_BANNER_DISMISSED = "banner_dismissed_id";
    public static final String PROVIDER_DEEPSEEK = "deepseek";
    public static final String PROVIDER_CUSTOM = "custom";
    public static final String MODEL_CHAT = "deepseek-chat";
    public static final String MODEL_REASONER = "deepseek-reasoner";

    private SettingsStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getApiKey(Context context) {
        String value = prefs(context).getString(KEY_API, "");
        return value == null ? "" : value.trim();
    }

    public static void setApiKey(Context context, String value) {
        prefs(context).edit().putString(KEY_API, value == null ? "" : value.trim()).apply();
    }

    public static boolean isCustomAi(Context context) {
        return PROVIDER_CUSTOM.equals(prefs(context).getString(KEY_AI_PROVIDER, PROVIDER_DEEPSEEK));
    }

    public static void setCustomAi(Context context, boolean custom) {
        prefs(context).edit().putString(KEY_AI_PROVIDER,
                custom ? PROVIDER_CUSTOM : PROVIDER_DEEPSEEK).apply();
    }

    public static String getAiBaseUrl(Context context) {
        String value = prefs(context).getString(KEY_AI_BASE, "https://api.openai.com/v1");
        return value == null ? "" : value.trim();
    }

    public static void setAiBaseUrl(Context context, String value) {
        prefs(context).edit().putString(KEY_AI_BASE, value == null ? "" : value.trim()).apply();
    }

    public static String getAiModel(Context context) {
        String value = prefs(context).getString(KEY_AI_MODEL, "");
        return value == null ? "" : value.trim();
    }

    public static void setAiModel(Context context, String value) {
        prefs(context).edit().putString(KEY_AI_MODEL, value == null ? "" : value.trim()).apply();
    }

    public static boolean isAiMode(Context context) {
        return prefs(context).getBoolean(KEY_AI, false);
    }

    public static void setAiMode(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AI, enabled).apply();
    }

    public static String getModel(Context context) {
        String value = prefs(context).getString(KEY_MODEL, MODEL_CHAT);
        return MODEL_REASONER.equals(value) ? MODEL_REASONER : MODEL_CHAT;
    }

    public static void setModel(Context context, String model) {
        prefs(context).edit().putString(KEY_MODEL, MODEL_REASONER.equals(model) ? MODEL_REASONER : MODEL_CHAT).apply();
    }

    public static String getEngineId(Context context) {
        String value = prefs(context).getString(KEY_ENGINE, "google");
        return value == null ? "google" : value;
    }

    public static void setEngineId(Context context, String id) {
        prefs(context).edit().putString(KEY_ENGINE, id == null ? "google" : id).apply();
    }

    public static String getThemeId(Context context) {
        String value = prefs(context).getString(KEY_THEME, "nightly");
        return value == null ? "nightly" : value;
    }

    public static void setThemeId(Context context, String id) {
        prefs(context).edit().putString(KEY_THEME, id == null ? "nightly" : id).apply();
    }

    public static String getFontId(Context context) {
        String value = prefs(context).getString(KEY_FONT, "sans");
        return value == null ? "sans" : value;
    }

    public static void setFontId(Context context, String id) {
        prefs(context).edit().putString(KEY_FONT, id == null ? "sans" : id).apply();
    }

    public static String getBackgroundType(Context context) {
        String value = prefs(context).getString(KEY_BG_TYPE, "none");
        return value == null ? "none" : value;
    }

    public static void setBackgroundType(Context context, String type) {
        prefs(context).edit().putString(KEY_BG_TYPE, type == null ? "none" : type).apply();
    }

    public static String getBackgroundUri(Context context) {
        String value = prefs(context).getString(KEY_BG_URI, "");
        return value == null ? "" : value;
    }

    public static void setBackgroundUri(Context context, String uri) {
        prefs(context).edit().putString(KEY_BG_URI, uri == null ? "" : uri).apply();
    }

    public static String getDownloadIds(Context context) {
        return prefs(context).getString(KEY_DOWNLOAD_IDS, "");
    }

    public static void setDownloadIds(Context context, String ids) {
        prefs(context).edit().putString(KEY_DOWNLOAD_IDS, ids == null ? "" : ids).apply();
    }

    public static long getBannerDismissedId(Context context) {
        return prefs(context).getLong(KEY_BANNER_DISMISSED, 0L);
    }

    public static void setBannerDismissedId(Context context, long id) {
        prefs(context).edit().putLong(KEY_BANNER_DISMISSED, id).apply();
    }
}
