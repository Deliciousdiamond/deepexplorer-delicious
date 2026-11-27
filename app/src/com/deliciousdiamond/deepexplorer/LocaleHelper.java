package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/** In-app language selection, applied by wrapping the activity base context. */
final class LocaleHelper {
    static final String SYSTEM = "system";
    static final String ZH_CN = "zh-CN";
    static final String EN = "en";
    static final String JA = "ja";
    static final String ZH_TW = "zh-TW";

    private static final String FILE = "deepexplorer_locale";
    private static final String KEY = "language";
    private static final Locale SYSTEM_DEFAULT = Locale.getDefault();

    private LocaleHelper() {
    }

    static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        String value = prefs.getString(KEY, SYSTEM);
        return value == null ? SYSTEM : value;
    }

    static void setLanguage(Context context, String code) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY, code == null ? SYSTEM : code).apply();
    }

    static Context wrap(Context base) {
        String code = getLanguage(base);
        if (SYSTEM.equals(code)) {
            Locale.setDefault(SYSTEM_DEFAULT);
            return base;
        }
        Locale locale = localeFor(code);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        return base.createConfigurationContext(configuration);
    }

    static Locale localeFor(String code) {
        if (EN.equals(code)) return Locale.ENGLISH;
        if (JA.equals(code)) return Locale.JAPANESE;
        if (ZH_TW.equals(code)) return Locale.TRADITIONAL_CHINESE;
        if (ZH_CN.equals(code)) return Locale.SIMPLIFIED_CHINESE;
        return Locale.getDefault();
    }

    static String[] codes() {
        return new String[]{SYSTEM, ZH_CN, EN, JA, ZH_TW};
    }

    static String label(Context context, String code) {
        if (EN.equals(code)) return context.getString(R.string.language_en);
        if (JA.equals(code)) return context.getString(R.string.language_ja);
        if (ZH_TW.equals(code)) return context.getString(R.string.language_zh_tw);
        if (ZH_CN.equals(code)) return context.getString(R.string.language_zh_cn);
        return context.getString(R.string.language_system);
    }

    static String htmlLang(String code) {
        if (EN.equals(code)) return "en";
        if (JA.equals(code)) return "ja";
        if (ZH_TW.equals(code)) return "zh-Hant";
        return "zh-CN";
    }
}
