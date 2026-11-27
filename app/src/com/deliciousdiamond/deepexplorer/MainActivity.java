package com.deliciousdiamond.deepexplorer;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.Manifest;
import android.app.DownloadManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** DeepExplorer: a deliberately small WebView browser with a DeepSeek AI panel. */
public class MainActivity extends Activity {
    private static final int MAX_TABS = 6;
    private static final long CACHE_LIMIT_BYTES = 100L * 1024L * 1024L;
    private static final long CACHE_CHECK_INTERVAL_MS = 5000L;

    private FrameLayout root;
    private FrameLayout homeBackground;
    private ImageView homeBgImage;
    private View homeBgScrim;
    private FrameLayout browserFrame;
    private LinearLayout topBar;
    private LinearLayout homeContent;
    private LinearLayout searchBox;
    private LinearLayout enginePicker;
    private TextView engineBadge;
    private TextView homeAppName;
    private TextView homeTagline;
    private FrameLayout aiButton;
    private ImageView aiFab;
    private View navMenu;
    private ImageView menuIcon;
    private LinearLayout findBar;
    private EditText findInput;
    private TextView findResult;
    private ImageView backIcon;
    private ImageView forwardIcon;
    private ImageView homeIcon;
    private ImageView tabsIcon;
    private ImageView aiIcon;
    private ImageView urlIcon;
    private ImageView reloadIcon;
    private EditText searchInput;
    private TextView urlText;
    private TextView tabBadge;
    private View homeHeader;
    private View homeWidgetsScroll;
    private HomeWidgets homeWidgets;
    private ProgressBar progress;
    private WebView aiWebView;
    private LinearLayout connectionBanner;
    private ImageView connectionIcon;
    private TextView connectionText;
    private ValueAnimator bannerColorAnimator;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean onlineState = true;
    private boolean bannerVisible;
    private boolean bannerOnline;
    private int bannerColor;
    private Dialog networkDiagnosticDialog;
    private LinearLayout downloadBanner;
    private ImageView downloadBannerIcon;
    private TextView downloadBannerText;
    private ValueAnimator downloadBannerColorAnimator;
    private boolean downloadBannerVisible;
    private boolean downloadBannerComplete;
    private boolean downloadBannerTerminal;
    private long downloadBannerDismissedId;
    private float downloadBannerBaseY;
    private int downloadBannerColor;
    private long latestDownloadId = -1L;
    private DownloadManager downloadManager;
    private final List<Long> downloadIds = new ArrayList<>();
    private final Map<Long, Long> downloadLastBytes = new HashMap<>();
    private final Map<Long, Long> downloadLastSampleTime = new HashMap<>();
    private final Map<Long, Long> downloadSpeeds = new HashMap<>();
    private Dialog downloadConfirmDialog;
    private LinearLayout downloadsList;
    private TextView downloadsEmpty;
    private ScrollView downloadsScroll;
    private PendingDownload pendingDownload;
    private boolean downloadPolling;
    private static final int REQ_DOWNLOAD_PERMISSION = 43;
    private static final int DOWNLOAD_BLUE = 0xFF1565C0;
    private static final int DOWNLOAD_GREEN = 0xFF1B7F4B;
    private static final int DOWNLOAD_RED = 0xFFB3261E;
    private Typeface currentTypeface;
    private String currentBackgroundType = "none";
    private String currentBackgroundUri = "";
    private String cachedErrorPageHtml;

    private static final int BANNER_RED = 0xFFB3261E;
    private static final int BANNER_GREEN = 0xFF1B7F4B;
    private static final int REQ_BG_IMAGE = 41;
    private static final long DIAGNOSTIC_COOLDOWN_MS = 5L * 60L * 1000L;
    private static final long DOWNLOAD_BANNER_AUTO_HIDE_MS = 4000L;
    private static final String[] OVERSEAS_HOSTS = {
            "google.com", "googleapis.com", "gstatic.com", "googlevideo.com", "googleusercontent.com",
            "youtube.com", "ytimg.com", "x.com", "twitter.com", "t.co", "facebook.com", "fbcdn.net",
            "instagram.com", "whatsapp.com", "telegram.org", "t.me", "openai.com", "chatgpt.com",
            "reddit.com", "wikipedia.org", "medium.com", "github.com", "netflix.com", "spotify.com",
            "discord.com", "twitch.tv", "quora.com", "pinterest.com", "tumblr.com", "dropbox.com",
            "slack.com", "notion.so", "bbc.com", "nytimes.com", "bloomberg.com", "reuters.com"
    };
    private final Map<String, Long> diagnosticThrottle = new HashMap<>();

    private static final String[] THEME_IDS = {"nightly", "ocean", "forest", "sunset", "rose", "graphite"};
    private static final int[] THEME_PRIMARY_LIGHT = {0xFF1A73E8, 0xFF0E7490, 0xFF15803D, 0xFFC2410C, 0xFFBE185D, 0xFF334155};
    private static final int[] THEME_PRIMARY_DARK = {0xFF8AB4F8, 0xFF67E8F9, 0xFF86EFAC, 0xFFFDBA74, 0xFFF9A8D4, 0xFFCBD5E1};
    private static final int[] THEME_ACCENT_LIGHT = {0xFF1967D2, 0xFF06B6D4, 0xFF65A30D, 0xFFF59E0B, 0xFFEC4899, 0xFF64748B};
    private static final int[] THEME_ACCENT_DARK = {0xFFAECBFA, 0xFF22D3EE, 0xFFBEF264, 0xFFFBBF24, 0xFFF472B6, 0xFF94A3B8};
    private static final int[][] THEME_BG_LIGHT = {
            {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF},
            {0xFFF3FBFF, 0xFFE6F7FF, 0xFFD8F1FF},
            {0xFFF5FBF4, 0xFFE8F7E4, 0xFFDDF1D8},
            {0xFFFFF8F1, 0xFFFFEEDC, 0xFFFFE3CC},
            {0xFFFFF5FA, 0xFFFFE9F3, 0xFFFFDDEB},
            {0xFFF8FAFC, 0xFFEEF3F8, 0xFFE5ECF3}
    };
    private static final int[][] THEME_BG_DARK = {
            {0xFF0B0B0F, 0xFF0B0B0F, 0xFF0B0B0F},
            {0xFF04121A, 0xFF062330, 0xFF083344},
            {0xFF06130A, 0xFF0B2113, 0xFF10331D},
            {0xFF1A0A04, 0xFF261006, 0xFF33170A},
            {0xFF1A0710, 0xFF260A19, 0xFF331024},
            {0xFF0B0F14, 0xFF121A22, 0xFF1A242F}
    };
    private static final String[] FONT_IDS = {"sans", "serif", "mono", "condensed", "light"};
    private static final String[] FONT_FAMILIES = {"sans-serif", "serif", "monospace", "sans-serif-condensed", "sans-serif-light"};
    private static final int[] FONT_NAMES = {R.string.font_sans, R.string.font_serif, R.string.font_mono, R.string.font_condensed, R.string.font_light};
    private static final int[] THEME_NAMES = {R.string.theme_minimal, R.string.theme_ocean, R.string.theme_forest,
            R.string.theme_sunset, R.string.theme_rose, R.string.theme_graphite};

    private final List<BrowserTab> tabs = new ArrayList<>();
    private BrowserTab currentTab;
    private int nextTabId = 1;
    private Mode mode = Mode.HOME;
    private boolean aiMode;
    private boolean aiReady;
    private boolean aiStreaming;
    private boolean destroyed;
    private String pendingAiQuery;
    private JSONArray aiHistory = new JSONArray();
    private AiClient aiClient;
    private int aiSeq;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private View pageOverlay;
    private TextView pageTitleView;
    private FrameLayout pageContent;
    private boolean pageVisible;
    private String pageKind;

    private View settingsPage;
    private boolean settingsPageVisible;
    private EditText settingsKeyInput;
    private TextView settingsSizeApp;
    private TextView settingsSizeCache;
    private Runnable settingsStorageUpdater;
    private Dialog tabsDialog;
    private Dialog modelDialog;
    private TabAdapter tabAdapter;
    private String selectedModel;
    private boolean modelSheetClosing;
    private boolean chromeVisible = true;
    private boolean aiClosing;
    private int chromeHeight;
    private Mode aiReturnMode = Mode.HOME;
    private boolean restoreAiModeAfterClose;
    private boolean trimRunning;
    private long lastTrimAt;
    private SearchEngine currentEngine;
    private List<SearchEngine> engines;
    private TransitionDrawable searchBackground;
    private ClipDrawable searchFillClip;
    private ValueAnimator searchFillAnimator;
    private float searchFillFraction;
    private View searchDivider;
    private ImageView engineChevron;
    private int colorOutline;
    private boolean searchGradientActive;
    private PopupWindow enginePopup;
    private PopupWindow menuPopup;
    private PopupWindow aiQuickPopup;
    private boolean findActive;

    private int colorBg;
    private int colorSurface;
    private int colorPrimary;
    private int colorOnSurface;
    private int colorOnSurfaceVariant;
    private int colorAccent;
    private int colorOnPrimary;
    private String currentFontCss;

    private enum Mode { HOME, BROWSER, AI }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aiClient = new AiClient(this);
        setContentView(R.layout.activity_main);

        colorBg = getColorCompat(R.color.bg);
        colorSurface = getColorCompat(R.color.surface);
        colorPrimary = getColorCompat(R.color.primary);
        colorOnSurface = getColorCompat(R.color.on_surface);
        colorOnSurfaceVariant = getColorCompat(R.color.on_surface_variant);
        colorAccent = getColorCompat(R.color.accent);
        colorOnPrimary = getColorCompat(R.color.on_primary);
        colorOutline = getColorCompat(R.color.outline);

        findViews();
        setupAiWebView();
        bindUi();
        applySystemBars();

