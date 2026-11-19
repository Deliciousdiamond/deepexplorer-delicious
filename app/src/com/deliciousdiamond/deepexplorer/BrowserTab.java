package com.deliciousdiamond.deepexplorer;

import android.webkit.WebView;

/** One browser tab: a WebView plus the small amount of state the UI shows. */
public final class BrowserTab {
    public final int id;
    public final WebView webView;
    public String title = "新标签页";
    public String url;
    public String mobileUserAgent;
    public boolean desktopMode;
    public boolean errorPageShowing;
    public boolean networkError;
    public String failedUrl;

    public BrowserTab(int id, WebView webView) {
        this.id = id;
        this.webView = webView;
    }
}
