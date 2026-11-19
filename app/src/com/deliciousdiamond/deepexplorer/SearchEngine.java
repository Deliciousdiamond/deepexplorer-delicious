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

    private SearchEngine(String id, String name, String letter, int color, String queryUrl) {
        this.id = id;
        this.name = name;
        this.letter = letter;
        this.color = color;
        this.queryUrl = queryUrl;
    }

    public static final List<SearchEngine> ALL = Arrays.asList(
            new SearchEngine("google", "Google", "G", 0xFF4285F4, "https://www.google.com/search?q="),
            new SearchEngine("bing", "Bing", "B", 0xFF008373, "https://www.bing.com/search?q="),
            new SearchEngine("duckduckgo", "DuckDuckGo", "D", 0xFFDE5833, "https://duckduckgo.com/?q="),
            new SearchEngine("brave", "Brave", "B", 0xFFFB542B, "https://search.brave.com/search?q="),
            new SearchEngine("startpage", "Startpage", "S", 0xFF4A4A4A, "https://www.startpage.com/sp/search?query="),
            new SearchEngine("ecosia", "Ecosia", "E", 0xFF2D6A4F, "https://www.ecosia.org/search?q="),
            new SearchEngine("qwant", "Qwant", "Q", 0xFF5B7BFF, "https://www.qwant.com/?q="),
            new SearchEngine("yandex", "Yandex", "Y", 0xFFFC3F1D, "https://yandex.com/search/?text="),
            new SearchEngine("baidu", "百度", "百", 0xFF2932E1, "https://www.baidu.com/s?wd="),
            new SearchEngine("sogou", "搜狗", "搜", 0xFFFF6A00, "https://www.sogou.com/web?query="),
            new SearchEngine("so360", "360 搜索", "360", 0xFF19B955, "https://www.so.com/s?q="),
            new SearchEngine("shenma", "神马", "神", 0xFFFF5A1F, "https://m.sm.cn/s?q=")
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
