# Changelog

All notable changes are documented here. Version format follows `1.x.y`.

## 3.3.3 — 2026-11-28

- Search AI transition is now the 2.0 crossfade (360ms) between the white glass pill
  and the light AI background, instead of the left-to-right fill reveal.
- The star stays bare with no background, ripple or focus ring while AI mode is on.

## 3.3.2 — 2026-11-28

- Search bar restored to the 2.0 glass pill: 56dp tall, 31dp radius, translucent
  gradient and a 6dp shadow; the original 22dp sparkle sits in a 46dp touch target.
- The star still has no background, ripple or focus ring when tapped, and the AI
  reveal fill stays inside the pill.

## 3.3.1 — 2026-11-28

- Download banner now fades out 2 seconds after it appears (was 4s); banner and
  download card corners are 0dp.
- Home search reverted to the second design: square, light fill reveal from the right,
  no gradient border or outward outline; the star has no focus ring or circular
  background.
- Floating AI button restored to the filled circular style. Its quick menu now sits
  above the button, so tapping the button again closes the menu.

## 3.3.0 — 2026-11-27

- Home search AI transition redesigned: instead of a solid blue fill, a light gradient
  wash plus a 2dp Google-style gradient border sweeps in from the right. Text stays
  dark and readable, the star tints to the theme color, and the engine badge/divider
  hide while AI mode is on.
- The error page is square now: 0 radius card, icon and retry button, flat neutral
  palette instead of the old purple rounded card.

## 3.2.0 — 2026-11-27

- Frosted-glass pass: top/bottom bars, menus and dialogs use translucent gradient
  surfaces with hairline borders instead of opaque white.
- Language can now be switched straight from the bottom menu (Settings → 语言 still
  works). Picking a language also sets the process locale for dates/numbers, and
  "follow system" restores the captured system default.

## 3.1.1 — 2026-11-27

- Restored the original sparkle icon for both AI buttons while keeping the no-background
  treatment and the reveal animation.
- The search-box reveal fill is now a lighter tint (theme color blended 42% toward
  white) so white text stays readable but the block feels softer.

## 3.1.0 — 2026-11-27

- The AI star in the search box is now a bare line icon: no background, no border,
  20dp, visually matched to the search engine badge.
- Enabling AI mode now reveals a theme-colored fill from the right edge of the search
  box with a ClipDrawable + ValueAnimator; the placeholder, text, star and divider
  cross-fade to white. Disabling retracts it to the right. Focusing the field keeps the
  state in sync.
- The floating AI button is a bare star as well.
- Every remaining container corner radius is 0dp.
- The quote widget now asks Hitokoto for a fresh quote in the background when shown
  (30s throttle) and on every tap, instead of cycling a stale cache.

## 3.0.0 — 2026-11-27

- New default "Minimal" theme: white background, Google-blue accent, no gradient or
  glow. The purple Nightly look is replaced; the other five themes still work.
- Flat UI pass: 8dp corners instead of 22-31dp, 1dp hairline borders instead of glass
  gradients and elevation, flat top/bottom bars, flatter cards and dialogs.
- Typography tweaks: letter spacing reset, slightly smaller search text, reduced
  shadows on the AI button; dialog and page spacing kept consistent.

## 2.0.2 — 2026-11-25

- Weather city validation no longer blocks the editor: the city is saved immediately
  and checked in the background, with failures shown on the card.
- Three-letter inputs are treated as airport codes first (airport-data.com); the card
  shows the airport name when one is found.
- Added a keyless fallback weather source (MET Norway) for when wttr.in fails, a 7-day
  place cache and shorter network timeouts.
- Added a UX issue template.

## 2.0.1 — 2026-11-23

- The bottom menu popup now leaves a clear gap above the navigation bar, so the menu
  button stays tappable and can close the menu again. The icon is tinted while the
  menu is open.

## 2.0.0 — 2026-11-23

- The three-dot menu moved from the top bar and the home header into the bottom
  navigation as a fifth item. It opens above the nav and also works in AI mode.
- Minimal chrome pass: 48dp top bar, 54dp bottom nav without divider or elevation,
  22dp navigation icons, a smaller wordmark/tagline/search box and flatter widget
  cards. The floating home menu button is gone.

## 1.12.2 — 2026-11-23

- Settings and the appearance/downloads/bookmarks pages now slide in from the right
  while fading in, and fade out to the right when closed.

## 1.12.1 — 2026-11-23

