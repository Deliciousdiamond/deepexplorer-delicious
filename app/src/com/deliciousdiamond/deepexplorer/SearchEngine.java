package com.deliciousdiamond.deepexplorer;

import java.util.Arrays;
import java.util.List;

/** Offline list of search engines; the query URL is the only data needed at search time. */
public final class SearchEngine {
    public final String id;
    public final String name;
    public final String letter;
    public final int color;
    public final String queryUrl;
    public final boolean custom;

    private SearchEngine(String id, String name, String letter, int color, String queryUrl,
                         boolean custom) {
        this.id = id;
        this.name = name;
        this.letter = letter;
        this.color = color;
        this.queryUrl = queryUrl;
        this.custom = custom;
    }

    /** A user-defined engine; the URL template must contain {@code %s}. */
    public static SearchEngine custom(String id, String name, String template, int color) {
        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        return new SearchEngine(id, name, letter, color, template, true);
    }

    /** Builds the search URL, substituting the encoded query into {@code %s}. */
    public String query(String encoded) {
        if (queryUrl.contains("%s")) return queryUrl.replace("%s", encoded);
        return queryUrl + encoded;
    }

    public static final List<SearchEngine> ALL = Arrays.asList(
            new SearchEngine("google", "Google", "G", 0xFF4285F4, "https://www.google.com/search?q=", false),
            new SearchEngine("bing", "Bing", "B", 0xFF008373, "https://www.bing.com/search?q=", false),
            new SearchEngine("duckduckgo", "DuckDuckGo", "D", 0xFFDE5833, "https://duckduckgo.com/?q=", false),
            new SearchEngine("brave", "Brave", "B", 0xFFFB542B, "https://search.brave.com/search?q=", false),
            new SearchEngine("startpage", "Startpage", "S", 0xFF4A4A4A, "https://www.startpage.com/sp/search?query=", false),
            new SearchEngine("ecosia", "Ecosia", "E", 0xFF2D6A4F, "https://www.ecosia.org/search?q=", false),
            new SearchEngine("qwant", "Qwant", "Q", 0xFF5B7BFF, "https://www.qwant.com/?q=", false),
            new SearchEngine("yandex", "Yandex", "Y", 0xFFFC3F1D, "https://yandex.com/search/?text=", false),
            new SearchEngine("baidu", "百度", "百", 0xFF2932E1, "https://www.baidu.com/s?wd=", false),
            new SearchEngine("sogou", "搜狗", "搜", 0xFFFF6A00, "https://www.sogou.com/web?query=", false),
            new SearchEngine("so360", "360 搜索", "360", 0xFF19B955, "https://www.so.com/s?q=", false),
            new SearchEngine("shenma", "神马", "神", 0xFFFF5A1F, "https://m.sm.cn/s?q=", false)
    );

    public static SearchEngine byId(String id) {
        if (id != null) {
            for (SearchEngine engine : ALL) {
                if (engine.id.equals(id)) return engine;
            }
        }
        return ALL.get(0);
    }

    public static SearchEngine next(SearchEngine current) {
        if (current == null) return ALL.get(0);
        int index = ALL.indexOf(current);
        return ALL.get((index + 1) % ALL.size());
    }
}
