package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.content.res.Configuration;
import android.webkit.WebView;

import java.io.File;
import java.util.List;

/** Storage accounting and the small, explicit cache-clearing routine. */
public final class StorageUtils {
    private StorageUtils() {
    }

    public static long dirSize(File file) {
        if (file == null || !file.exists()) return 0L;
        if (file.isFile()) return file.length();
        long total = 0L;
        File[] children = file.listFiles();
        if (children == null) return 0L;
        for (File child : children) {
            if (child.isDirectory()) total += dirSize(child);
            else total += child.length();
        }
        return total;
    }

    /** Everything under the app's private data directory (cache, files, WebView data). */
    public static long appSize(Context context) {
        long total = dirSize(new File(context.getApplicationInfo().dataDir));
        File external = context.getExternalCacheDir();
        if (external != null) total += dirSize(external);
        return total;
    }

    /** The main WebView data directory, where Chromium keeps HTTP/code caches. */
    public static long webViewSize(Context context) {
        return dirSize(new File(context.getApplicationInfo().dataDir, "app_webview"));
    }

    public static String format(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024.0) return String.format(java.util.Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024.0) return String.format(java.util.Locale.US, "%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format(java.util.Locale.US, "%.2f GB", gb);
    }

    public static void clearWebView(WebView webView) {
        if (webView == null) return;
        try {
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();
        } catch (Throwable ignored) {
        }
    }

    public static void clearAll(List<BrowserTab> tabs, WebView aiView) {
        if (tabs != null) {
            for (BrowserTab tab : tabs) clearWebView(tab.webView);
        }
        clearWebView(aiView);
    }

    /** Cap enforcement must not erase the user's navigation history. */
    public static void clearCachesOnly(List<BrowserTab> tabs, WebView aiView) {
        if (tabs != null) {
            for (BrowserTab tab : tabs) {
                try { tab.webView.clearCache(true); } catch (Throwable ignored) { }
            }
        }
        if (aiView != null) {
            try { aiView.clearCache(true); } catch (Throwable ignored) { }
        }
    }

    public static boolean isNight(Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }
}