        selectedModel = SettingsStore.getModel(this);
        engines = EngineStore.all(this);
        currentEngine = EngineStore.byId(this, SettingsStore.getEngineId(this));
        chromeHeight = dp(48);
        updateEngineBadge();
        currentBackgroundType = SettingsStore.getBackgroundType(this);
        currentBackgroundUri = SettingsStore.getBackgroundUri(this);
        applyTheme();
        loadHomeBackground();
        setupConnectivity();
        aiMode = SettingsStore.isAiMode(this);
        updateAiUi();
        createTab(true);
        trimCacheIfNeeded();
    }

    private void findViews() {
        root = findViewById(R.id.root);
        settingsPage = findViewById(R.id.settings_page);
        pageOverlay = findViewById(R.id.page_overlay);
        pageTitleView = findViewById(R.id.page_title);
        pageContent = findViewById(R.id.page_content);
        homeBackground = findViewById(R.id.home_background);
        homeBgImage = findViewById(R.id.home_bg_image);
        homeBgImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        homeBgScrim = findViewById(R.id.home_bg_scrim);
        browserFrame = findViewById(R.id.browser_frame);
        connectionBanner = findViewById(R.id.connection_banner);
        connectionIcon = findViewById(R.id.connection_icon);
        connectionText = findViewById(R.id.connection_text);
        downloadBanner = findViewById(R.id.download_banner);
        downloadBannerIcon = findViewById(R.id.download_banner_icon);
        downloadBannerText = findViewById(R.id.download_banner_text);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        loadDownloadIds();
        downloadBannerDismissedId = SettingsStore.getBannerDismissedId(this);
        topBar = findViewById(R.id.top_bar);
        homeContent = findViewById(R.id.home_content);
        searchBox = findViewById(R.id.search_box);
        enginePicker = findViewById(R.id.engine_picker);
        engineBadge = findViewById(R.id.engine_badge);
        homeAppName = findViewById(R.id.home_app_name);
        homeTagline = findViewById(R.id.home_tagline);
        aiButton = findViewById(R.id.ai_button);
        aiFab = findViewById(R.id.ai_fab);
        navMenu = findViewById(R.id.nav_menu);
        menuIcon = findViewById(R.id.icon_menu);
        findBar = findViewById(R.id.find_bar);
        findInput = findViewById(R.id.find_input);
        findResult = findViewById(R.id.find_result);
        backIcon = findViewById(R.id.icon_back);
        forwardIcon = findViewById(R.id.icon_forward);
        homeIcon = findViewById(R.id.icon_home);
        tabsIcon = findViewById(R.id.icon_tabs);
        aiIcon = findViewById(R.id.ai_icon);
        searchDivider = findViewById(R.id.search_divider);
        engineChevron = findViewById(R.id.engine_chevron);
        urlIcon = findViewById(R.id.url_icon);
        reloadIcon = findViewById(R.id.reload_btn);
        urlText = findViewById(R.id.url_text);
        tabBadge = findViewById(R.id.tab_badge);
        homeHeader = findViewById(R.id.home_header);
        homeWidgetsScroll = findViewById(R.id.home_widgets_scroll);
        homeWidgets = new HomeWidgets(this, (LinearLayout) findViewById(R.id.home_widgets));
        progress = findViewById(R.id.progress);
        aiWebView = findViewById(R.id.ai_web_view);
        searchInput = findViewById(R.id.search_input);
    }

    private void bindUi() {
        setupSettingsPage();
        setupPageOverlay();
        enginePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEnginePopup();
            }
        });
        navMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPageMenu();
            }
        });
        findCloseSetup();
        aiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == Mode.AI) {
                    setAiMode(false);
                } else {
                    setAiMode(!aiMode);
                }
            }
        });
        searchInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && aiMode) applyAiSearchBackground(true);
            }
        });
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    submitSearch();
                    return true;
                }
                return false;
            }
        });
        reloadIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTab != null) {
                    if (currentTab.errorPageShowing && !isOnline()) {
                        onlineState = false;
                        showOfflineBanner();
                        return;
                    }
                    progress.setVisibility(View.VISIBLE);
                    if (currentTab.errorPageShowing && currentTab.failedUrl != null) {
                        currentTab.errorPageShowing = false;
                        currentTab.networkError = false;
                        currentTab.webView.loadUrl(currentTab.failedUrl);
                    } else {
                        currentTab.webView.reload();
                    }
                }
            }
        });
        findViewById(R.id.nav_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackFromNav();
            }
        });
        findViewById(R.id.nav_forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == Mode.AI) return;
                if (currentTab != null && currentTab.webView.canGoForward()) {
                    currentTab.errorPageShowing = false;
                    currentTab.webView.goForward();
                    showBrowser();
                    updateChrome();
                }
            }
        });
        findViewById(R.id.nav_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == Mode.AI) cancelAiStream();
                showHome();
            }
        });
        findViewById(R.id.nav_tabs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTabsDialog();
            }
        });
        bindNavBounce(findViewById(R.id.nav_back), backIcon);
        bindNavBounce(findViewById(R.id.nav_forward), forwardIcon);
        bindNavBounce(findViewById(R.id.nav_home), homeIcon);
        bindNavBounce(findViewById(R.id.nav_tabs), tabsIcon);
        downloadBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownloadsDialog();
                if (downloadBannerTerminal) hideDownloadBannerAnimated();
            }
        });
        aiFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == Mode.BROWSER) {
                    showAiQuickMenu();
                } else {
                    openAiFromBrowser();
                }
            }
        });
        aiFab.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        aiFab.animate().cancel();
                        aiFab.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).withLayer();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        aiFab.animate().cancel();
                        aiFab.animate().scaleX(1f).scaleY(1f).setDuration(240)
                                .setInterpolator(new OvershootInterpolator(2.2f)).withLayer();
                        break;
                }
                return false;
            }
        });
    }

    private void showAiFab(boolean show) {
        if (aiFab == null) return;
        if (show) {
            if (aiFab.getVisibility() == View.VISIBLE && aiFab.getAlpha() > 0.95f) return;
            aiFab.animate().cancel();
            aiFab.setVisibility(View.VISIBLE);
            aiFab.setAlpha(0f);
            aiFab.setScaleX(0.65f);
            aiFab.setScaleY(0.65f);
            aiFab.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(new OvershootInterpolator(1.8f))
                    .withLayer();
        } else {
            if (aiFab.getVisibility() != View.VISIBLE) return;
            aiFab.animate().cancel();
            aiFab.animate().alpha(0f).scaleX(0.65f).scaleY(0.65f)
                    .setDuration(150)
                    .setInterpolator(new DecelerateInterpolator())
                    .withLayer()
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (mode != Mode.BROWSER) aiFab.setVisibility(View.GONE);
                        }
                    });
        }
    }

    /** Down-scale on touch and a springy return on release. No layout work. */
    private void bindNavBounce(View target, final ImageView icon) {
        target.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        icon.animate().cancel();
                        icon.animate().scaleX(0.88f).scaleY(0.88f)
                                .setDuration(90).withLayer();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        icon.animate().cancel();
                        icon.animate().scaleX(1f).scaleY(1f)
                                .setDuration(250)
                                .setInterpolator(new OvershootInterpolator(2.2f))
                                .withLayer();
                        break;
                }
                return false;
            }
        });
    }

    // ---------------------------------------------------------------- tabs

    private BrowserTab createTab(boolean activate) {
        if (tabs.size() >= MAX_TABS) {
            Toast.makeText(this, R.string.tab_limit, Toast.LENGTH_SHORT).show();
            return null;
        }
        WebView webView = new WebView(this);
        BrowserTab tab = new BrowserTab(nextTabId++, webView);
        setupBrowserWebView(tab);
        tabs.add(tab);
        updateTabBadge();
        if (activate) showTab(tab);
        return tab;
    }

    private void showTab(BrowserTab tab) {
        if (tab == null) return;
        if (findActive) hideFindBar();
        if (currentTab != null && currentTab.webView.getParent() == browserFrame) {
            browserFrame.removeView(currentTab.webView);
            currentTab.webView.onPause();
        }
        currentTab = tab;
        browserFrame.removeAllViews();
        if (tab.webView.getParent() != null) {
            ((ViewGroup) tab.webView.getParent()).removeView(tab.webView);
        }
        browserFrame.addView(tab.webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tab.webView.onResume();
        if (tab.url == null || tab.url.isEmpty() || "about:blank".equals(tab.url)) {
            showHome();
        } else {
            showBrowser();
            // 标签切换：新页面淡入并轻微上浮，旧页面立即移除，避免白屏硬切。
            animateTabIn(tab.webView);
            installScrollWatcher(tab.webView);
        }
        updateTabBadge();
        updateChrome();
    }

    private void closeTab(BrowserTab tab) {
        if (tab == null) return;
        boolean wasCurrent = tab == currentTab;
        tabs.remove(tab);
        if (tab.webView.getParent() != null) {
            ((ViewGroup) tab.webView.getParent()).removeView(tab.webView);
        }
        try {
            tab.webView.stopLoading();
            tab.webView.destroy();
        } catch (Throwable ignored) {
        }
        if (wasCurrent) currentTab = null;
        if (tabs.isEmpty()) {
            if (tabsDialog != null) {
                tabsDialog.dismiss();
                tabsDialog = null;
            }
            createTab(true);
        } else if (wasCurrent) {
            showTab(tabs.get(0));
        }
        if (tabAdapter != null) {
            tabAdapter.setCurrentId(currentTab == null ? 0 : currentTab.id);
        }
        updateTabBadge();
        updateChrome();
    }

    private void showTabsDialog() {
        if (tabsDialog != null && tabsDialog.isShowing()) return;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tabs);
        dialog.setCanceledOnTouchOutside(true);
        tabsDialog = dialog;

        final ListView list = dialog.findViewById(R.id.tabs_list);
        tabAdapter = new TabAdapter(this, tabs, currentTab == null ? 0 : currentTab.id,
                new TabAdapter.Listener() {
                    @Override
                    public void onSelect(BrowserTab tab) {
                        showTab(tab);
                        dialog.dismiss();
                    }

                    @Override
                    public void onClose(BrowserTab tab) {
                        closeTab(tab);
                        if (tabAdapter != null) tabAdapter.setCurrentId(currentTab == null ? 0 : currentTab.id);
                    }
                });
        list.setAdapter(tabAdapter);

        dialog.findViewById(R.id.tabs_new).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserTab tab = createTab(true);
                if (tab != null) dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                tabsDialog = null;
                tabAdapter = null;
            }
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.45f);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            window.setLayout((int) (dm.widthPixels * 0.92f), (int) (dm.heightPixels * 0.72f));
        }
    }

    private void updateTabBadge() {
        if (tabBadge == null) return;
        int count = tabs.size();
        tabBadge.setText(String.valueOf(count));
        tabBadge.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------- browser

    @SuppressLint("SetJavaScriptEnabled")
    private void setupBrowserWebView(final BrowserTab tab) {
        final WebView webView = tab.webView;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setAppCacheEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setUserAgentString(settings.getUserAgentString() + " DeepExplorer/1.0");
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            settings.setForceDark(WebSettings.FORCE_DARK_OFF);
        }
        webView.setBackgroundColor(colorBg);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                showDownloadConfirmation(url, userAgent, contentDisposition, mimeType, webView.getUrl());
            }
        });

        applyWebSettingsFont(tab);
        tab.mobileUserAgent = settings.getUserAgentString();
        webView.setFindListener(new WebView.FindListener() {
            @Override
            public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                if (webView == currentWebView()) updateFindResult(activeMatchOrdinal, numberOfMatches);
            }
        });
        webView.addJavascriptInterface(new ScrollBridge(tab), "DeepExplorerScroll");
        webView.addJavascriptInterface(new ErrorBridge(tab), "ErrorPage");
        if (Build.VERSION.SDK_INT >= 23) {
            webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                private int lastY;
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    int dy = scrollY - lastY;
                    lastY = scrollY;
                    if (dy != 0) handlePageScroll(tab, dy, scrollY);
                }
            });
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleBrowserUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleBrowserUrl(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                BrowserTab tab = tabOf(view);
                if (tab != null && !tab.errorPageShowing) {
                    tab.url = url;
                    tab.networkError = false;
                }
                if (view == currentWebView() && findActive) hideFindBar();
                if (view == currentWebView() && mode == Mode.BROWSER && !"about:blank".equals(url)) {
                    progress.setVisibility(View.VISIBLE);
                    setChromeVisible(true, true);
                    view.animate().cancel();
                    view.setAlpha(0.86f);
                    updateChrome();
                }
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                injectWebsiteFont(view);
                if (view == currentWebView()) fadePageIn(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                BrowserTab tab = tabOf(view);
                if (tab != null) {
                    if (!tab.errorPageShowing) {
                        tab.url = url;
                        tab.networkError = false;
                        if (tab.title == null || tab.title.isEmpty()) tab.title = hostLabel(url);
                    }
                }
                if (view == currentWebView()) {
                    progress.setVisibility(View.GONE);
                    fadePageIn(view);
                    updateChrome();
                }
                injectWebsiteFont(view);
                installScrollWatcher(view);
                trimCacheIfNeeded();
                if (tabAdapter != null) tabAdapter.notifyDataSetChanged();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progress.setVisibility(View.GONE);
                    view.animate().cancel();
                    view.setAlpha(1f);
                    int code = Build.VERSION.SDK_INT >= 23 ? error.getErrorCode() : 0;
                    String failedUrl = request.getUrl() == null ? view.getUrl() : request.getUrl().toString();
                    BrowserTab failedTab = tabOf(view);
                    if (failedTab != null) {
                        failedTab.failedUrl = failedUrl;
                        failedTab.networkError = true;
                    }
                    boolean diagnosed = maybeShowNetworkDiagnostic(view, request, code);
                    boolean customPage = loadProxyErrorPage(view, failedUrl);
                    if (!diagnosed && !customPage && view == currentWebView() && mode == Mode.BROWSER) {
                        Toast.makeText(MainActivity.this, R.string.page_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                progress.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view == currentWebView() && mode == Mode.BROWSER) {
                    if (newProgress < 100) {
                        progress.setVisibility(View.VISIBLE);
                        progress.setProgress(newProgress);
                    } else {
                        progress.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                BrowserTab tab = tabOf(view);
                if (tab != null && title != null) {
                    tab.title = title;
                    if (tabAdapter != null) tabAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.deny();
            }
        });
    }

    private boolean handleBrowserUrl(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme();
        if (scheme == null) return true;
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "about".equalsIgnoreCase(scheme)) {
            return false;
        }
        if ("mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)
                || "sms".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) {
            }
            return true;
        }
        return true;
    }

    private BrowserTab tabOf(WebView view) {
        for (BrowserTab tab : tabs) {
            if (tab.webView == view) return tab;
        }
        return null;
    }

    private WebView currentWebView() {
        return currentTab == null ? null : currentTab.webView;
    }

    private void showBrowser() {
        hideKeyboard();
        if (mode == Mode.AI) cancelAiStream();
        mode = Mode.BROWSER;
        if (currentTab != null) currentTab.webView.onResume();
        updateHomeBackgroundVisibility();
        browserFrame.setVisibility(View.VISIBLE);
        topBar.setVisibility(View.VISIBLE);
        homeContent.setVisibility(View.GONE);
        aiWebView.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        resetChromePosition();
        showAiFab(true);
        updateChrome();
        syncConnectivityBanner();
        syncDownloadBanner();
    }

    private void showHome() {
        hideKeyboard();
        if (findActive) hideFindBar();
        if (mode == Mode.AI) cancelAiStream();
        mode = Mode.HOME;
        browserFrame.setVisibility(View.GONE);
        topBar.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        aiWebView.setVisibility(View.GONE);
        homeContent.setVisibility(View.VISIBLE);
        resetChromePosition();
        aiWebView.setAlpha(1f);
        aiWebView.setTranslationY(0f);
        setHomeLayout(false);
        animateHomeEntrance();
        showAiFab(false);
        hideConnectionBannerNow();
        hideDownloadBannerNow();
        updateHomeBackgroundVisibility();
        if (homeWidgets != null) homeWidgets.refresh();
        updateChrome();
    }

    private void showAi() {
        aiClosing = false;
        if (findActive) hideFindBar();
        boolean wasAi = mode == Mode.AI;
        if (!wasAi && currentTab != null) {
            try { currentTab.webView.onPause(); } catch (Throwable ignored) { }
        }
        mode = Mode.AI;
        browserFrame.setVisibility(View.GONE);
        topBar.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        aiWebView.setVisibility(View.VISIBLE);
        homeContent.setVisibility(View.VISIBLE);
        resetChromePosition();
        searchBox.animate().cancel();
        searchBox.setAlpha(1f);
        searchBox.setTranslationY(0f);
        setHomeLayout(true);
        applyAiTheme();
        showAiFab(false);
        hideConnectionBannerNow();
        hideDownloadBannerNow();
        updateHomeBackgroundVisibility();
        if (!wasAi) {
            aiWebView.animate().cancel();
            aiWebView.setAlpha(0f);
            aiWebView.setTranslationY(dp(8));
            aiWebView.setScaleX(0.985f);
            aiWebView.setScaleY(0.985f);
            aiWebView.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f));
        }
        updateChrome();
    }

    private void setHomeLayout(boolean ai) {
        FrameLayout.LayoutParams homeParams = (FrameLayout.LayoutParams) homeContent.getLayoutParams();
        homeParams.height = ai ? dp(84) : ViewGroup.LayoutParams.MATCH_PARENT;
        homeContent.setLayoutParams(homeParams);

        LinearLayout.LayoutParams searchParams = (LinearLayout.LayoutParams) searchBox.getLayoutParams();
        searchParams.topMargin = dp(ai ? 12 : 18);
        searchBox.setLayoutParams(searchParams);
        if (homeHeader != null) homeHeader.setVisibility(ai ? View.GONE : View.VISIBLE);
        if (homeWidgetsScroll != null) homeWidgetsScroll.setVisibility(ai ? View.GONE : View.VISIBLE);
    }

    private void animateHomeEntrance() {
        if (homeAppName == null || searchBox == null) return;
        homeAppName.animate().cancel();
        if (homeTagline != null) homeTagline.animate().cancel();
        searchBox.animate().cancel();
        homeAppName.setAlpha(0f);
        homeAppName.setTranslationY(dp(10));
        if (homeTagline != null) {
            homeTagline.setAlpha(0f);
            homeTagline.setTranslationY(dp(8));
        }
        searchBox.setAlpha(0f);
        searchBox.setTranslationY(dp(12));
        homeAppName.animate().alpha(1f).translationY(0f)
                .setDuration(340)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                .withLayer();
        if (homeTagline != null) {
            homeTagline.animate().alpha(1f).translationY(0f)
                    .setDuration(360)
                    .setStartDelay(30)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                    .withLayer();
        }
        searchBox.animate().alpha(1f).translationY(0f)
                .setDuration(400)
                .setStartDelay(65)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                .withLayer();
    }

    // ------------------------------------------------------------------ AI

    @SuppressLint("SetJavaScriptEnabled")
    private void setupAiWebView() {
        WebSettings settings = aiWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        aiWebView.setBackgroundColor(Color.TRANSPARENT);
        aiWebView.setVerticalScrollBarEnabled(false);
        aiWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        aiWebView.addJavascriptInterface(new AiBridge(), "AiNative");
        aiWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleAiUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleAiUrl(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                aiReady = true;
                applyAiTheme();
                applyModelLabel();
                applyAiStrings();
                if (pendingAiQuery != null && aiMode) {
                    String query = pendingAiQuery;
                    pendingAiQuery = null;
                    sendAi(query);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    aiReady = false;
                    pendingAiQuery = null;
                    Toast.makeText(MainActivity.this, R.string.ai_ui_load_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        aiWebView.loadUrl("file:///android_asset/ai.html");
    }

    private boolean handleAiUrl(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme();
        // The panel itself is loaded from file:///android_asset; model output must not
        // be able to navigate to another local file through a clicked link.
        if ("file".equalsIgnoreCase(scheme)) return true;
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            loadUrlInCurrent(uri.toString());
            return true;
        }
        if ("mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    private void setAiMode(boolean enabled) {
        if (aiMode == enabled) return;
        aiMode = enabled;
        SettingsStore.setAiMode(this, enabled);
        updateAiUi();
        if (!enabled) {
            pendingAiQuery = null;
            cancelAiStream();
            if (mode == Mode.AI) closeAiPanel();
        }
    }

    private void applyTheme() {
        String id = SettingsStore.getThemeId(this);
        int index = 0;
        for (int i = 0; i < THEME_IDS.length; i++) {
            if (THEME_IDS[i].equals(id)) {
                index = i;
                break;
            }
        }
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        colorPrimary = night ? THEME_PRIMARY_DARK[index] : THEME_PRIMARY_LIGHT[index];
        colorAccent = night ? THEME_ACCENT_DARK[index] : THEME_ACCENT_LIGHT[index];
        int[] bg = night ? THEME_BG_DARK[index] : THEME_BG_LIGHT[index];
        if (root != null) {
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, bg));
        }
        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(bg[0]);
            window.setNavigationBarColor(bg[0]);
        }
        if (homeAppName != null) homeAppName.setTextColor(colorPrimary);
        if (progress != null) {
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
        }
        applyAiFabTint();
        setupSearchBackground();
        updateAiUi();
        updateNavState();
        applyFont();
        if (homeWidgets != null) {
            homeWidgets.setAccent(colorPrimary);
            homeWidgets.setTypeface(currentTypeface);
            homeWidgets.refresh();
        }
    }

    private void applyAiFabTint() {
        if (aiFab == null) return;
        aiFab.setBackground(null);
        aiFab.setImageTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
    }

    private void applyFont() {
        String id = SettingsStore.getFontId(this);
        int index = 0;
        for (int i = 0; i < FONT_IDS.length; i++) {
            if (FONT_IDS[i].equals(id)) {
                index = i;
                break;
            }
        }
        try {
            currentTypeface = Typeface.create(FONT_FAMILIES[index], Typeface.NORMAL);
        } catch (Throwable ignored) {
            currentTypeface = Typeface.DEFAULT;
        }
        currentFontCss = cssFontFamily(index);
        if (homeAppName != null) homeAppName.setTypeface(currentTypeface);
        if (homeTagline != null) homeTagline.setTypeface(currentTypeface);
        if (searchInput != null) searchInput.setTypeface(currentTypeface);
        if (findInput != null) findInput.setTypeface(currentTypeface);
        if (urlText != null) urlText.setTypeface(currentTypeface);
        for (BrowserTab tab : tabs) {
            applyWebSettingsFont(tab);
            injectWebsiteFont(tab.webView);
        }
        applyAiTheme();
    }

    private String cssFontFamily(int index) {
        switch (index) {
            case 1:
                return "\"Noto Serif\", Georgia, \"Times New Roman\", serif";
            case 2:
                return "\"Roboto Mono\", \"Noto Sans Mono\", monospace";
            case 3:
                return "\"sans-serif-condensed\", \"Roboto Condensed\", \"Noto Sans\", sans-serif";
            case 4:
                return "\"sans-serif-light\", \"Roboto Light\", \"Noto Sans\", sans-serif";
            default:
                return "\"Roboto\", \"Noto Sans SC\", \"Noto Sans CJK SC\", sans-serif";
        }
    }

    private void applyWebSettingsFont(BrowserTab tab) {
        if (tab == null || tab.webView == null) return;
        String id = SettingsStore.getFontId(this);
        int index = 0;
        for (int i = 0; i < FONT_IDS.length; i++) {
            if (FONT_IDS[i].equals(id)) {
                index = i;
                break;
            }
        }
        String family = FONT_FAMILIES[index];
        WebSettings settings = tab.webView.getSettings();
        settings.setStandardFontFamily(family);
        settings.setSansSerifFontFamily(family);
        settings.setSerifFontFamily(index == 1 ? "serif" : family);
        settings.setFixedFontFamily("monospace");
    }

    private void injectWebsiteFont(WebView view) {
        if (view == null || currentFontCss == null) return;
        if ("sans".equals(SettingsStore.getFontId(this))) return;
        String css = "html,body,article,section,main,p,div,li,a,h1,h2,h3,h4,h5,h6,td,th,label,button,input,textarea,select{"
                + "font-family:" + currentFontCss + " !important;}"
                + "span:not([class*=\"icon\"]):not([class*=\"Icon\"]):not([class*=\"fa-\"]):not([class*=\"material\"]):not([class*=\"glyph\"]):not(.fa):not(.fas):not(.far):not(.fab){"
                + "font-family:" + currentFontCss + " !important;}"
                + "code,pre,kbd,samp,tt{font-family:monospace !important;}";
        String js = "(function(){try{var id='__deep_explorer_font';var old=document.getElementById(id);"
                + "if(old)old.remove();var s=document.createElement('style');s.id=id;s.textContent="
                + JSONObject.quote(css) + ";document.documentElement.appendChild(s);}catch(e){}})();";
        try {
            view.evaluateJavascript(js, null);
        } catch (Throwable ignored) {
        }
    }

    private void updateAiUi() {
        if (searchInput == null) return;
        updateSearchHint();
        aiButton.setBackground(null);
        applyAiSearchBackground(true);
    }

    private void setupSearchBackground() {
        Drawable base = getDrawable(R.drawable.bg_search);
        GradientDrawable fill = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorPrimary, colorAccent});
        fill.setCornerRadius(0f);
        searchFillClip = new ClipDrawable(fill, Gravity.RIGHT, ClipDrawable.HORIZONTAL);
        searchBox.setBackground(new LayerDrawable(new Drawable[]{base, searchFillClip}));
        searchFillFraction = aiMode ? 1f : 0f;
        searchFillClip.setLevel((int) (searchFillFraction * 10000));
        setSearchFillFraction(searchFillFraction);
        searchGradientActive = aiMode;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private void applyAiSearchBackground(boolean animate) {
        if (searchFillClip == null) return;
        final float target = aiMode ? 1f : 0f;
        if (searchFillAnimator != null) searchFillAnimator.cancel();
        searchGradientActive = aiMode;
        if (!animate) {
            setSearchFillFraction(target);
            return;
        }
        searchFillAnimator = ValueAnimator.ofFloat(searchFillFraction, target);
        searchFillAnimator.setDuration(380);
        searchFillAnimator.setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f));
        searchFillAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setSearchFillFraction((Float) animation.getAnimatedValue());
            }
        });
        searchFillAnimator.start();
    }

    /** Applies the blue reveal fraction to the fill, text, hint, star and divider. */
    private void setSearchFillFraction(float fraction) {
        searchFillFraction = Math.max(0f, Math.min(1f, fraction));
        if (searchFillClip != null) {
            searchFillClip.setLevel((int) (searchFillFraction * 10000));
        }
        ArgbEvaluator evaluator = new ArgbEvaluator();
        int textColor = (Integer) evaluator.evaluate(searchFillFraction,
                colorOnSurface, Color.WHITE);
        int hintColor = (Integer) evaluator.evaluate(searchFillFraction,
                colorOnSurfaceVariant, 0xE6FFFFFF);
        int iconColor = (Integer) evaluator.evaluate(searchFillFraction,
                colorOnSurfaceVariant, Color.WHITE);
        int dividerColor = (Integer) evaluator.evaluate(searchFillFraction,
                colorOutline, 0x66FFFFFF);
        if (searchInput != null) {
            searchInput.setTextColor(textColor);
            searchInput.setHintTextColor(hintColor);
        }
        if (aiIcon != null) {
            aiIcon.setImageTintList(android.content.res.ColorStateList.valueOf(iconColor));
        }
        if (engineChevron != null) {
            engineChevron.setImageTintList(android.content.res.ColorStateList.valueOf(iconColor));
        }
        if (searchDivider != null) {
            searchDivider.setBackgroundColor(dividerColor);
        }
    }

    private void updateEngineBadge() {
        if (engineBadge == null) return;
        SearchEngine engine = currentEngine == null ? SearchEngine.byId(null) : currentEngine;
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(engine.color);
        engineBadge.setBackground(circle);
        engineBadge.setText(engine.letter);
        engineBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, engine.letter.length() > 1 ? 8f : 11f);
        enginePicker.setContentDescription(getString(R.string.search_engine) + ": " + engine.name);
        updateSearchHint();
    }

    private void updateSearchHint() {
        if (searchInput == null) return;
        if (aiMode) {
            searchInput.setHint(R.string.ai_hint);
            return;
        }
        SearchEngine engine = currentEngine == null ? SearchEngine.byId(null) : currentEngine;
        // "360 搜索" would read as "用 360 搜索 搜索…"; trim only that suffix so a
        // custom engine literally named "我的搜索" keeps its name.
        String label = engine.name;
        if (label.endsWith(" 搜索")) label = label.substring(0, label.length() - 3);
        searchInput.setHint(getString(R.string.search_hint_engine, label));
    }

    /** Called by the home address widget; keeps widget taps inside the app. */
    void openFromWidget(String url) {
        if (url == null || url.isEmpty()) return;
        loadUrlInCurrent(url);
    }

    private void selectEngine(SearchEngine engine) {
        if (engine == null) return;
        currentEngine = engine;
        SettingsStore.setEngineId(this, engine.id);
        updateEngineBadge();
        if (enginePopup != null) enginePopup.dismiss();
    }

    private void showCustomEngineDialog() {
        if (enginePopup != null) enginePopup.dismiss();
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_widget_edit);
        TextView title = dialog.findViewById(R.id.widget_edit_title);
        TextView labelA = dialog.findViewById(R.id.widget_edit_label_a);
        TextView labelB = dialog.findViewById(R.id.widget_edit_label_b);
        final EditText name = dialog.findViewById(R.id.widget_edit_a);
        final EditText url = dialog.findViewById(R.id.widget_edit_b);
        title.setText(R.string.engine_custom_title);
        labelA.setText(R.string.engine_custom_name);
        labelB.setText(R.string.engine_custom_url);
        name.setHint(R.string.engine_custom_name_hint);
        url.setHint(R.string.engine_custom_url_hint);
        url.setSingleLine(true);
        url.setMinLines(1);
        url.setMaxLines(1);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        ViewGroup.LayoutParams urlParams = url.getLayoutParams();
        if (urlParams != null) {
            urlParams.height = dp(50);
            url.setLayoutParams(urlParams);
        }
        dialog.findViewById(R.id.widget_edit_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.widget_edit_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String engineName = name.getText().toString().trim();
                String template = url.getText().toString().trim();
                if (engineName.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.engine_custom_invalid_name,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if ((!template.startsWith("http://") && !template.startsWith("https://"))
                        || !template.contains("%s")) {
                    Toast.makeText(MainActivity.this, R.string.engine_custom_invalid_url,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                SearchEngine engine = EngineStore.add(MainActivity.this, engineName, template);
                engines = EngineStore.all(MainActivity.this);
                currentEngine = engine;
                SettingsStore.setEngineId(MainActivity.this, engine.id);
                updateEngineBadge();
                Toast.makeText(MainActivity.this, R.string.engine_custom_added,
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.34f);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min((int) (screenWidth * 0.92f), dp(430)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }

    private void confirmDeleteEngine(final SearchEngine engine) {
        if (engine == null || !engine.custom) return;
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.engine_custom_delete_title)
                .setMessage(engine.name)
                .setPositiveButton(R.string.engine_custom_delete_action,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                EngineStore.remove(MainActivity.this, engine.id);
                                engines = EngineStore.all(MainActivity.this);
                                if (currentEngine != null && currentEngine.id.equals(engine.id)) {
                                    currentEngine = EngineStore.byId(MainActivity.this, "google");
                                    SettingsStore.setEngineId(MainActivity.this, currentEngine.id);
                                    updateEngineBadge();
                                }
                                if (enginePopup != null) enginePopup.dismiss();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEnginePopup() {
        hideKeyboard();
        if (enginePopup != null && enginePopup.isShowing()) {
            enginePopup.dismiss();
            return;
        }
        final int popupWidth = Math.min(dp(304), getResources().getDisplayMetrics().widthPixels - dp(32));
        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_glass_panel);
        panel.setPadding(dp(8), dp(9), dp(8), dp(10));
        panel.setLayoutParams(new ViewGroup.LayoutParams(popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(R.string.search_engine);
        title.setTextColor(colorOnSurfaceVariant);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        title.setPadding(dp(8), 0, dp(8), dp(3));
        panel.addView(title);

        int total = engines.size();
        int rowCount = (total + 3) / 4;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            panel.addView(row);
            for (int column = 0; column < 4; column++) {
                int index = rowIndex * 4 + column;
                if (index >= total) break;
                final SearchEngine engine = engines.get(index);
                boolean selected = currentEngine != null && currentEngine.id.equals(engine.id);

                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(3), dp(7), dp(3), dp(7));
                cell.setBackgroundResource(selected ? R.drawable.bg_engine_cell_selected : R.drawable.bg_engine_cell);
                cell.setContentDescription(getString(R.string.engine_select, engine.name));
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, dp(66), 1f);
                cellParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                cell.setLayoutParams(cellParams);

                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(engine.color);
                TextView badge = new TextView(this);
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(30), dp(30));
                badge.setLayoutParams(badgeParams);
                badge.setBackground(circle);
                badge.setGravity(Gravity.CENTER);
                badge.setText(engine.letter);
                badge.setTextColor(Color.WHITE);
                badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, engine.letter.length() > 1 ? 8f : 12f);
                badge.setIncludeFontPadding(false);
                cell.addView(badge);

                TextView name = new TextView(this);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                nameParams.topMargin = dp(4);
                name.setLayoutParams(nameParams);
                name.setText(engine.name);
                name.setTextColor(selected ? colorPrimary : colorOnSurface);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
                name.setGravity(Gravity.CENTER);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                name.setIncludeFontPadding(false);
                cell.addView(name);

                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectEngine(engine);
                    }
                });
                if (engine.custom) {
                    cell.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            confirmDeleteEngine(engine);
                            return true;
                        }
                    });
                }
                row.addView(cell);
            }
        }

        TextView addCustom = new TextView(this);
        addCustom.setText("＋ " + getString(R.string.engine_custom_add));
        addCustom.setTextColor(colorPrimary);
        addCustom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        addCustom.setGravity(Gravity.CENTER_VERTICAL);
        addCustom.setPadding(dp(12), dp(12), dp(12), dp(10));
        addCustom.setBackgroundResource(R.drawable.bg_icon_ripple);
        addCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomEngineDialog();
            }
        });
        panel.addView(addCustom);

        PopupWindow popup = new PopupWindow(panel, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        if (Build.VERSION.SDK_INT >= 21) popup.setElevation(dp(12));
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                enginePopup = null;
            }
        });
        enginePopup = popup;
        panel.setAlpha(0f);
        panel.setScaleX(0.94f);
        panel.setScaleY(0.94f);
        panel.setTranslationY(-dp(6));
        panel.setPivotX(popupWidth / 2f);
        panel.setPivotY(0f);
        popup.showAsDropDown(enginePicker, -dp(8), dp(6));
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(230)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                .withLayer();
    }

    private void updateAiHistory() {
        while (aiHistory.length() > 12) aiHistory.remove(0);
        // The chat-completions API expects the conversation to start with a user
        // turn; trimming a single entry can leave an assistant message in front
        // and make the next request fail.
        while (aiHistory.length() > 0) {
            JSONObject first = aiHistory.optJSONObject(0);
            if (first != null && "user".equals(first.optString("role"))) break;
            aiHistory.remove(0);
        }
    }

    private void submitSearch() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) return;
        hideKeyboard();
        if (aiMode) {
            String key = SettingsStore.getApiKey(this);
            if (key.isEmpty()) {
                Toast.makeText(this, R.string.key_missing, Toast.LENGTH_SHORT).show();
                showSettings(true);
                return;
            }
            aiReturnMode = Mode.HOME;
            restoreAiModeAfterClose = false;
            showAi();
            if (!aiReady) {
                pendingAiQuery = query;
                return;
            }
            sendAi(query);
        } else {
            searchInput.setText("");
            searchInput.clearFocus();
            loadUrlInCurrent(query);
        }
    }

    private void sendAi(String query) {
        sendAi(query, null);
    }

    /**
     * @param apiContent optional longer text sent to the model while the chat bubble only
     *                   shows {@code query} (used by the page-summary action)
     */
    private void sendAi(String query, String apiContent) {
        if (!aiReady) {
            if (apiContent != null) {
                Toast.makeText(this, R.string.ai_summary_loading, Toast.LENGTH_SHORT).show();
                return;
            }
            pendingAiQuery = query;
            return;
        }
        final String endpoint = currentAiEndpoint();
        final String aiModel = currentAiModel();
        if (endpoint.isEmpty()) {
            Toast.makeText(this, R.string.ai_endpoint_invalid, Toast.LENGTH_SHORT).show();
            showSettings(false);
            return;
        }
        if (aiModel.isEmpty()) {
            Toast.makeText(this, R.string.ai_model_missing, Toast.LENGTH_SHORT).show();
            showSettings(false);
            return;
        }
        cancelAiStream();
        showAi();
        searchInput.setText("");
        searchInput.clearFocus();
        hideKeyboard();

        addHistory("user", apiContent != null ? apiContent : query);
        try {
            aiWebView.evaluateJavascript("AI.addUser(" + JSONObject.quote(query) + ");", null);
            aiWebView.evaluateJavascript("AI.startAssistant();", null);
        } catch (Throwable ignored) {
        }

        JSONArray messages = new JSONArray();
        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", getString(R.string.ai_system_prompt));
            messages.put(system);
            for (int i = 0; i < aiHistory.length(); i++) messages.put(aiHistory.get(i));
        } catch (Exception ignored) {
        }

        aiStreaming = true;
        final int seq = ++aiSeq;
        final String key = SettingsStore.getApiKey(this);
        aiClient.stream(endpoint, key, aiModel, messages, new AiClient.Listener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onChunk(final String text) {
                if (!aiStreaming || seq != aiSeq) return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed || seq != aiSeq || !aiStreaming || !aiReady) return;
                        aiWebView.evaluateJavascript("AI.append(" + JSONObject.quote(text) + ");", null);
                    }
                });
            }

            @Override
            public void onDone(final String fullText) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed || seq != aiSeq) return;
                        aiStreaming = false;
                        if (fullText == null || fullText.trim().isEmpty()) {
                            aiWebView.evaluateJavascript("AI.error(" + JSONObject.quote(getString(R.string.ai_empty_response)) + ");", null);
                            removeLastUser();
                            return;
                        }
                        aiWebView.evaluateJavascript("AI.finish();", null);
                        addHistory("assistant", fullText);
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed || seq != aiSeq) return;
                        aiStreaming = false;
                        aiWebView.evaluateJavascript("AI.error(" + JSONObject.quote(message) + ");", null);
                        removeLastUser();
                    }
                });
            }
        });
    }

    private void addHistory(String role, String content) {
        try {
            // A stopped request leaves a user turn without an assistant reply; drop it so
            // the next request never starts with two user messages in a row.
            if ("user".equals(role) && aiHistory.length() > 0) {
                JSONObject last = aiHistory.optJSONObject(aiHistory.length() - 1);
                if (last != null && "user".equals(last.optString("role"))) {
                    aiHistory.remove(aiHistory.length() - 1);
                }
            }
            JSONObject item = new JSONObject();
            item.put("role", role);
            item.put("content", content);
            aiHistory.put(item);
            updateAiHistory();
        } catch (Exception ignored) {
        }
    }

    private void removeLastUser() {
        try {
            if (aiHistory.length() == 0) return;
            int last = aiHistory.length() - 1;
            JSONObject item = aiHistory.optJSONObject(last);
            if (item != null && "user".equals(item.optString("role"))) {
                aiHistory.remove(last);
            }
        } catch (Exception ignored) {
        }
    }

    private void cancelAiStream() {
        aiStreaming = false;
        aiClient.cancel();
        if (aiReady) {
            try {
                aiWebView.evaluateJavascript("AI.finish();", null);
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyAiTheme() {
        if (!aiReady) return;
        try {
            String font = currentFontCss == null ? cssFontFamily(0) : currentFontCss;
            String js = "AI.setTheme(" + JSONObject.quote(colorHex(colorPrimary)) + ","
                    + JSONObject.quote(colorHex(colorAccent)) + ","
                    + JSONObject.quote(colorHex(colorOnPrimary)) + ","
                    + StorageUtils.isNight(this) + ");"
                    + "AI.setFont(" + JSONObject.quote(font) + ");";
            aiWebView.evaluateJavascript(js, null);
        } catch (Throwable ignored) {
        }
    }

    private String colorHex(int color) {
        return String.format(Locale.US, "#%06X", color & 0xFFFFFF);
    }

    // -------------------------------------------------------------- address

    private void loadUrlInCurrent(String input) {
        if (currentTab == null) {
            createTab(true);
            if (currentTab == null) return;
        }
        String url = normalizeUrl(input);
        currentTab.errorPageShowing = false;
        currentTab.networkError = false;
        currentTab.url = url;
        currentTab.title = hostLabel(url);
        showBrowser();
        progress.setVisibility(View.VISIBLE);
        updateChrome();
        currentTab.webView.loadUrl(url);
    }

    private String normalizeUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "https://www.google.com";
        String lower = value.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://")) return value;
        boolean hasSpace = value.indexOf(' ') >= 0 || value.indexOf('\n') >= 0;
        boolean localHostPort = value.matches("^[A-Za-z0-9.\\-]+:\\d+(/.*)?$");
        if (localHostPort) return "http://" + value;
        if (value.matches("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$")) return "http://" + value;
        if (lower.startsWith("localhost")) return "http://" + value;
        if (hasSpace || (!value.contains(".") && !value.contains(":"))) {
            try {
                SearchEngine engine = currentEngine == null ? SearchEngine.byId(null) : currentEngine;
                return engine.query(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return "https://www.google.com";
            }
        }
        return "https://" + value;
    }

    private String hostLabel(String url) {
        if (url == null || url.isEmpty()) return getString(R.string.new_tab_page);
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) return host;
        } catch (Exception ignored) {
        }
        return url;
    }

    private void resetChromePosition() {
        chromeVisible = true;
        if (browserFrame != null) {
            browserFrame.animate().cancel();
            browserFrame.setTranslationY(0f);
        }
        if (topBar != null) {
            topBar.animate().cancel();
            topBar.setTranslationY(0f);
            topBar.setAlpha(1f);
        }
        if (progress != null) {
            progress.animate().cancel();
            progress.setTranslationY(0f);
            progress.setAlpha(1f);
        }
    }

    /** Hardware translation/alpha only: no per-frame layout changes. */
    private void setChromeVisible(boolean visible, boolean animate) {
        if (chromeVisible == visible) return;
        chromeVisible = visible;
        float offset = visible ? 0f : -chromeHeight;
        float alpha = visible ? 1f : 0f;
        if (!animate) {
            topBar.setTranslationY(offset);
            topBar.setAlpha(alpha);
            progress.setTranslationY(offset);
            progress.setAlpha(alpha);
            return;
        }
        PathInterpolator curve = visible
                ? new PathInterpolator(0.2f, 0.9f, 0.2f, 1f)
                : new PathInterpolator(0.4f, 0f, 0.2f, 1f);
        int duration = visible ? 260 : 220;
        topBar.animate().cancel();
        progress.animate().cancel();
        topBar.animate().translationY(offset).alpha(alpha)
                .setDuration(duration).setInterpolator(curve).withLayer();
        progress.animate().translationY(offset).alpha(alpha)
                .setDuration(duration).setInterpolator(curve).withLayer();
    }

    private void handlePageScroll(BrowserTab tab, int dy, int y) {
        if (tab == null || tab != currentTab || mode != Mode.BROWSER) return;
        // A finished-download banner should never sit on top of the page the user is reading.
        if (downloadBannerVisible && downloadBannerTerminal && dy > 4) hideDownloadBannerAnimated();
        if (findActive || bannerVisible) return;
        if (y <= dp(6)) {
            if (!chromeVisible) setChromeVisible(true, true);
            return;
        }
        if (dy > 6 && chromeVisible && y > chromeHeight) {
            setChromeVisible(false, true);
        } else if (dy < -6 && !chromeVisible) {
            setChromeVisible(true, true);
        }
    }

    private void installScrollWatcher(WebView view) {
        if (view == null) return;
        String js = "(function(){if(window.__deScrollInstalled){return;}window.__deScrollInstalled=true;" +
                "var last=(window.scrollY||document.documentElement.scrollTop||document.body.scrollTop||0);" +
                "var pending=false;function emit(){pending=false;var y=(window.scrollY||document.documentElement.scrollTop||document.body.scrollTop||0);" +
                "var dy=y-last;last=y;try{if(window.DeepExplorerScroll){window.DeepExplorerScroll.onScroll(dy,y);}}catch(e){}}" +
                "window.addEventListener('scroll',function(){if(pending){return;}pending=true;" +
                "if(window.requestAnimationFrame){window.requestAnimationFrame(emit);}else{emit();}},{passive:true});})();";
        try {
            view.evaluateJavascript(js, null);
        } catch (Throwable ignored) {
        }
    }

    private void animateTabIn(WebView view) {
        if (view == null) return;
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(dp(7));
        view.setScaleX(0.995f);
        view.setScaleY(0.995f);
        view.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                .start();
    }

    private void fadePageIn(WebView view) {
        if (view == null) return;
        view.animate().cancel();
        view.animate().alpha(1f).setDuration(150)
                .setInterpolator(new DecelerateInterpolator());
    }

    private final class ScrollBridge {
        private final BrowserTab tab;

        ScrollBridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void onScroll(final int dy, final int y) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!destroyed) handlePageScroll(tab, dy, y);
                }
            });
        }
    }

    private void updateChrome() {
        if (urlText == null) return;
        String url = currentTab == null ? null : currentTab.url;
        if (url == null || url.isEmpty() || "about:blank".equals(url)) {
            urlText.setText(R.string.new_tab_page);
            urlIcon.setImageResource(R.drawable.ic_globe);
        } else {
            urlText.setText(url);
            urlIcon.setImageResource(url.startsWith("https://") ? R.drawable.ic_lock : R.drawable.ic_globe);
        }
        urlIcon.setColorFilter(colorOnSurfaceVariant);
        updateNavState();
    }

    private void updateNavState() {
        boolean canBack = mode == Mode.AI || (currentTab != null && currentTab.webView.canGoBack());
        boolean canForward = mode != Mode.AI && currentTab != null && currentTab.webView.canGoForward();
        backIcon.setColorFilter(canBack ? colorPrimary : colorOnSurfaceVariant);
        backIcon.setAlpha(canBack ? 1f : 0.4f);
        forwardIcon.setColorFilter(canForward ? colorPrimary : colorOnSurfaceVariant);
        forwardIcon.setAlpha(canForward ? 1f : 0.4f);
        homeIcon.setColorFilter(mode == Mode.HOME ? colorPrimary : colorOnSurfaceVariant);
        tabsIcon.setColorFilter(colorOnSurfaceVariant);
    }

    private final class AiBridge {
        @JavascriptInterface
        public void openModelSheet() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (SettingsStore.isCustomAi(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, R.string.ai_custom_model_hint_toast,
                                Toast.LENGTH_SHORT).show();
                        showAiEndpointDialog();
                    } else {
                        showModelSheet();
                    }
                }
            });
        }

        @JavascriptInterface
        public void closeChat() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    closeAiPanel();
                }
            });
        }
    }

    private void showAiQuickMenu() {
        if (aiQuickPopup != null && aiQuickPopup.isShowing()) {
            aiQuickPopup.dismiss();
            return;
        }
        final int width = dp(208);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_glass_panel);
        panel.setPadding(dp(6), dp(6), dp(6), dp(6));
        panel.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(menuRow(R.drawable.ic_ai, getString(R.string.ai_ask), false,
                new Runnable() {
                    @Override
                    public void run() {
                        openAiFromBrowser();
                    }
                }));
        panel.addView(menuRow(R.drawable.ic_globe, getString(R.string.ai_summary), false,
                new Runnable() {
                    @Override
                    public void run() {
                        summarizeCurrentPage();
                    }
                }));
        PopupWindow popup = new PopupWindow(panel, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        if (Build.VERSION.SDK_INT >= 21) popup.setElevation(dp(12));
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                aiQuickPopup = null;
            }
        });
        aiQuickPopup = popup;
        panel.setAlpha(0f);
        panel.setScaleX(0.94f);
        panel.setScaleY(0.94f);
        popup.showAtLocation(aiFab, Gravity.BOTTOM | Gravity.END, dp(16), dp(120));
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
    }

    /** Pulls the visible text out of the current page and asks the AI to summarize it. */
    private void summarizeCurrentPage() {
        if (mode != Mode.BROWSER || currentTab == null) {
            Toast.makeText(this, R.string.no_page, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!aiReady) {
            Toast.makeText(this, R.string.ai_panel_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        if (SettingsStore.getApiKey(this).isEmpty()) {
            Toast.makeText(this, R.string.key_missing, Toast.LENGTH_SHORT).show();
            showSettings(true);
            return;
        }
        final BrowserTab tab = currentTab;
        Toast.makeText(this, R.string.ai_summary_loading, Toast.LENGTH_SHORT).show();
        tab.webView.evaluateJavascript(
                "(function(){try{var t=document.body?document.body.innerText:'';"
                        + "return t.length>8000?t.substring(0,8000):t;}catch(e){return '';}})()",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        String text = "";
                        try {
                            Object parsed = new org.json.JSONTokener(
                                    value == null ? "\"\"" : value).nextValue();
                            text = parsed == null ? "" : parsed.toString();
                        } catch (Throwable ignored) {
                        }
                        text = text.replaceAll("\\s+", " ").trim();
                        if (text.length() < 80) {
                            Toast.makeText(MainActivity.this, R.string.ai_summary_empty,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (text.length() > 6000) text = text.substring(0, 6000);
                        String title = tab.title == null || tab.title.trim().isEmpty()
                                ? getString(R.string.web_page) : tab.title.trim();
                        String url = tab.url == null ? "" : tab.url;
                        String prompt = getString(R.string.ai_summary_prompt, title, url, text);
                        openAiFromBrowser();
                        sendAi(getString(R.string.ai_summary_title, title), prompt);
                    }
                });
    }

    private void openAiFromBrowser() {
        if (mode != Mode.BROWSER) return;
        aiReturnMode = Mode.BROWSER;
        restoreAiModeAfterClose = !aiMode;
        if (!aiMode) {
            aiMode = true;
            SettingsStore.setAiMode(this, true);
            updateAiUi();
        }
        showAi();
        searchInput.requestFocus();
        showKeyboard(searchInput);
    }

    private void closeAiPanel() {
        if (mode != Mode.AI || aiClosing) return;
        aiClosing = true;
        hideKeyboard();
        cancelAiStream();
        final boolean toBrowser = aiReturnMode == Mode.BROWSER
                && currentTab != null && currentTab.url != null && !currentTab.url.isEmpty();
        if (toBrowser) {
            // Reveal the live page behind the fading AI panel instead of cutting to it.
            mode = Mode.BROWSER;
            if (currentTab != null) {
                try { currentTab.webView.onResume(); } catch (Throwable ignored) { }
            }
            browserFrame.setVisibility(View.VISIBLE);
            topBar.setVisibility(View.VISIBLE);
            homeContent.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
            resetChromePosition();
            updateChrome();
        }
        aiWebView.animate().cancel();
        aiWebView.animate().alpha(0f).scaleX(0.985f).scaleY(0.985f).translationY(dp(8))
                .setDuration(190)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed) return;
                        aiWebView.setVisibility(View.GONE);
                        aiWebView.setAlpha(1f);
                        aiWebView.setScaleX(1f);
                        aiWebView.setScaleY(1f);
                        aiWebView.setTranslationY(0f);
                        aiClosing = false;
                        if (toBrowser) {
                            if (restoreAiModeAfterClose) {
                                restoreAiModeAfterClose = false;
                                aiMode = false;
                                SettingsStore.setAiMode(MainActivity.this, false);
                                updateAiUi();
                            }
                            if (mode == Mode.BROWSER) showAiFab(true);
                            else showAiFab(false);
                        } else {
                            showHome();
                        }
                    }
                }).start();
    }

    private String currentAiEndpoint() {
        if (!SettingsStore.isCustomAi(this)) {
            return "https://api.deepseek.com/chat/completions";
        }
        String base = SettingsStore.getAiBaseUrl(this);
        if (base.isEmpty()) return "";
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.endsWith("/chat/completions")) return base;
        return base + "/chat/completions";
    }

    private String currentAiModel() {
        if (!SettingsStore.isCustomAi(this)) return selectedModel == null ? "" : selectedModel;
        return SettingsStore.getAiModel(this);
    }

    private String modelLabel() {
        if (SettingsStore.isCustomAi(this)) {
            String custom = SettingsStore.getAiModel(this);
            return custom.isEmpty() ? getString(R.string.custom_model) : custom;
        }
        return SettingsStore.MODEL_REASONER.equals(selectedModel) ? "DeepSeek Reasoner" : "DeepSeek Chat";
    }

    private void applyAiStrings() {
        try {
            String js = "AI.setStrings({thinking:" + JSONObject.quote(getString(R.string.ai_thinking))
                    + ",interrupted:" + JSONObject.quote(getString(R.string.ai_interrupted))
                    + ",requestFailed:" + JSONObject.quote(getString(R.string.ai_request_failed))
                    + ",close:" + JSONObject.quote(getString(R.string.close))
                    + ",emptyBody:" + JSONObject.quote(getString(R.string.ai_empty_body)) + "});";
            aiWebView.evaluateJavascript(js, null);
        } catch (Throwable ignored) {
        }
    }

    private void applyModelLabel() {
        if (!aiReady) return;
        try {
            aiWebView.evaluateJavascript("AI.setModel(" + JSONObject.quote(modelLabel()) + ");", null);
        } catch (Throwable ignored) {
        }
    }

    private void showModelSheet() {
        if (modelDialog != null && modelDialog.isShowing()) return;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_model_sheet);
        dialog.setCancelable(false);
        modelDialog = dialog;
        modelSheetClosing = false;

        final View sheet = dialog.findViewById(R.id.model_sheet);
        final View scrim = dialog.findViewById(R.id.model_scrim);
        updateModelChecks(dialog);
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeModelSheet();
            }
        });
        dialog.findViewById(R.id.model_chat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectModel(SettingsStore.MODEL_CHAT);
            }
        });
        dialog.findViewById(R.id.model_reasoner).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectModel(SettingsStore.MODEL_REASONER);
            }
        });
        dialog.setOnKeyListener(new android.content.DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface d, int keyCode, android.view.KeyEvent event) {
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                        && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                    closeModelSheet();
                    return true;
                }
                return false;
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (modelDialog == dialog) modelDialog = null;
                modelSheetClosing = false;
            }
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.TOP);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0f);
        }
        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        sheet.setTranslationY(screenHeight);
        scrim.setAlpha(0f);
        sheet.post(new Runnable() {
            @Override
            public void run() {
                sheet.animate().translationY(0f)
                        .setDuration(330)
                        .setInterpolator(new OvershootInterpolator(0.55f))
                        .withLayer();
                scrim.animate().alpha(1f).setDuration(240).start();
            }
        });
    }

    private void updateModelChecks(Dialog dialog) {
        if (dialog == null) return;
        View chat = dialog.findViewById(R.id.model_check_chat);
        View reasoner = dialog.findViewById(R.id.model_check_reasoner);
        if (chat != null) chat.setVisibility(SettingsStore.MODEL_REASONER.equals(selectedModel) ? View.INVISIBLE : View.VISIBLE);
        if (reasoner != null) reasoner.setVisibility(SettingsStore.MODEL_REASONER.equals(selectedModel) ? View.VISIBLE : View.INVISIBLE);
    }

    private void selectModel(String model) {
        selectedModel = model;
        SettingsStore.setModel(this, model);
        if (aiStreaming) cancelAiStream();
        applyModelLabel();
        closeModelSheet();
    }

    private void closeModelSheet() {
        final Dialog dialog = modelDialog;
        if (dialog == null || modelSheetClosing) return;
        modelSheetClosing = true;
        final View sheet = dialog.findViewById(R.id.model_sheet);
        final View scrim = dialog.findViewById(R.id.model_scrim);
        if (sheet != null) {
            sheet.animate().translationY(sheet.getHeight() > 0 ? sheet.getHeight() : dp(280))
                    .setDuration(260)
                    .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f))
                    .withLayer()
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (dialog.isShowing()) dialog.dismiss();
                        }
                    });
        } else if (dialog.isShowing()) {
            dialog.dismiss();
        }
        if (scrim != null) scrim.animate().alpha(0f).setDuration(200).start();
    }

    private String localizeErrorPage(String html) {
        String lang = LocaleHelper.htmlLang(LocaleHelper.getLanguage(this));
        return html.replace("<html lang=\"zh-CN\">", "<html lang=\"" + lang + "\">")
                .replace("网络连接超时", getString(R.string.network_diag_title))
                .replace("无法连接到该网站。请检查您的代理或防火墙设置。",
                        getString(R.string.network_diag_message))
                .replace(">重试<", ">" + getString(R.string.retry) + "<")
                .replace(">深探<", ">" + getString(R.string.app_name) + "<");
    }

    private boolean loadProxyErrorPage(WebView view, String failedUrl) {
        BrowserTab tab = tabOf(view);
        if (tab == null || view == null) return false;
        tab.failedUrl = failedUrl;
        tab.errorPageShowing = true;
        tab.networkError = true;
        // Use a neutral origin: loading the local error page with the failed site's
        // URL as base would give our HTML that site's origin (cookies, storage).
        String base = "file:///android_asset/";
        try {
            view.loadDataWithBaseURL(base, readErrorPageAsset(), "text/html", "UTF-8", base);
            return true;
        } catch (Throwable ignored) {
            tab.errorPageShowing = false;
            return false;
        }
    }

    private String readErrorPageAsset() {
        if (cachedErrorPageHtml != null) return cachedErrorPageHtml;
        InputStream input = null;
        ByteArrayOutputStream output = null;
        try {
            input = getAssets().open("error.html");
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            cachedErrorPageHtml = localizeErrorPage(
                    new String(output.toByteArray(), "UTF-8"));
            return cachedErrorPageHtml;
        } catch (Throwable ignored) {
            cachedErrorPageHtml = "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                    + "<body style=\"font-family:sans-serif;padding:32px;color:#241A33;background:#FBF7FF\">"
                    + "<h2>" + getString(R.string.network_diag_title) + "</h2><p>"
                    + getString(R.string.network_diag_message) + "</p></body></html>";
            return cachedErrorPageHtml;
        } finally {
            if (input != null) try { input.close(); } catch (Throwable ignored) { }
            if (output != null) try { output.close(); } catch (Throwable ignored) { }
        }
    }

    private final class ErrorBridge {
        private final BrowserTab tab;

        ErrorBridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void retry() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // The bridge is injected into every page, so only honour the
                    // call while our own error page is actually on screen.
                    if (tab == null || !tab.errorPageShowing
                            || tab.failedUrl == null || tab.failedUrl.isEmpty()) return;
                    if (!isOnline()) {
                        onlineState = false;
                        showOfflineBanner();
                        return;
                    }
                    tab.errorPageShowing = false;
                    tab.networkError = false;
                    if (tab == currentTab) {
                        loadUrlInCurrent(tab.failedUrl);
                    } else {
                        tab.webView.loadUrl(tab.failedUrl);
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------- downloads

    private static final class PendingDownload {
        final String url;
        final String userAgent;
        final String contentDisposition;
        final String mimeType;
        final String referer;
        final String site;
        final String fileName;

        PendingDownload(String url, String userAgent, String contentDisposition,
                        String mimeType, String referer, String site, String fileName) {
            this.url = url;
            this.userAgent = userAgent;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimeType;
            this.referer = referer;
            this.site = site;
            this.fileName = fileName;
        }
    }

    private static final class DownloadInfo {
        long id;
        String title;
        String mime;
        int status;
        long done;
        long total;
        long speed;
    }

    private void showDownloadConfirmation(String url, String userAgent, String contentDisposition,
                                          String mimeType, String referer) {
        if (url == null || url.isEmpty()) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (downloadConfirmDialog != null && downloadConfirmDialog.isShowing()) return;
        String site = hostOf(referer);
        if (site == null || site.isEmpty()) site = hostOf(url);
        if (site == null || site.isEmpty()) site = getString(R.string.download_site_unknown);
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        fileName = sanitizeFileName(fileName);
        final PendingDownload pending = new PendingDownload(url, userAgent, contentDisposition,
                mimeType, referer, site, fileName);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_download_confirm);
        dialog.setCancelable(true);
        downloadConfirmDialog = dialog;
        TextView message = dialog.findViewById(R.id.download_confirm_message);
        message.setText(getString(R.string.download_warning, site));
        dialog.findViewById(R.id.download_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.download_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startDownload(pending);
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (downloadConfirmDialog == dialog) downloadConfirmDialog = null;
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.34f);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min((int) (screenWidth * 0.9f), dp(420)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        View panel = dialog.findViewById(android.R.id.content);
        if (panel != null) {
            panel.setAlpha(0f);
            panel.animate().alpha(1f).setDuration(180).start();
        }
    }

    private String hostOf(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return Uri.parse(raw).getHost();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "download";
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safe.isEmpty()) return "download";
        return safe.length() > 120 ? safe.substring(0, 120) : safe;
    }

    private void startDownload(PendingDownload pending) {
        if (pending == null) return;
        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            this.pendingDownload = pending;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQ_DOWNLOAD_PERMISSION);
            return;
        }
        enqueueDownload(pending);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_DOWNLOAD_PERMISSION) return;
        PendingDownload pending = this.pendingDownload;
        this.pendingDownload = null;
        if (pending == null) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownload(pending);
        } else {
            Toast.makeText(this, R.string.download_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    private void enqueueDownload(PendingDownload pending) {
        if (downloadManager == null) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pending.url));
            if (pending.userAgent != null && !pending.userAgent.isEmpty()) {
                request.addRequestHeader("User-Agent", pending.userAgent);
            }
            if (pending.referer != null && !pending.referer.isEmpty()) {
                request.addRequestHeader("Referer", pending.referer);
            }
            String cookie = CookieManager.getInstance().getCookie(pending.url);
            if (cookie != null && !cookie.isEmpty()) request.addRequestHeader("Cookie", cookie);
            request.setTitle(pending.fileName);
            request.setDescription(getString(R.string.download_running));
            if (pending.mimeType != null && !pending.mimeType.isEmpty()) {
                request.setMimeType(pending.mimeType);
            }
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            try {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, pending.fileName);
            } catch (Throwable t) {
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS,
                        pending.fileName);
            }
            long id = downloadManager.enqueue(request);
            latestDownloadId = id;
            if (!downloadIds.contains(id)) downloadIds.add(0, id);
            while (downloadIds.size() > 30) downloadIds.remove(downloadIds.size() - 1);
            saveDownloadIds();
            showDownloadBannerStart();
            startDownloadPolling();
        } catch (Throwable t) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDownloadIds() {
        downloadIds.clear();
        String raw = SettingsStore.getDownloadIds(this);
        if (raw == null || raw.isEmpty()) return;
        for (String part : raw.split(",")) {
            try {
                long id = Long.parseLong(part.trim());
                if (id > 0 && !downloadIds.contains(id)) downloadIds.add(id);
            } catch (Throwable ignored) {
            }
        }
        if (!downloadIds.isEmpty()) latestDownloadId = downloadIds.get(0);
    }

    private void saveDownloadIds() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < downloadIds.size(); i++) {
            if (i > 0) builder.append(',');
            builder.append(downloadIds.get(i));
        }
        SettingsStore.setDownloadIds(this, builder.toString());
    }

    private DownloadInfo queryDownload(long id) {
        if (downloadManager == null) return null;
        Cursor cursor = null;
        try {
            cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
            if (cursor == null || !cursor.moveToFirst()) return null;
            DownloadInfo info = new DownloadInfo();
            info.id = id;
            info.title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
            info.mime = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            info.status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            int doneIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            info.done = doneIndex >= 0 && !cursor.isNull(doneIndex) ? cursor.getLong(doneIndex) : 0L;
            info.total = totalIndex >= 0 && !cursor.isNull(totalIndex) ? cursor.getLong(totalIndex) : 0L;
            info.speed = sampleDownloadSpeed(info);
            return info;
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (cursor != null) try { cursor.close(); } catch (Throwable ignored) { }
        }
    }

    private long sampleDownloadSpeed(DownloadInfo info) {
        long now = System.currentTimeMillis();
        Long previousBytes = downloadLastBytes.get(info.id);
        Long previousTime = downloadLastSampleTime.get(info.id);
        long speed = 0L;
        if (info.status == DownloadManager.STATUS_RUNNING && previousBytes != null && previousTime != null
                && now > previousTime && info.done >= previousBytes) {
            speed = (info.done - previousBytes) * 1000L / Math.max(1L, now - previousTime);
        }
        downloadLastBytes.put(info.id, info.done);
        downloadLastSampleTime.put(info.id, now);
        downloadSpeeds.put(info.id, speed);
        return speed;
    }

    private void showDownloadBannerStart() {
        if (mode != Mode.BROWSER || downloadBanner == null) return;
        handler.removeCallbacks(downloadBannerAutoHide);
        downloadBannerComplete = false;
        downloadBannerTerminal = false;
        setDownloadBannerColor(DOWNLOAD_BLUE);
        downloadBannerIcon.setImageResource(R.drawable.ic_download);
        downloadBannerIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        downloadBannerText.setText(R.string.download_start);
        downloadBannerBaseY = bannerVisible ? dp(48) : 0f;
        if (!downloadBannerVisible) {
            downloadBannerVisible = true;
            downloadBanner.setVisibility(View.VISIBLE);
            downloadBanner.animate().cancel();
            downloadBanner.setAlpha(0f);
            downloadBanner.setTranslationY(downloadBannerBaseY - dp(12));
            downloadBanner.animate().alpha(1f).translationY(downloadBannerBaseY)
                    .setDuration(260)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                    .withLayer();
        } else {
            downloadBanner.setTranslationY(downloadBannerBaseY);
        }
    }

    private void showDownloadBannerComplete() {
        if (downloadBanner == null) return;
        if (mode != Mode.BROWSER) {
            hideDownloadBannerNow();
            return;
        }
        downloadBannerComplete = true;
        downloadBannerTerminal = true;
        if (!downloadBannerVisible) {
            downloadBannerVisible = true;
            downloadBanner.setVisibility(View.VISIBLE);
            downloadBannerBaseY = bannerVisible ? dp(48) : 0f;
            setDownloadBannerColor(DOWNLOAD_GREEN);
            downloadBanner.setTranslationY(downloadBannerBaseY);
            downloadBanner.setAlpha(1f);
        } else {
            animateDownloadBannerColor(DOWNLOAD_GREEN);
        }
        downloadBannerIcon.setImageResource(R.drawable.ic_check);
        downloadBannerIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        downloadBannerText.setText(R.string.download_complete);
        scheduleDownloadBannerAutoHide();
    }

    private void showDownloadBannerFailed() {
        if (downloadBanner == null) return;
        if (mode != Mode.BROWSER) {
            hideDownloadBannerNow();
            return;
        }
        downloadBannerComplete = false;
        downloadBannerTerminal = true;
        if (!downloadBannerVisible) {
            downloadBannerVisible = true;
            downloadBanner.setVisibility(View.VISIBLE);
            downloadBannerBaseY = bannerVisible ? dp(48) : 0f;
            downloadBanner.setTranslationY(downloadBannerBaseY);
            downloadBanner.setAlpha(1f);
        }
        setDownloadBannerColor(DOWNLOAD_RED);
        downloadBannerIcon.setImageResource(R.drawable.ic_error_cross);
        downloadBannerIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        downloadBannerText.setText(R.string.download_failed);
        scheduleDownloadBannerAutoHide();
    }

    private void setDownloadBannerColor(int color) {
        if (downloadBannerColorAnimator != null) downloadBannerColorAnimator.cancel();
        downloadBannerColor = color;
        downloadBanner.setBackground(downloadBannerDrawable(color));
    }

    private void animateDownloadBannerColor(int target) {
        if (downloadBannerColorAnimator != null) downloadBannerColorAnimator.cancel();
        final int from = downloadBannerColor;
        downloadBannerColorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), from, target);
        downloadBannerColorAnimator.setDuration(320);
        downloadBannerColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                downloadBannerColor = (Integer) animation.getAnimatedValue();
                downloadBanner.setBackground(downloadBannerDrawable(downloadBannerColor));
            }
        });
        downloadBannerColorAnimator.start();
    }

    private GradientDrawable downloadBannerDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private void updateDownloadBannerBase(boolean animate) {
        if (downloadBanner == null) return;
        downloadBannerBaseY = bannerVisible ? dp(48) : 0f;
        if (!downloadBannerVisible) return;
        downloadBanner.animate().cancel();
        if (animate) {
            downloadBanner.animate().translationY(downloadBannerBaseY)
                    .setDuration(200)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                    .withLayer();
        } else {
            downloadBanner.setTranslationY(downloadBannerBaseY);
        }
    }

    private void scheduleDownloadBannerAutoHide() {
        handler.removeCallbacks(downloadBannerAutoHide);
        handler.postDelayed(downloadBannerAutoHide, DOWNLOAD_BANNER_AUTO_HIDE_MS);
    }

    private final Runnable downloadBannerAutoHide = new Runnable() {
        @Override
        public void run() {
            hideDownloadBannerAnimated();
        }
    };

    /** Fades a finished-download banner away and remembers that this download was seen. */
    private void hideDownloadBannerAnimated() {
        handler.removeCallbacks(downloadBannerAutoHide);
        if (downloadBannerTerminal && latestDownloadId > 0) {
            downloadBannerDismissedId = latestDownloadId;
            SettingsStore.setBannerDismissedId(this, downloadBannerDismissedId);
        }
        downloadBannerTerminal = false;
        downloadBannerVisible = false;
        if (downloadBanner == null) return;
        downloadBanner.animate().cancel();
        downloadBanner.animate().alpha(0f)
                .translationY(downloadBannerBaseY - dp(10))
                .setDuration(200)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (downloadBannerVisible) return;
                        downloadBanner.setVisibility(View.GONE);
                        downloadBanner.setAlpha(1f);
                        downloadBanner.setTranslationY(downloadBannerBaseY);
                    }
                });
    }

    private void hideDownloadBannerNow() {
        handler.removeCallbacks(downloadBannerAutoHide);
        if (downloadBannerTerminal && latestDownloadId > 0) {
            downloadBannerDismissedId = latestDownloadId;
            SettingsStore.setBannerDismissedId(this, downloadBannerDismissedId);
        }
        downloadBannerTerminal = false;
        downloadBannerVisible = false;
        if (downloadBanner == null) return;
        downloadBanner.animate().cancel();
        downloadBanner.setVisibility(View.GONE);
    }

    private void startDownloadPolling() {
        if (downloadPolling) return;
        downloadPolling = true;
        handler.post(downloadPollRunnable);
    }

    private final Runnable downloadPollRunnable = new Runnable() {
        @Override
        public void run() {
            downloadPolling = false;
            // Only poll while the banner can be seen. onPause removes this callback
            // and onResume restarts it through syncDownloadBanner(), so a paused or
            // queued download can no longer keep the app awake in the background.
            if (destroyed || latestDownloadId <= 0 || mode != Mode.BROWSER) return;
            DownloadInfo info = queryDownload(latestDownloadId);
            if (info == null) return;
            if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
                showDownloadBannerComplete();
                return;
            }
            if (info.status == DownloadManager.STATUS_FAILED) {
                showDownloadBannerFailed();
                return;
            }
            showDownloadBannerStart();
            downloadPolling = true;
            handler.postDelayed(this, 700);
        }
    };

    private void syncDownloadBanner() {
        if (mode != Mode.BROWSER || latestDownloadId <= 0) return;
        DownloadInfo info = queryDownload(latestDownloadId);
        if (info == null) return;
        if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
            if (latestDownloadId == downloadBannerDismissedId) {
                hideDownloadBannerNow();
                return;
            }
            showDownloadBannerComplete();
        } else if (info.status == DownloadManager.STATUS_FAILED) {
            if (latestDownloadId == downloadBannerDismissedId) {
                hideDownloadBannerNow();
                return;
            }
            showDownloadBannerFailed();
        } else {
            showDownloadBannerStart();
            startDownloadPolling();
        }
    }

    private void showDownloadsDialog() {
        if (pageVisible && "downloads".equals(pageKind)) return;
        openPage(R.string.downloads, "downloads");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(6), dp(18), dp(20));
        downloadsEmpty = new TextView(this);
        downloadsEmpty.setText(R.string.download_empty);
        downloadsEmpty.setTextColor(colorOnSurfaceVariant);
        downloadsEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        downloadsEmpty.setGravity(Gravity.CENTER);
        downloadsEmpty.setPadding(dp(12), dp(56), dp(12), dp(56));
        root.addView(downloadsEmpty);
        downloadsList = new LinearLayout(this);
        downloadsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(downloadsList);
        downloadsScroll = new ScrollView(this);
        downloadsScroll.setFillViewport(true);
        downloadsScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        downloadsScroll.addView(root);
        pageContent.addView(downloadsScroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        renderDownloads();
        handler.removeCallbacks(downloadsTickRunnable);
        handler.postDelayed(downloadsTickRunnable, 900);
    }

    private final Runnable downloadsTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!pageVisible || !"downloads".equals(pageKind)) return;
            renderDownloads();
            handler.postDelayed(this, 900);
        }
    };

    private void renderDownloads() {
        if (downloadsList == null) return;
        int savedScroll = downloadsScroll == null ? 0 : downloadsScroll.getScrollY();
        downloadsList.removeAllViews();
        List<Long> valid = new ArrayList<>();
        for (Long id : new ArrayList<>(downloadIds)) {
            DownloadInfo info = queryDownload(id);
            if (info == null) continue;
            valid.add(id);
            downloadsList.addView(buildDownloadRow(info));
        }
        if (!valid.equals(downloadIds)) {
            downloadIds.clear();
            downloadIds.addAll(valid);
            saveDownloadIds();
        }
        downloadsEmpty.setVisibility(valid.isEmpty() ? View.VISIBLE : View.GONE);
        if (downloadsScroll != null) downloadsScroll.post(new Runnable() {
            @Override
            public void run() {
                downloadsScroll.scrollTo(0, savedScroll);
            }
        });
    }

    private View buildDownloadRow(final DownloadInfo info) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundResource(R.drawable.bg_engine_cell);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(5);
        row.setLayoutParams(rowParams);

        TextView name = new TextView(this);
        name.setText(info.title == null || info.title.isEmpty() ? "download" : info.title);
        name.setTextColor(colorOnSurface);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        row.addView(name);

        TextView status = new TextView(this);
        status.setTextColor(colorOnSurfaceVariant);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(3);
        status.setLayoutParams(statusParams);
        String statusText;
        if (info.status == DownloadManager.STATUS_SUCCESSFUL) statusText = getString(R.string.download_success);
        else if (info.status == DownloadManager.STATUS_FAILED) statusText = getString(R.string.download_failed_short);
        else if (info.status == DownloadManager.STATUS_PAUSED) statusText = getString(R.string.download_paused);
        else if (info.status == DownloadManager.STATUS_PENDING) statusText = getString(R.string.download_pending);
        else statusText = getString(R.string.download_running);
        StringBuilder details = new StringBuilder(statusText);
        if (info.total > 0) {
            if (info.done > 0) details.append(" · ").append(StorageUtils.format(info.done));
            details.append(" / ").append(StorageUtils.format(info.total));
        } else if (info.done > 0) {
            details.append(" · ").append(StorageUtils.format(info.done));
        }
        if (info.status == DownloadManager.STATUS_RUNNING && info.speed > 0) {
            details.append(" · ").append(StorageUtils.format(info.speed)).append("/s");
        }
        status.setText(details.toString());
        row.addView(status);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(4));
        barParams.topMargin = dp(8);
        bar.setLayoutParams(barParams);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(colorOnSurfaceVariant));
        if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
            bar.setProgress(100);
        } else if (info.total > 0) {
            bar.setProgress((int) Math.min(100L, info.done * 100L / info.total));
        } else {
            bar.setProgress(0);
        }
        row.addView(bar);

        if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openDownloadedFile(info);
                }
            });
        }
        return row;
    }

    private void openDownloadedFile(DownloadInfo info) {
        if (downloadManager == null || info == null) return;
        try {
            Uri uri = downloadManager.getUriForDownloadedFile(info.id);
            if (uri == null) {
                Toast.makeText(this, R.string.download_open_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, info.mime == null ? "*/*" : info.mime);
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(view, getString(R.string.download_open)));
        } catch (Throwable t) {
            Toast.makeText(this, R.string.download_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------- network diagnostics

    private boolean maybeShowNetworkDiagnostic(WebView view, WebResourceRequest request, int errorCode) {
        if (view == null || request == null) return false;
        if (!request.isForMainFrame()) return false;
        if (view != currentWebView() || mode != Mode.BROWSER) return false;
        if (errorCode != WebViewClient.ERROR_TIMEOUT
                && errorCode != WebViewClient.ERROR_CONNECT
                && errorCode != WebViewClient.ERROR_HOST_LOOKUP) {
            return false;
        }
        if (!isOnline()) return false;

        String host = request.getUrl() == null ? null : request.getUrl().getHost();
        if (host == null || host.isEmpty()) return false;
        host = host.toLowerCase(Locale.US);
        if (host.startsWith("www.")) host = host.substring(4);
        String baseDomain = overseasBaseDomain(host);
        if (baseDomain == null) return false;
        host = baseDomain;

        long now = System.currentTimeMillis();
        Long last = diagnosticThrottle.get(host);
        // Once the signature matches, consider it handled even inside the cooldown,
        // so repeated refreshes do not fall back to a Toast each time.
        if (last != null && now - last < DIAGNOSTIC_COOLDOWN_MS) return true;
        if (networkDiagnosticDialog != null && networkDiagnosticDialog.isShowing()) return true;
        diagnosticThrottle.put(host, now);
        showNetworkDiagnosticDialog();
        return true;
    }

    private String overseasBaseDomain(String host) {
        if (host == null) return null;
        if (host.startsWith("google.") || host.contains(".google.")) return "google.com";
        for (String domain : OVERSEAS_HOSTS) {
            if (host.equals(domain) || host.endsWith("." + domain)) return domain;
        }
        return null;
    }

    private void showNetworkDiagnosticDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_network_diagnostic);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        networkDiagnosticDialog = dialog;
        dialog.findViewById(R.id.network_diag_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (networkDiagnosticDialog == dialog) networkDiagnosticDialog = null;
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.34f);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min((int) (screenWidth * 0.9f), dp(420)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        View panel = dialog.findViewById(R.id.network_diag_panel);
        if (panel != null) {
            panel.setAlpha(0f);
            panel.setScaleX(0.96f);
            panel.setScaleY(0.96f);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                    .withLayer();
        }
    }

    // ----------------------------------------------------------- appearance

    private void showAppearanceDialog() {
        if (mode == Mode.AI) return;
        openPage(R.string.appearance, "appearance");
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(4), dp(18), dp(24));
        scroll.addView(content);
        pageContent.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        buildAppearanceContent(content);
    }

    private void buildAppearanceContent(LinearLayout content) {
        addAppearanceLabel(content, getString(R.string.theme_color));
        addThemeSwatches(content);

        addAppearanceLabel(content, getString(R.string.home_background));
        addBackgroundRow(content, getString(R.string.background_default),
                "none".equals(currentBackgroundType), new Runnable() {
                    @Override
                    public void run() {
                        currentBackgroundType = "none";
                        currentBackgroundUri = "";
                        SettingsStore.setBackgroundType(MainActivity.this, "none");
                        SettingsStore.setBackgroundUri(MainActivity.this, "");
                        loadHomeBackground();
                        showAppearanceDialog();
                    }
                });
        addBackgroundRow(content, getString(R.string.background_image),
                "image".equals(currentBackgroundType), new Runnable() {
                    @Override
                    public void run() {
                        pickBackground();
                    }
                });

        addAppearanceLabel(content, getString(R.string.font_label));
        addFontRows(content);
    }

    private void addAppearanceLabel(LinearLayout content, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(colorOnSurfaceVariant);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(15);
        params.bottomMargin = dp(7);
        label.setLayoutParams(params);
        content.addView(label);
    }

    private void addThemeSwatches(LinearLayout content) {
        String current = SettingsStore.getThemeId(this);
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < THEME_IDS.length; i++) {
            final String id = THEME_IDS[i];
            boolean selected = id.equals(current);
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            cell.setPadding(dp(2), dp(4), dp(2), dp(4));
            cell.setBackgroundResource(R.drawable.bg_engine_cell);

            View dot = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(30), dp(30));
            dot.setLayoutParams(dotParams);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(night ? THEME_PRIMARY_DARK[i] : THEME_PRIMARY_LIGHT[i]);
            circle.setStroke(dp(2), selected ? colorOnSurface : 0x00000000);
            dot.setBackground(circle);
            cell.addView(dot);

            TextView name = new TextView(this);
            name.setText(THEME_NAMES[i]);
            name.setTextColor(selected ? colorPrimary : colorOnSurfaceVariant);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = dp(4);
            name.setLayoutParams(nameParams);
            cell.addView(name);

            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsStore.setThemeId(MainActivity.this, id);
                    applyTheme();
                    showAppearanceDialog();
                }
            });
            row.addView(cell);
        }
        content.addView(row);
    }

    private void addBackgroundRow(LinearLayout content, String label, boolean selected, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), 0, dp(10), 0);
        row.setBackgroundResource(R.drawable.bg_engine_cell);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        params.bottomMargin = dp(3);
        row.setLayoutParams(params);
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(selected ? colorPrimary : colorOnSurface);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text);
        if (selected) {
            ImageView check = new ImageView(this);
            check.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
            row.addView(check);
        }
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        content.addView(row);
    }

    private void addFontRows(LinearLayout content) {
        String current = SettingsStore.getFontId(this);
        for (int i = 0; i < FONT_IDS.length; i++) {
            final String id = FONT_IDS[i];
            boolean selected = id.equals(current);
            Typeface typeface;
            try {
                typeface = Typeface.create(FONT_FAMILIES[i], Typeface.NORMAL);
            } catch (Throwable ignored) {
                typeface = Typeface.DEFAULT;
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11), 0, dp(10), 0);
            row.setBackgroundResource(R.drawable.bg_engine_cell);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
            params.bottomMargin = dp(3);
            row.setLayoutParams(params);
            TextView text = new TextView(this);
            text.setText(FONT_NAMES[i]);
            text.setTypeface(typeface);
            text.setTextColor(selected ? colorPrimary : colorOnSurface);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(text);
            if (selected) {
                ImageView check = new ImageView(this);
                check.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
                check.setImageResource(R.drawable.ic_check);
                check.setImageTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
                row.addView(check);
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsStore.setFontId(MainActivity.this, id);
                    applyFont();
                    showAppearanceDialog();
                }
            });
            content.addView(row);
        }
    }

    // ---------------------------------------------------------- wallpaper

    private void loadHomeBackground() {
        currentBackgroundType = SettingsStore.getBackgroundType(this);
        currentBackgroundUri = SettingsStore.getBackgroundUri(this);
        if (!"image".equals(currentBackgroundType)) {
            currentBackgroundType = "none";
            currentBackgroundUri = "";
            SettingsStore.setBackgroundType(this, "none");
            SettingsStore.setBackgroundUri(this, "");
        }
        if (homeBackground == null) return;
        if ("image".equals(currentBackgroundType) && !currentBackgroundUri.isEmpty()) {
            homeBackground.setVisibility(View.GONE);
            homeBgImage.setVisibility(View.VISIBLE);
            loadBackgroundBitmap(currentBackgroundUri);
        } else {
            homeBgImage.setImageDrawable(null);
            homeBgImage.setVisibility(View.GONE);
            homeBackground.setVisibility(View.GONE);
        }
        updateHomeBackgroundVisibility();
    }

    /**
     * Decodes the wallpaper off the main thread and downsampled to the view size.
     * A gallery photo decoded at full resolution is easily 40 MB+ as ARGB_8888,
     * which can OOM the app on a low-RAM device.
     */
    private void loadBackgroundBitmap(final String uriString) {
        final int targetWidth = Math.max(getResources().getDisplayMetrics().widthPixels, dp(320));
        final int targetHeight = Math.max(getResources().getDisplayMetrics().heightPixels, dp(480));
        io.execute(new Runnable() {
            @Override
            public void run() {
                final android.graphics.Bitmap bitmap =
                        decodeSampledBitmap(Uri.parse(uriString), targetWidth, targetHeight);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bitmap == null) return;
                        if (destroyed || !uriString.equals(currentBackgroundUri)) {
                            bitmap.recycle();
                            return;
                        }
                        homeBgImage.setImageBitmap(bitmap);
                        updateHomeBackgroundVisibility();
                    }
                });
            }
        });
    }

    private android.graphics.Bitmap decodeSampledBitmap(Uri uri, int reqWidth, int reqHeight) {
        try {
            android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream boundsStream = getContentResolver().openInputStream(uri);
            if (boundsStream == null) return null;
            try {
                android.graphics.BitmapFactory.decodeStream(boundsStream, null, bounds);
            } finally {
                try { boundsStream.close(); } catch (Throwable ignored) { }
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight);
            InputStream stream = getContentResolver().openInputStream(uri);
            if (stream == null) return null;
            android.graphics.Bitmap decoded;
            try {
                decoded = android.graphics.BitmapFactory.decodeStream(stream, null, options);
            } finally {
                try { stream.close(); } catch (Throwable ignored) { }
            }
            return applyExifRotation(decoded, uri);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Camera photos are usually stored sideways with an EXIF orientation flag. BitmapFactory
     * ignores that flag, so a portrait wallpaper would otherwise show up rotated.
     */
    private android.graphics.Bitmap applyExifRotation(android.graphics.Bitmap bitmap, Uri uri) {
        if (bitmap == null || Build.VERSION.SDK_INT < 24) return bitmap;
        int orientation;
        InputStream stream = null;
        try {
            stream = getContentResolver().openInputStream(uri);
            if (stream == null) return bitmap;
            android.media.ExifInterface exif = new android.media.ExifInterface(stream);
            orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable t) {
            return bitmap;
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Throwable ignored) { }
            }
        }
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        switch (orientation) {
            case android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1f, 1f);
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180f);
                break;
            case android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1f, -1f);
                break;
            case android.media.ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90f);
                break;
            case android.media.ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90f);
                matrix.postScale(-1f, 1f);
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90f);
                break;
            default:
                return bitmap;
        }
        try {
            android.graphics.Bitmap rotated = android.graphics.Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (Throwable t) {
            return bitmap;
        }
    }

    private static int sampleSizeFor(int width, int height, int reqWidth, int reqHeight) {
        int sample = 1;
        if (width <= 0 || height <= 0 || reqWidth <= 0 || reqHeight <= 0) return sample;
        // Bound the decoded bitmap by roughly twice the view's pixel count: a
        // portrait screen cropping a landscape photo still needs a cheap bitmap.
        final long budget = (long) reqWidth * (long) reqHeight * 2L;
        while (sample < 32 && (long) (width / sample) * (long) (height / sample) > budget) {
            sample *= 2;
        }
        return sample;
    }

    private void updateHomeBackgroundVisibility() {
        if (homeBackground == null) return;
        boolean hasImage = "image".equals(currentBackgroundType) && !currentBackgroundUri.isEmpty();
        boolean show = mode == Mode.HOME && hasImage;
        homeBackground.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) homeBgImage.setVisibility(View.VISIBLE);
    }

    private void pickBackground() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_BG_IMAGE);
        } catch (Throwable t) {
            Toast.makeText(this, R.string.background_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_BG_IMAGE || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Throwable ignored) {
        }
        currentBackgroundType = "image";
        currentBackgroundUri = uri.toString();
        SettingsStore.setBackgroundType(this, currentBackgroundType);
        SettingsStore.setBackgroundUri(this, currentBackgroundUri);
        loadHomeBackground();
        if (pageVisible && "appearance".equals(pageKind)) showAppearanceDialog();
        Toast.makeText(this, R.string.background_applied, Toast.LENGTH_SHORT).show();
    }

    // --------------------------------------------------------- connectivity

    private void setupConnectivity() {
        try {
            connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            onlineState = isOnline();
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    refreshConnectivityState();
                }

                @Override
                public void onLost(Network network) {
                    refreshConnectivityState();
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    refreshConnectivityState();
                }
            };
            if (Build.VERSION.SDK_INT >= 24) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean isOnline() {
        try {
            if (connectivityManager == null) connectivityManager =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            Network network = connectivityManager.getActiveNetwork();
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void refreshConnectivityState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean nowOnline = isOnline();
                if (nowOnline == onlineState) return;
                onlineState = nowOnline;

                if (!nowOnline) {
                    // Offline always wins: close the proxy/timeout modal first,
                    // then show only the red connectivity banner.
                    dismissNetworkDiagnosticDialog();
                    if (mode == Mode.BROWSER) showOfflineBanner();
                    else hideConnectionBannerNow();
                    return;
                }

                // Online: remove any stale diagnostic card, show the green
                // recovery banner, and reload a page left in an error state.
                boolean modalWasShowing = networkDiagnosticDialog != null
                        && networkDiagnosticDialog.isShowing();
                dismissNetworkDiagnosticDialog();
                if (mode != Mode.BROWSER) {
                    hideConnectionBannerNow();
                    return;
                }
                boolean pageError = currentTab != null
                        && (currentTab.errorPageShowing || currentTab.networkError);
                showRestoredBanner();
                if (pageError || modalWasShowing) reloadAfterNetworkRecovery();
            }
        });
    }

    private void syncConnectivityBanner() {
        if (mode != Mode.BROWSER) {
            dismissNetworkDiagnosticDialog();
            hideConnectionBannerNow();
            return;
        }
        if (!onlineState) {
            dismissNetworkDiagnosticDialog();
            showOfflineBanner();
        } else if (currentTab != null
                && (currentTab.errorPageShowing || currentTab.networkError)) {
            dismissNetworkDiagnosticDialog();
            showRestoredBanner();
            reloadAfterNetworkRecovery();
        } else if (bannerVisible && !bannerOnline) {
            dismissNetworkDiagnosticDialog();
            showRestoredBanner();
        } else if (!bannerOnline) {
            dismissNetworkDiagnosticDialog();
            hideConnectionBannerNow();
        }
    }

    private void dismissNetworkDiagnosticDialog() {
        if (networkDiagnosticDialog != null) {
            networkDiagnosticDialog.dismiss();
            networkDiagnosticDialog = null;
        }
        if (downloadConfirmDialog != null) {
            downloadConfirmDialog.dismiss();
            downloadConfirmDialog = null;
        }
        handler.removeCallbacks(downloadPollRunnable);
        handler.removeCallbacks(downloadsTickRunnable);
        if (downloadBannerColorAnimator != null) downloadBannerColorAnimator.cancel();
    }

    private void reloadAfterNetworkRecovery() {
        BrowserTab tab = currentTab;
        if (tab == null) return;
        String target = tab.failedUrl != null && !tab.failedUrl.isEmpty()
                ? tab.failedUrl
                : tab.url;
        tab.errorPageShowing = false;
        tab.networkError = false;
        tab.failedUrl = null;
        setChromeVisible(true, true);
        if (target != null && !target.isEmpty() && !"about:blank".equals(target)) {
            tab.url = target;
            progress.setVisibility(View.VISIBLE);
            updateChrome();
            tab.webView.loadUrl(target);
        } else {
            progress.setVisibility(View.VISIBLE);
            tab.webView.reload();
        }
    }

    private void showOfflineBanner() {
        dismissNetworkDiagnosticDialog();
        if (mode != Mode.BROWSER) return;
        onlineState = false;
        handler.removeCallbacks(hideGreenBanner);
        connectionBanner.animate().cancel();
        connectionText.setText(R.string.offline_message);
        connectionIcon.setImageResource(R.drawable.ic_wifi_off);
        if (!bannerVisible) {
            bannerVisible = true;
            bannerOnline = false;
            setBannerColor(BANNER_RED);
            connectionBanner.setVisibility(View.VISIBLE);
            connectionBanner.setAlpha(0f);
            connectionBanner.setTranslationY(-dp(12));
            connectionBanner.animate().alpha(1f).translationY(0f)
                    .setDuration(280)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                    .withLayer();
        } else {
            bannerOnline = false;
            animateBannerColor(BANNER_RED);
        }
        connectionIcon.setScaleX(0.9f);
        connectionIcon.setScaleY(0.9f);
        connectionIcon.animate().scaleX(1f).scaleY(1f)
                .setDuration(220).setInterpolator(new OvershootInterpolator(1.6f));
        setChromeVisible(true, true);
        updateDownloadBannerBase(true);
    }

    private void showRestoredBanner() {
        if (mode != Mode.BROWSER || bannerOnline) return;
        setChromeVisible(true, true);
        bannerOnline = true;
        connectionText.setText(R.string.online_message);
        connectionText.setAlpha(0.55f);
        connectionText.animate().alpha(1f).setDuration(240).start();
        connectionIcon.setImageResource(R.drawable.ic_wifi);
        if (!bannerVisible) {
            bannerVisible = true;
            setBannerColor(BANNER_GREEN);
            connectionBanner.animate().cancel();
            connectionBanner.setVisibility(View.VISIBLE);
            connectionBanner.setAlpha(0f);
            connectionBanner.setTranslationY(-dp(12));
            connectionBanner.animate().alpha(1f).translationY(0f)
                    .setDuration(260)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                    .withLayer();
        } else {
            animateBannerColor(BANNER_GREEN);
        }
        connectionIcon.setScaleX(0.75f);
        connectionIcon.setScaleY(0.75f);
        connectionIcon.animate().scaleX(1f).scaleY(1f)
                .setDuration(300).setInterpolator(new OvershootInterpolator(2f));
        handler.removeCallbacks(hideGreenBanner);
        handler.postDelayed(hideGreenBanner, 2000);
        updateDownloadBannerBase(true);
    }

    private final Runnable hideGreenBanner = new Runnable() {
        @Override
        public void run() {
            if (!bannerOnline || !bannerVisible) return;
            bannerOnline = false;
            bannerVisible = false;
            updateDownloadBannerBase(true);
            connectionBanner.animate().cancel();
            connectionBanner.animate().alpha(0f).translationY(-dp(12))
                    .setDuration(200)
                    .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f))
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (!bannerVisible) connectionBanner.setVisibility(View.GONE);
                        }
                    });
        }
    };

    private void hideConnectionBannerNow() {
        handler.removeCallbacks(hideGreenBanner);
        bannerVisible = false;
        bannerOnline = false;
        if (connectionBanner == null) return;
        connectionBanner.animate().cancel();
        connectionBanner.setVisibility(View.GONE);
        updateDownloadBannerBase(true);
    }

    private void setBannerColor(int color) {
        if (bannerColorAnimator != null) bannerColorAnimator.cancel();
        bannerColor = color;
        connectionBanner.setBackground(bannerDrawable(color));
    }

    private void animateBannerColor(final int target) {
        if (bannerColorAnimator != null) bannerColorAnimator.cancel();
        final int from = bannerColor;
        bannerColorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), from, target);
        bannerColorAnimator.setDuration(380);
        bannerColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bannerColor = (Integer) animation.getAnimatedValue();
                connectionBanner.setBackground(bannerDrawable(bannerColor));
            }
        });
        bannerColorAnimator.start();
    }

    private GradientDrawable bannerDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    // ------------------------------------------------------- Firefox-like tools

    private void findCloseSetup() {
        findInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFind(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        findInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    findNext(true);
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.find_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNext(false);
            }
        });
        findViewById(R.id.find_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNext(true);
            }
        });
        findViewById(R.id.find_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFindBar();
            }
        });
    }

    private void showFindBar() {
        if (mode != Mode.BROWSER || currentWebView() == null) {
            Toast.makeText(this, R.string.no_page, Toast.LENGTH_SHORT).show();
            return;
        }
        if (findActive) {
            findInput.requestFocus();
            showKeyboard(findInput);
            return;
        }
        findActive = true;
        setChromeVisible(true, true);
        findBar.setVisibility(View.VISIBLE);
        findBar.animate().cancel();
        findBar.setAlpha(0f);
        findBar.setTranslationY(-dp(8));
        findBar.animate().alpha(1f).translationY(0f)
                .setDuration(230)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                .withLayer();
        findInput.setText("");
        findResult.setText("");
        findInput.requestFocus();
        showKeyboard(findInput);
    }

    private void hideFindBar() {
        if (!findActive) return;
        findActive = false;
        WebView webView = currentWebView();
        if (webView != null) webView.clearMatches();
        findResult.setText("");
        hideKeyboard();
        findInput.clearFocus();
        findBar.animate().cancel();
        findBar.animate().alpha(0f).translationY(-dp(8))
                .setDuration(180)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!findActive) findBar.setVisibility(View.GONE);
                    }
                });
    }

    private void performFind(String query) {
        if (!findActive) return;
        WebView webView = currentWebView();
        if (webView == null) return;
        String text = query == null ? "" : query.trim();
        if (text.isEmpty()) {
            webView.clearMatches();
            findResult.setText("");
        } else {
            webView.findAllAsync(text);
        }
    }

    private void findNext(boolean forward) {
        WebView webView = currentWebView();
        if (webView == null || !findActive) return;
        if (findInput.getText().toString().trim().isEmpty()) return;
        webView.findNext(forward);
    }

    private void updateFindResult(int active, int total) {
        if (!findActive || findResult == null) return;
        if (total <= 0) {
            findResult.setText("0/0");
        } else {
            findResult.setText(Math.min(active + 1, total) + "/" + total);
        }
    }

    private void showPageMenu() {
        hideKeyboard();
        if (menuPopup != null && menuPopup.isShowing()) {
            menuPopup.dismiss();
            return;
        }
        final int width = dp(248);
        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_glass_panel);
        panel.setPadding(dp(6), dp(7), dp(6), dp(7));
        panel.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));

        final boolean isBrowser = mode == Mode.BROWSER;
        final boolean bookmarked = isBrowser && currentTab != null && currentTab.url != null
                && BookmarkStore.contains(this, currentTab.url);

        panel.addView(menuRow(R.drawable.ic_add, getString(R.string.new_tab), false, new Runnable() {
            @Override
            public void run() {
                createTab(true);
            }
        }));
        if (isBrowser) {
            panel.addView(menuRow(R.drawable.ic_bookmark,
                    getString(bookmarked ? R.string.remove_bookmark : R.string.add_bookmark), false, new Runnable() {
                        @Override
                        public void run() {
                            toggleBookmark();
                        }
                    }));
        }
        panel.addView(menuRow(R.drawable.ic_bookmark, getString(R.string.bookmarks), false, new Runnable() {
            @Override
            public void run() {
                showBookmarks();
            }
        }));
        if (isBrowser) {
            panel.addView(menuRow(R.drawable.ic_search, getString(R.string.find_in_page), false, new Runnable() {
                @Override
                public void run() {
                    showFindBar();
                }
            }));
            panel.addView(menuRow(R.drawable.ic_desktop, getString(R.string.desktop_site),
                    currentTab != null && currentTab.desktopMode, new Runnable() {
                        @Override
                        public void run() {
                            toggleDesktopMode();
                        }
                    }));
            panel.addView(menuRow(R.drawable.ic_share, getString(R.string.share), false, new Runnable() {
                @Override
                public void run() {
                    sharePage();
                }
            }));
            panel.addView(menuRow(R.drawable.ic_ai, getString(R.string.ai_summary), false, new Runnable() {
                @Override
                public void run() {
                    summarizeCurrentPage();
                }
            }));
        }
        panel.addView(menuRow(R.drawable.ic_download, getString(R.string.downloads), false, new Runnable() {
            @Override
            public void run() {
                showDownloadsDialog();
            }
        }));
        panel.addView(menuRow(R.drawable.ic_palette, getString(R.string.appearance), false, new Runnable() {
            @Override
            public void run() {
                showAppearanceDialog();
            }
        }));
        panel.addView(menuRow(R.drawable.ic_settings, getString(R.string.settings), false, new Runnable() {
            @Override
            public void run() {
                showSettings(false);
            }
        }));

        PopupWindow popup = new PopupWindow(panel, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        if (Build.VERSION.SDK_INT >= 21) popup.setElevation(dp(12));
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                menuPopup = null;
                if (menuIcon != null) {
                    menuIcon.setImageTintList(android.content.res.ColorStateList.valueOf(colorOnSurfaceVariant));
                }
            }
        });
        menuPopup = popup;
        panel.setAlpha(0f);
        panel.setScaleX(0.94f);
        panel.setScaleY(0.94f);
        panel.setTranslationY(dp(6));
        panel.setPivotX(width);
        panel.setPivotY(0f);
        // Keep a clear gap above the 54dp bottom nav so the menu button stays tappable.
        popup.showAtLocation(navMenu, Gravity.BOTTOM | Gravity.END, dp(10), dp(90));
        if (menuIcon != null) {
            menuIcon.setImageTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
        }
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(210)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                .withLayer();
    }

    private View menuRow(int iconRes, String label, boolean checked, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(46));
        row.setPadding(dp(11), 0, dp(9), 0);
        row.setBackgroundResource(R.drawable.bg_engine_cell);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        rowParams.setMargins(dp(2), dp(1), dp(2), dp(1));
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                checked ? colorPrimary : colorOnSurfaceVariant));
        row.addView(icon);

        TextView text = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(12);
        text.setLayoutParams(textParams);
        text.setText(label);
        text.setTextColor(checked ? colorPrimary : colorOnSurface);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        text.setSingleLine(true);
        row.addView(text);

        if (checked) {
            ImageView check = new ImageView(this);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(18), dp(18));
            check.setLayoutParams(checkParams);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
            row.addView(check);
        }

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuPopup != null) menuPopup.dismiss();
                if (aiQuickPopup != null) aiQuickPopup.dismiss();
                action.run();
            }
        });
        return row;
    }

    private void toggleBookmark() {
        if (currentTab == null || currentTab.url == null || currentTab.url.isEmpty()) {
            Toast.makeText(this, R.string.no_page, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean added = BookmarkStore.toggle(this, currentTab.title, currentTab.url);
        Toast.makeText(this, added ? R.string.bookmark_added : R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
    }

    private void showBookmarks() {
        openPage(R.string.bookmarks, "bookmarks");
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(18), dp(6), dp(18), dp(24));
        scroll.addView(list);
        pageContent.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        final List<BookmarkStore.Item> items = BookmarkStore.list(this);
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.bookmarks_empty);
            empty.setTextColor(colorOnSurfaceVariant);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(48), dp(12), dp(48));
            list.addView(empty);
            return;
        }

        for (final BookmarkStore.Item item : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11), dp(8), dp(5), dp(8));
            row.setBackgroundResource(R.drawable.bg_engine_cell);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(6);
            row.setLayoutParams(rowParams);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView name = new TextView(this);
            name.setText(item.title);
            name.setTextColor(colorOnSurface);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textCol.addView(name);
            TextView url = new TextView(this);
            url.setText(item.url);
            url.setTextColor(colorOnSurfaceVariant);
            url.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            url.setSingleLine(true);
            url.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            url.setPadding(0, dp(2), 0, 0);
            textCol.addView(url);
            row.addView(textCol);

            ImageView delete = new ImageView(this);
            delete.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
            delete.setPadding(dp(9), dp(9), dp(9), dp(9));
            delete.setImageResource(R.drawable.ic_trash);
            delete.setImageTintList(android.content.res.ColorStateList.valueOf(colorOnSurfaceVariant));
            delete.setBackgroundResource(R.drawable.bg_icon_ripple);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BookmarkStore.remove(MainActivity.this, item.url);
                    showBookmarks();
                }
            });
            row.addView(delete);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closePage();
                    loadUrlInCurrent(item.url);
                }
            });
            list.addView(row);
        }
    }

    private void toggleDesktopMode() {
        if (currentTab == null) return;
        currentTab.desktopMode = !currentTab.desktopMode;
        WebSettings settings = currentTab.webView.getSettings();
        if (currentTab.desktopMode) {
            settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
        } else {
            if (currentTab.mobileUserAgent != null) settings.setUserAgentString(currentTab.mobileUserAgent);
            settings.setUseWideViewPort(false);
            settings.setLoadWithOverviewMode(false);
        }
        currentTab.webView.reload();
    }

    private void sharePage() {
        if (currentTab == null || currentTab.url == null || currentTab.url.isEmpty()) {
            Toast.makeText(this, R.string.no_page, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        String title = currentTab.title == null ? "" : currentTab.title;
        send.putExtra(Intent.EXTRA_TEXT, title + "\n" + currentTab.url);
        startActivity(Intent.createChooser(send, getString(R.string.share)));
    }

    // ------------------------------------------------------------- settings

    private void showAiEndpointDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ai_endpoint);
        final TextView deepseek = dialog.findViewById(R.id.ai_provider_deepseek);
        final TextView custom = dialog.findViewById(R.id.ai_provider_custom);
        final EditText baseUrl = dialog.findViewById(R.id.ai_base_url);
        final EditText model = dialog.findViewById(R.id.ai_model);
        baseUrl.setText(SettingsStore.getAiBaseUrl(this));
        model.setText(SettingsStore.getAiModel(this));
        final boolean[] customMode = {SettingsStore.isCustomAi(this)};
        applyAiProviderUi(customMode[0], deepseek, custom, dialog);
        deepseek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customMode[0] = false;
                applyAiProviderUi(false, deepseek, custom, dialog);
            }
        });
        custom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customMode[0] = true;
                applyAiProviderUi(true, deepseek, custom, dialog);
            }
        });
        dialog.findViewById(R.id.ai_endpoint_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.ai_endpoint_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String base = baseUrl.getText().toString().trim();
                String modelName = model.getText().toString().trim();
                if (customMode[0]) {
                    if (!base.startsWith("http://") && !base.startsWith("https://")) {
                        Toast.makeText(MainActivity.this, R.string.ai_endpoint_invalid,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (modelName.isEmpty()) {
                        Toast.makeText(MainActivity.this, R.string.ai_model_missing,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SettingsStore.setAiBaseUrl(MainActivity.this, base);
                    SettingsStore.setAiModel(MainActivity.this, modelName);
                }
                SettingsStore.setCustomAi(MainActivity.this, customMode[0]);
                if (aiStreaming) cancelAiStream();
                applyModelLabel();
                dialog.dismiss();
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.34f);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min((int) (screenWidth * 0.92f), dp(430)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }

    private void applyAiProviderUi(boolean customMode, TextView deepseek, TextView custom,
                                   Dialog dialog) {
        deepseek.setBackgroundResource(customMode ? R.drawable.bg_button_tonal : R.drawable.bg_button_primary);
        deepseek.setTextColor(customMode ? colorPrimary : colorOnPrimary);
        custom.setBackgroundResource(customMode ? R.drawable.bg_button_primary : R.drawable.bg_button_tonal);
        custom.setTextColor(customMode ? colorOnPrimary : colorPrimary);
        int visibility = customMode ? View.VISIBLE : View.GONE;
        View baseLabel = dialog.findViewById(R.id.ai_base_label);
        View baseField = dialog.findViewById(R.id.ai_base_url);
        View modelLabel = dialog.findViewById(R.id.ai_model_label);
        View modelField = dialog.findViewById(R.id.ai_model);
        if (baseLabel != null) baseLabel.setVisibility(visibility);
        if (baseField != null) baseField.setVisibility(visibility);
        if (modelLabel != null) modelLabel.setVisibility(visibility);
        if (modelField != null) modelField.setVisibility(visibility);
    }

    private void showLanguageDialog(final Dialog settings) {
        final String[] codes = LocaleHelper.codes();
        String current = LocaleHelper.getLanguage(this);
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) {
                checked = i;
                break;
            }
        }
        String[] labels = new String[codes.length];
        for (int i = 0; i < codes.length; i++) labels[i] = LocaleHelper.label(this, codes[i]);
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.language)
                .setMessage(R.string.language_note)
                .setSingleChoiceItems(labels, checked,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                dialog.dismiss();
                                LocaleHelper.setLanguage(MainActivity.this, codes[which]);
                                if (settings != null) settings.dismiss();
                                recreate();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupPageOverlay() {
        if (pageOverlay == null) return;
        pageOverlay.setVisibility(View.GONE);
        pageOverlay.findViewById(R.id.page_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePage();
            }
        });
    }

    private void openPage(int titleRes, String kind) {
        if (pageOverlay == null) return;
        boolean same = pageVisible && kind != null && kind.equals(pageKind);
        hideKeyboard();
        pageKind = kind;
        pageVisible = true;
        pageTitleView.setText(titleRes);
        pageContent.removeAllViews();
        if (same) {
            pageOverlay.setVisibility(View.VISIBLE);
            pageOverlay.setAlpha(1f);
            pageOverlay.setTranslationX(0f);
            return;
        }
        enterPage(pageOverlay);
    }

    private void closePage() {
        if (!pageVisible) return;
        pageVisible = false;
        pageKind = null;
        handler.removeCallbacks(downloadsTickRunnable);
        exitPage(pageOverlay, new Runnable() {
            @Override
            public void run() {
                if (!pageVisible) {
                    pageOverlay.setVisibility(View.GONE);
                    pageOverlay.setTranslationX(0f);
                }
            }
        });
    }

    /** Slides a full-screen page in from the right while fading it in. */
    private void enterPage(final View page) {
        page.animate().cancel();
        page.setVisibility(View.VISIBLE);
        page.setAlpha(0f);
        page.setTranslationX(dp(26));
        page.animate().alpha(1f).translationX(0f)
                .setDuration(220)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.2f, 1f))
                .withLayer()
                .start();
    }

    /** Fades a page out to the right; {@code onHidden} runs once the animation ends. */
    private void exitPage(final View page, final Runnable onHidden) {
        page.animate().cancel();
        page.animate().alpha(0f).translationX(dp(26))
                .setDuration(170)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 1f, 1f))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (onHidden != null) onHidden.run();
                    }
                })
                .start();
    }

    private void setupSettingsPage() {
        if (settingsPage == null) return;
        settingsPage.setVisibility(View.GONE);
        settingsKeyInput = settingsPage.findViewById(R.id.settings_key);
        settingsSizeApp = settingsPage.findViewById(R.id.settings_size_app);
        settingsSizeCache = settingsPage.findViewById(R.id.settings_size_cache);
        settingsPage.findViewById(R.id.settings_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSettingsPage();
            }
        });
        settingsPage.findViewById(R.id.settings_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsStore.setApiKey(MainActivity.this, settingsKeyInput.getText().toString());
                Toast.makeText(MainActivity.this, R.string.key_saved, Toast.LENGTH_SHORT).show();
            }
        });
        settingsPage.findViewById(R.id.settings_ai_endpoint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAiEndpointDialog();
            }
        });
        settingsPage.findViewById(R.id.settings_widgets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (homeWidgets != null) homeWidgets.showManager();
            }
        });
        settingsPage.findViewById(R.id.settings_language).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDialog(null);
            }
        });
        settingsPage.findViewById(R.id.settings_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllWebViewData();
                Toast.makeText(MainActivity.this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
                updateStorageText();
            }
        });
        settingsStorageUpdater = new Runnable() {
            @Override
            public void run() {
                if (!settingsPageVisible) return;
                updateStorageText();
                handler.postDelayed(this, 2500);
            }
        };
    }

    private void showSettings(boolean focusKey) {
        if (settingsPage == null) return;
        if (!settingsPageVisible) {
            settingsPageVisible = true;
            enterPage(settingsPage);
        }
        settingsKeyInput.setText(SettingsStore.getApiKey(this));
        settingsKeyInput.setSelection(settingsKeyInput.getText().length());
        Button languageButton = settingsPage.findViewById(R.id.settings_language);
        languageButton.setText(getString(R.string.language) + "："
                + LocaleHelper.label(this, LocaleHelper.getLanguage(this)));
        TextView about = settingsPage.findViewById(R.id.settings_about);
        about.setText(R.string.settings_about);
        updateStorageText();
        handler.removeCallbacks(settingsStorageUpdater);
        handler.post(settingsStorageUpdater);
        if (focusKey) {
            settingsKeyInput.requestFocus();
            showKeyboard(settingsKeyInput);
        } else {
            settingsKeyInput.clearFocus();
            hideKeyboard();
        }
    }

    private void hideSettingsPage() {
        if (!settingsPageVisible) return;
        settingsPageVisible = false;
        handler.removeCallbacks(settingsStorageUpdater);
        hideKeyboard();
        settingsKeyInput.clearFocus();
        exitPage(settingsPage, new Runnable() {
            @Override
            public void run() {
                if (!settingsPageVisible) {
                    settingsPage.setVisibility(View.GONE);
                    settingsPage.setTranslationX(0f);
                }
            }
        });
    }

    private void updateStorageText() {
        if (!settingsPageVisible || settingsSizeApp == null || settingsSizeCache == null) return;
        io.execute(new Runnable() {
            @Override
            public void run() {
                final long total = StorageUtils.appSize(MainActivity.this);
                final long cache = StorageUtils.webViewSize(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!settingsPageVisible) return;
                        settingsSizeApp.setText(StorageUtils.format(total));
                        settingsSizeCache.setText(StorageUtils.format(cache) + " / 100 MB");
                    }
                });
            }
        });
    }

    private void clearAllWebViewData() {
        StorageUtils.clearAll(tabs, aiWebView);
        lastTrimAt = System.currentTimeMillis();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            trimCacheIfNeeded();
        }
    }

    // WebView has no public hard quota for its Chromium cache. We enforce the 100 MB
    // budget by measuring app_webview and clearing cache/history once it grows past it.
    private void trimCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (trimRunning || now - lastTrimAt < CACHE_CHECK_INTERVAL_MS) return;
        trimRunning = true;
        lastTrimAt = now;
        io.execute(new Runnable() {
            @Override
            public void run() {
                final long cache = StorageUtils.webViewSize(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        trimRunning = false;
                        if (!destroyed && cache > CACHE_LIMIT_BYTES) {
                            StorageUtils.clearCachesOnly(tabs, aiWebView);
                        }
                    }
                });
            }
        });
    }

    // ----------------------------------------------------------------- life

    @Override
    protected void onResume() {
        super.onResume();
        if (mode == Mode.BROWSER && currentTab != null) currentTab.webView.onResume();
        if (mode == Mode.HOME) updateHomeBackgroundVisibility();
        if (aiWebView != null) aiWebView.onResume();
        if (mode == Mode.BROWSER) syncDownloadBanner();
        updateChrome();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(downloadPollRunnable);
        downloadPolling = false;
        for (BrowserTab tab : tabs) {
            try { tab.webView.onPause(); } catch (Throwable ignored) { }
        }
        if (aiWebView != null) {
            try { aiWebView.onPause(); } catch (Throwable ignored) { }
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (aiStreaming) cancelAiStream();
        if (isFinishing()) {
            clearAllWebViewData();
        } else {
            trimCacheIfNeeded();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (homeWidgets != null) homeWidgets.destroy();
        aiClient.cancel();
        clearAllWebViewData();
        if (tabsDialog != null) {
            tabsDialog.dismiss();
            tabsDialog = null;
        }
        handler.removeCallbacks(settingsStorageUpdater);
        handler.removeCallbacks(downloadsTickRunnable);
        if (modelDialog != null) {
            modelDialog.dismiss();
            modelDialog = null;
        }
        if (enginePopup != null) {
            enginePopup.dismiss();
            enginePopup = null;
        }
        if (menuPopup != null) {
            menuPopup.dismiss();
            menuPopup = null;
        }
        if (aiQuickPopup != null) {
            aiQuickPopup.dismiss();
            aiQuickPopup = null;
        }
        if (networkDiagnosticDialog != null) {
            networkDiagnosticDialog.dismiss();
            networkDiagnosticDialog = null;
        }
        if (downloadConfirmDialog != null) {
            downloadConfirmDialog.dismiss();
            downloadConfirmDialog = null;
        }
        handler.removeCallbacks(downloadBannerAutoHide);
        if (downloadBannerColorAnimator != null) {
            downloadBannerColorAnimator.cancel();
            downloadBannerColorAnimator = null;
        }
        if (networkCallback != null) {
            try { connectivityManager.unregisterNetworkCallback(networkCallback); } catch (Throwable ignored) { }
            networkCallback = null;
        }
        if (bannerColorAnimator != null) bannerColorAnimator.cancel();
        for (BrowserTab tab : new ArrayList<>(tabs)) {
            try {
                if (tab.webView.getParent() != null) {
                    ((ViewGroup) tab.webView.getParent()).removeView(tab.webView);
                }
                tab.webView.destroy();
            } catch (Throwable ignored) {
            }
        }
        tabs.clear();
        currentTab = null;
        if (aiWebView != null) {
            try { aiWebView.removeJavascriptInterface("AiNative"); } catch (Throwable ignored) { }
            try { aiWebView.destroy(); } catch (Throwable ignored) { }
        }
        handler.removeCallbacksAndMessages(null);
        io.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (enginePopup != null && enginePopup.isShowing()) {
            enginePopup.dismiss();
            return;
        }
        if (aiQuickPopup != null && aiQuickPopup.isShowing()) {
            aiQuickPopup.dismiss();
            return;
        }
        if (menuPopup != null && menuPopup.isShowing()) {
            menuPopup.dismiss();
            return;
        }
        if (settingsPageVisible) {
            hideSettingsPage();
            return;
        }
        if (pageVisible) {
            closePage();
            return;
        }
        if (findActive) {
            hideFindBar();
            return;
        }
        if (modelDialog != null && modelDialog.isShowing()) {
            closeModelSheet();
            return;
        }
        if (tabsDialog != null && tabsDialog.isShowing()) {
            tabsDialog.dismiss();
            return;
        }
        if (mode == Mode.AI) {
            closeAiPanel();
            return;
        }
        if (mode == Mode.BROWSER) {
            if (currentTab != null && currentTab.webView.canGoBack()) {
                currentTab.webView.goBack();
                updateChrome();
            } else {
                showHome();
            }
            return;
        }
        finish();
    }

    private void goBackFromNav() {
        if (mode == Mode.AI) {
            closeAiPanel();
            return;
        }
        if (currentTab != null && currentTab.webView.canGoBack()) {
            currentTab.errorPageShowing = false;
            currentTab.webView.goBack();
            showBrowser();
            updateChrome();
            return;
        }
        if (mode == Mode.BROWSER) {
            showHome();
            return;
        }
        if (currentTab != null && currentTab.url != null && !currentTab.url.isEmpty()) {
            showBrowser();
        }
    }

    // -------------------------------------------------------------- chrome

    private void applySystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(colorBg);
        window.setNavigationBarColor(colorBg);
        int flags = window.getDecorView().getSystemUiVisibility();
        boolean night = StorageUtils.isNight(this);
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        if (!night) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void showKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    private int getColorCompat(int id) {
        return getResources().getColor(id);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