- Appearance, Downloads and Bookmarks are now full-screen pages like Settings, each
  with a back arrow and support for the system back key. Downloads keep refreshing
  only while the page is open.

## 1.12.0 — 2026-11-23

- Settings is now a full-screen page with its own back button and a scrollable layout
  instead of a modal dialog. Storage figures keep refreshing while the page is open
  and stop when it closes.

## 1.11.0 — 2026-11-22

- The UI is now localized into English, Japanese and Traditional Chinese. Settings →
  语言 switches between following the system and a manual override; switching reloads
  the UI (open tabs are closed, since tabs are not persisted yet).
- Remaining hard-coded UI text was moved into resources: home widgets, weather
  descriptions, AI panel strings, error page, tab titles and AI error messages.

## 1.10.0 — 2026-11-21

- Custom search engines: the engine picker now has「添加自定义搜索引擎」where a name
  and a URL template with `%s` can be entered. Long-press a custom engine to delete it.
- AI endpoint settings: Settings → AI 接口设置 switches between DeepSeek official and
  any OpenAI-compatible base URL + model. The same API key field is used for both.
- One-tap page summary: tapping the AI floating button in the browser opens a small
  menu with「问 AI」and「AI 总结本页」; the same action is in the page menu. The visible
  page text is extracted, capped at 6,000 characters, and sent as hidden context so
  the chat bubble stays short.

## 1.9.3 — 2026-11-21

- Weather city input is now checked against the Open-Meteo geocoder before it is
  saved; a typo shows "找不到这个城市" instead of silently showing weather for an
  unrelated place. Existing invalid cities open the editor when tapped.
- Weather is fetched by the coordinates resolved from that city, so wttr.in can no
  longer fall back to its own guess for an unknown name.
- build.sh now passes explicit `--v1/--v2/--v3` signing flags to apksigner. All three
  schemes were already present, but the intent is now visible in the script.

## 1.9.2 — 2026-11-18

- The quote widget now pulls from Hitokoto (一言), a large Chinese quote collection,
  filtered to literature, poetry and philosophy. The most recent 24 quotes are cached
  locally, and the built-in set remains as an offline fallback.
- Quote cards show the work/author attribution returned by the service.

## 1.9.1 — 2026-11-18

- The download-complete / download-failed banner now fades out after four seconds,
  hides as soon as the page is scrolled, and hides when its details are opened. A
  finished download no longer re-shows the banner on every return to the browser.
- Widget manager rows refresh their summary immediately after an edit.

## 1.9.0 — 2026-11-18

- Home widgets: weather, address shortcut, note and quote cards under the search box.
  Enable and edit them in Settings → 首页组件; the quote card is on by default.
- The weather card uses the keyless wttr.in JSON endpoint, caches for 30 minutes, and
  falls back to a tap-to-retry card when the network is unavailable.
- Home layout: the wordmark now sits top-left with 「从这里起航。」 at the top-right.
- The search placeholder names the current engine ("用 Google 搜索或者键入URL") and
  becomes 「深度求索……」 while AI mode is on.
- Fixed the remaining SSE cancellation race: each request owns its connection and only
  the current request may deliver callbacks, so a fast second question no longer
  receives the previous request's error or loses its user turn.
- Wallpaper decoding now honours the EXIF orientation, so portrait photos no longer
  appear sideways.
- A stopped AI request no longer leaves two user turns in a row in the history.
- The AI panel no longer follows `file:` links from model output.

## 1.8.1 — 2026-11-17

- Home wallpaper is now decoded on a background thread and downsampled to the
  view size; picking a large gallery photo no longer risks an OOM or a UI stall.
- AI history trimming now always leaves a conversation that starts with a user
  turn, so a long chat can no longer fail with a 400 from the API.
- Link and image URLs in model output are scheme-checked before rendering, so
  `javascript:` links cannot run in the AI panel.
- Fixed a cancellation race in the SSE client where a finished request could
  clear the connection of the next one (stop/retry left streams running).
- Error-response bodies are now truncated at exactly the byte budget.
- Download progress polling stops while the app is paused and resumes when the
  browser comes back, instead of waking up every 700 ms in the background.
- The local error page is loaded with a neutral file origin instead of the
  failed site's URL; `ErrorPage.retry` only answers while that page is shown.
- Dialog cleanup on destroy now also closes the download dialogs.
- Storage figures in settings refresh every 2.5 s instead of every second.

## 1.8.0 — 2026-11-17

- Added web downloads through Android DownloadManager.
- Download confirmation dialog with site name, continue/cancel.
- Blue start banner, green completion banner; tap opens the download details.
- Download details page lists filename, progress, speed, status and opens finished files.
- Added the Downloads entry to both home and browser menus.
## 1.7.1 — 2026-11-16

- Fixed Markdown rendering in the AI panel: marked is now embedded directly in
  `ai.html` instead of being loaded as a sibling file URL (which some WebView
  configurations block).
- Added a local fallback Markdown renderer for bold, headings, lists, quotes,
  code and links if marked fails to execute.
- Blocked raw HTML from model output before rendering.
- Made the sticky AI header/bubbles more opaque so scrolled messages no longer
  show through the header.
## 1.7.0 — 2026-11-16

- Renamed the application to **深探** and corrected the package to
  `com.deliciousdiamond.deepexplorer` (new app identity).
- New launcher icon: black background, white triangle, small blue dot at the
  lower-left.
- Homepage now shows 深探 with the tagline 开始一切。.
- Browser page viewport now ends above the bottom navigation, so fixed page
  buttons are no longer blocked by the app's bottom bar. The page sits under
  the top bar and only the top bar hides on scroll; the page no longer shifts.
- Top bar uses a nearly opaque glass surface so page content does not bleed
  through while the page scrolls underneath.

## 1.6.4 — 2026-11-16

- 断网时强制关闭网络诊断/代理超时弹窗，红色断网横幅优先展示。
- 恢复网络时关闭残留弹窗，展示绿色恢复横幅，并自动重载处于错误状态的当前页面。
- 所有主框架 WebView 加载错误统一替换为本地 `assets/error.html`，不再暴露系统默认错误页。
- 自定义错误页的「重试」在仍断网时直接拦截，只刷新红色断网横幅。
- 页面错误状态按标签页跟踪（自定义错误页 / 原生加载失败），恢复后只重载当前页。

## 1.6.3 — 2026-11-16

- Unified the AI panel with the current theme: glass header, glass model/close
  chips and assistant bubbles, theme-colored user bubbles.
- AI panel now follows the selected UI font and light/dark theme.
- Website text attempts to follow the selected browser font via WebView font
  families plus scoped CSS injection; code blocks and common icon fonts are
  preserved.

## 1.6.2 — 2026-11-16

- Proxy/DNS/timeout diagnostics now replace the default WebView error page with
  a local `assets/error.html`.
- Added `ErrorPage` JS bridge and a retry button; the address bar keeps the
  original URL.
- Cooldown still suppresses the dialog, but the local error page is still used.

## 1.6.1 — 2026-11-16

- Added the network environment diagnostic module.
- Detects main-frame `ERROR_TIMEOUT`, `ERROR_CONNECT` and `ERROR_HOST_LOOKUP`
  while the network is otherwise online and the host is a common overseas
  domain.
- Added a red-cross MD3-style card with a 5-minute per-base-domain cooldown.
- Offline state is handled by the connection banner instead.

## 1.6.0 — 2026-11-16

- Removed the video wallpaper feature entirely; only gradient or image
  backgrounds remain.
- Redesigned the settings dialog with a sticky header/close button and split
  app-data/cache rows.
- Reduced the homepage wordmark size and softened the bottom navigation glass.
- Settings footer now shows only the version number.

## 1.5.0 — 2026-11-16

- Added theme colors, image wallpaper and built-in font choices.
- Added the connectivity banner: red offline, green online state for 2 seconds.
- Added theme-aware glass surfaces.

## 1.4.x — 2026-11-14

- Added Firefox-style menu, find-in-page, desktop site, bookmarks and share.
- Added domestic search engines (Baidu, Sogou, 360, Shenma).
- Moved the app package to `com.diliciousdiamond.deepexplorer` (later corrected in 1.7.0).
- Introduced the line-art compass icon and Nightly-inspired palette.

## 1.3.0 — 2026-11-14

- Added glass styling, engine picker, simplified homepage and AI gradient.
- Package renamed to include the GitHub ID.

## 1.2.0 — 2026-11-14

- Added the in-page AI floating entry and close-to-page behavior.
- Added model switching and the local error/diagnostic foundations.

## 1.1.0 — 2026-11-11

- Added address-bar auto-hide, spring AI bubbles, model bottom sheet, page fades
  and bottom-nav bounce.

## 1.0.0 — 2026-11-11

- Initial WebView browser with DeepSeek chat, tabs, cache management and
  Material 3 styling.
