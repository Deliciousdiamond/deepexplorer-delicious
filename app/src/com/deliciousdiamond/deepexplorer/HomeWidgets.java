package com.deliciousdiamond.deepexplorer;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds the optional cards under the home search box and the small manager dialog
 * behind Settings → 首页组件.
 *
 * <p>Everything is plain framework View code: the app ships no AndroidX and no image
 * or networking library, so the weather card talks to the keyless wttr.in JSON endpoint
 * and every other card is local.</p>
 */
final class HomeWidgets {
    private static final long WEATHER_TTL_MS = 30L * 60L * 1000L;
    private static final long WEATHER_RETRY_MS = 60L * 1000L;

    private static final String WEATHER_FILE = "deepexplorer_weather";

    private final MainActivity activity;
    private final LinearLayout container;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final QuoteRepository quotes;

    private WidgetStore.Item[] items;
    private Typeface typeface = Typeface.DEFAULT;
    private int accentColor;

    private boolean destroyed;
    private boolean weatherLoading;
    private static final int WEATHER_ERROR_NONE = 0;
    private static final int WEATHER_ERROR_NETWORK = 1;
    private static final int WEATHER_ERROR_CITY = 2;

    private String weatherErrorCity;
    private long weatherErrorAt;
    private int weatherErrorCode = WEATHER_ERROR_NONE;

    private Dialog managerDialog;
    private Dialog editDialog;

    HomeWidgets(MainActivity activity, LinearLayout container) {
        this.activity = activity;
        this.container = container;
        this.items = WidgetStore.load(activity);
        this.accentColor = activity.getResources().getColor(R.color.primary);
        this.quotes = new QuoteRepository(activity, io, main);
        WidgetStore.Item quote = WidgetStore.find(items, WidgetStore.QUOTE);
        if (quote != null && quote.enabled) quotes.prefetch();
    }

    void setAccent(int color) {
        accentColor = color;
    }

    void setTypeface(Typeface face) {
        typeface = face == null ? Typeface.DEFAULT : face;
        applyTypeface(container);
    }

    /** Rebuilds every enabled card. Cheap enough to call when the home screen appears. */
    void refresh() {
        if (destroyed || container == null) return;
        container.removeAllViews();
        for (WidgetStore.Item item : items) {
            if (item == null || !item.enabled) continue;
            View card = buildCard(item);
            if (card != null) container.addView(card);
        }
    }

    void destroy() {
        destroyed = true;
        io.shutdownNow();
        main.removeCallbacksAndMessages(null);
        if (editDialog != null) {
            editDialog.dismiss();
            editDialog = null;
        }
        if (managerDialog != null) {
            managerDialog.dismiss();
            managerDialog = null;
        }
    }

    // ---------------------------------------------------------------- cards

    private View buildCard(final WidgetStore.Item item) {
        if (WidgetStore.WEATHER.equals(item.type)) return buildWeatherCard(item);
        if (WidgetStore.ADDRESS.equals(item.type)) return buildAddressCard(item);
        if (WidgetStore.NOTE.equals(item.type)) return buildNoteCard(item);
        if (WidgetStore.QUOTE.equals(item.type)) return buildQuoteCard(item);
        return null;
    }

    private LinearLayout baseCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_widget_card);
        card.setPadding(dp(16), dp(13), dp(16), dp(13));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private TextView text(String value, float sizeSp, int color, boolean medium) {
        TextView view = new TextView(activity);
        view.setText(value == null ? "" : value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(medium ? Typeface.create(typeface, Typeface.BOLD) : typeface);
        return view;
    }

    private int color(int id) {
        return activity.getResources().getColor(id);
    }

    private LinearLayout buildWeatherCard(final WidgetStore.Item item) {
        final LinearLayout card = baseCard();
        String city = item.title == null || item.title.trim().isEmpty()
                ? activity.getString(R.string.weather_default_city) : item.title.trim();

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(city, 13f, color(R.color.on_surface_variant), false);
        name.setMaxLines(1);
        header.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView hint = text(activity.getString(R.string.widget_refresh_hint),
                10f, color(R.color.on_surface_variant), false);
        header.addView(hint);
        card.addView(header);

        Weather weather = readWeather();
        boolean fresh = weather != null && weather.city.equals(city)
                && System.currentTimeMillis() - weather.time < WEATHER_TTL_MS;
        boolean failed = weatherErrorCity != null && weatherErrorCity.equals(city)
                && System.currentTimeMillis() - weatherErrorAt < WEATHER_RETRY_MS;

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        row.setPadding(0, dp(6), 0, 0);

        String tempText;
        String descText;
        String metaText;
        if (weather != null && weather.city.equals(city)) {
            tempText = Math.round(weather.temperature) + "°";
            descText = weather.description;
            metaText = activity.getString(R.string.widget_weather_updated,
                    new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(weather.time)));
        } else if (failed) {
            tempText = "--";
            descText = activity.getString(weatherErrorCode == WEATHER_ERROR_CITY
                    ? R.string.widget_weather_city_missing : R.string.widget_weather_failed);
            metaText = "";
        } else {
            tempText = "--";
            descText = activity.getString(R.string.widget_weather_loading);
            metaText = "";
        }

        TextView temp = text(tempText, 34f, accentColor, true);
        row.addView(temp);
        TextView desc = text(descText, 12f, color(R.color.on_surface), false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.leftMargin = dp(10);
        descParams.bottomMargin = dp(6);
        row.addView(desc, descParams);
        card.addView(row);

        if (!metaText.isEmpty()) {
            TextView meta = text(metaText, 10f, color(R.color.on_surface_variant), false);
            meta.setPadding(0, dp(2), 0, 0);
            card.addView(meta);
        }

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (weatherErrorCode == WEATHER_ERROR_CITY) {
                    showEditor(item);
                    return;
                }
                weatherErrorCity = null;
                weatherErrorAt = 0L;
                weatherErrorCode = WEATHER_ERROR_NONE;
                refresh();
            }
        });

        if (!fresh) loadWeather(item);
        return card;
    }

    private LinearLayout buildAddressCard(final WidgetStore.Item item) {
        final LinearLayout card = baseCard();
        final String url = item.value == null ? "" : item.value.trim();
        String title = item.title == null || item.title.trim().isEmpty()
                ? hostOf(url) : item.title.trim();

        card.addView(text(activity.getString(R.string.widget_address), 11f,
                color(R.color.on_surface_variant), false));
        TextView name = text(title, 16f, color(R.color.on_surface), true);
        name.setPadding(0, dp(3), 0, 0);
        name.setMaxLines(1);
        card.addView(name);
        TextView link = text(url, 11f, accentColor, false);
        link.setPadding(0, dp(3), 0, 0);
        link.setMaxLines(1);
        card.addView(link);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    activity.openFromWidget(url);
                } else {
                    showEditor(item);
                }
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showEditor(item);
                return true;
            }
        });
        return card;
    }

    private LinearLayout buildNoteCard(final WidgetStore.Item item) {
        final LinearLayout card = baseCard();
        card.addView(text(activity.getString(R.string.widget_note), 11f,
                color(R.color.on_surface_variant), false));
        String body = item.value == null ? "" : item.value.trim();
        TextView content = text(body.isEmpty()
                        ? activity.getString(R.string.widget_note_empty) : body,
                13f, body.isEmpty() ? color(R.color.on_surface_variant) : color(R.color.on_surface),
                false);
        content.setPadding(0, dp(4), 0, 0);
        content.setMaxLines(5);
        content.setLineSpacing(dp(2), 1f);
        card.addView(content);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditor(item);
            }
        });
        return card;
    }

    private LinearLayout buildQuoteCard(final WidgetStore.Item item) {
        final LinearLayout card = baseCard();
        QuoteRepository.Quote quote = quotes.current();
        TextView body = text(quote.text, 14f, color(R.color.on_surface), false);
        body.setMaxLines(4);
        body.setEllipsize(TextUtils.TruncateAt.END);
        body.setLineSpacing(dp(2), 1f);
        card.addView(body);

        String attribution = quote.attribution();
        String hint = activity.getString(R.string.widget_quote_hint);
        TextView footer = text(attribution.isEmpty() ? hint : attribution + " · " + hint,
                10f, color(R.color.on_surface_variant), false);
        footer.setPadding(0, dp(8), 0, 0);
        card.addView(footer);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quotes.next();
                refresh();
            }
        });
        return card;
    }

    private String hostOf(String url) {
        try {
            String host = Uri.parse(url).getHost();
            if (host != null && !host.isEmpty()) return host;
        } catch (Throwable ignored) {
        }
        return activity.getString(R.string.widget_address);
    }

    // -------------------------------------------------------------- manager

    void showManager() {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_widgets);
        managerDialog = dialog;

        LinearLayout rows = dialog.findViewById(R.id.widget_rows);
        for (WidgetStore.Item item : items) {
            rows.addView(buildManagerRow(dialog, item));
        }
        dialog.findViewById(R.id.widgets_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (managerDialog == dialog) managerDialog = null;
            }
        });
        dialog.show();
        styleDialog(dialog, 0.92f, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View buildManagerRow(final Dialog manager, final WidgetStore.Item item) {
        View row = activity.getLayoutInflater().inflate(R.layout.item_widget, null, false);
        TextView title = row.findViewById(R.id.widget_row_title);
        TextView sub = row.findViewById(R.id.widget_row_sub);
        CheckBox check = row.findViewById(R.id.widget_row_check);

        title.setText(titleOf(item));
        sub.setText(summaryOf(item));
        check.setChecked(item.enabled);
        check.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                item.enabled = isChecked;
                WidgetStore.save(activity, items);
                refresh();
                Toast.makeText(activity, isChecked ? R.string.widget_enabled : R.string.widget_disabled,
                        Toast.LENGTH_SHORT).show();
            }
        });

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WidgetStore.QUOTE.equals(item.type)) {
                    // Quotes have nothing to configure; tapping the row flips the switch.
                    CheckBox box = v.findViewById(R.id.widget_row_check);
                    box.setChecked(!box.isChecked());
                } else {
                    showEditor(item);
                }
            }
        });
        return row;
    }

    private String titleOf(WidgetStore.Item item) {
        if (WidgetStore.WEATHER.equals(item.type)) return activity.getString(R.string.widget_weather);
        if (WidgetStore.ADDRESS.equals(item.type)) return activity.getString(R.string.widget_address);
        if (WidgetStore.NOTE.equals(item.type)) return activity.getString(R.string.widget_note);
        return activity.getString(R.string.widget_quote);
    }

    /** Keeps the manager rows in sync after an edit, without closing the dialog. */
    private void updateManagerSummaries() {
        if (managerDialog == null) return;
        LinearLayout rows = managerDialog.findViewById(R.id.widget_rows);
        if (rows == null) return;
        for (int i = 0; i < rows.getChildCount() && i < items.length; i++) {
            TextView sub = rows.getChildAt(i).findViewById(R.id.widget_row_sub);
            if (sub != null) sub.setText(summaryOf(items[i]));
        }
    }

    private String summaryOf(WidgetStore.Item item) {
        if (WidgetStore.WEATHER.equals(item.type)) {
            return item.title == null || item.title.trim().isEmpty()
                    ? activity.getString(R.string.weather_no_city) : item.title;
        }
        if (WidgetStore.ADDRESS.equals(item.type)) {
            return item.title + " · " + item.value;
        }
        if (WidgetStore.NOTE.equals(item.type)) {
            String body = item.value == null ? "" : item.value.trim().replace('\n', ' ');
            if (body.isEmpty()) return activity.getString(R.string.weather_no_content);
            return body.length() > 40 ? body.substring(0, 40) + "…" : body;
        }
        return activity.getString(R.string.widget_quote_summary);
    }

    // --------------------------------------------------------------- editor

    private void showEditor(final WidgetStore.Item item) {
        if (WidgetStore.QUOTE.equals(item.type)) return;
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_widget_edit);
        editDialog = dialog;

        TextView title = dialog.findViewById(R.id.widget_edit_title);
        TextView labelA = dialog.findViewById(R.id.widget_edit_label_a);
        TextView labelB = dialog.findViewById(R.id.widget_edit_label_b);
        final EditText fieldA = dialog.findViewById(R.id.widget_edit_a);
        final EditText fieldB = dialog.findViewById(R.id.widget_edit_b);

        title.setText(activity.getString(R.string.widget_edit_title) + " · " + titleOf(item));

        if (WidgetStore.WEATHER.equals(item.type)) {
            labelA.setText(R.string.widget_weather_city);
            fieldA.setText(item.title);
            fieldA.setHint(R.string.widget_weather_city_hint);
            labelB.setVisibility(View.GONE);
            fieldB.setVisibility(View.GONE);
        } else if (WidgetStore.ADDRESS.equals(item.type)) {
            labelA.setText(R.string.widget_address_title);
            fieldA.setText(item.title);
            fieldA.setHint(R.string.widget_address_hint);
            labelB.setText(R.string.widget_address_url);
            fieldB.setText(item.value);
            fieldB.setHint(R.string.widget_address_url_hint);
            fieldB.setSingleLine(true);
            fieldB.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            fieldB.setMinLines(1);
            fieldB.setMaxLines(1);
            setFieldHeight(fieldB, dp(50));
        } else {
            labelA.setText(R.string.widget_note_label);
            fieldA.setText(item.value);
            fieldA.setHint(R.string.widget_note_hint);
            fieldA.setSingleLine(false);
            fieldA.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            fieldA.setMinLines(4);
            fieldA.setMaxLines(8);
            fieldA.setGravity(Gravity.TOP | Gravity.START);
            setFieldHeight(fieldA, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelB.setVisibility(View.GONE);
            fieldB.setVisibility(View.GONE);
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
                String a = fieldA.getText().toString().trim();
                String b = fieldB.getText().toString().trim();
                if (WidgetStore.WEATHER.equals(item.type)) {
                    if (a.isEmpty()) {
                        Toast.makeText(activity, R.string.widget_weather_city_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    validateCityAndSave(item, a, dialog);
                    return;
                } else if (WidgetStore.ADDRESS.equals(item.type)) {
                    if (!b.startsWith("http://") && !b.startsWith("https://")) {
                        Toast.makeText(activity, R.string.widget_address_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    item.title = a.isEmpty() ? hostOf(b) : a;
                    item.value = b;
                } else {
                    item.value = fieldA.getText().toString();
                }
                WidgetStore.save(activity, items);
                if (WidgetStore.WEATHER.equals(item.type)) {
                    weatherErrorCity = null;
                    weatherErrorAt = 0L;
                }
                updateManagerSummaries();
                refresh();
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                if (editDialog == dialog) editDialog = null;
            }
        });
        dialog.show();
        styleDialog(dialog, 0.92f, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Checks the city against the geocoder before committing it, so a typo cannot
     *  silently turn into weather for an unrelated place. */
    private void validateCityAndSave(final WidgetStore.Item item, final String city,
                                     final Dialog dialog) {
        final Button save = dialog.findViewById(R.id.widget_edit_save);
        save.setEnabled(false);
        Toast.makeText(activity, R.string.widget_weather_checking, Toast.LENGTH_SHORT).show();
        io.execute(new Runnable() {
            @Override
            public void run() {
                WeatherFetch result = null;
                try {
                    result = fetchWeather(city);
                } catch (Throwable ignored) {
                }
                final WeatherFetch fetched = result;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        save.setEnabled(true);
                        if (destroyed) return;
                        if (fetched == null) {
                            Toast.makeText(activity, R.string.widget_weather_network,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (fetched.cityNotFound) {
                            Toast.makeText(activity, R.string.widget_weather_city_not_found,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        item.title = city;
                        WidgetStore.save(activity, items);
                        if (fetched.weather != null) saveWeather(fetched.weather);
                        weatherErrorCity = null;
                        weatherErrorAt = 0L;
                        weatherErrorCode = WEATHER_ERROR_NONE;
                        updateManagerSummaries();
                        refresh();
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    private void setFieldHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = height;
            view.setLayoutParams(params);
        }
    }

    private void styleDialog(Dialog dialog, float widthRatio, int height) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.34f);
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        window.setLayout(Math.min((int) (screenWidth * widthRatio), dp(430)), height);
        window.setGravity(Gravity.CENTER);
    }

    // -------------------------------------------------------------- weather

    private static final class Weather {
        String city = "";
        double temperature;
        String description = "";
        long time;
    }

    private void loadWeather(final WidgetStore.Item item) {
        if (destroyed || weatherLoading) return;
        final String city = item.title == null || item.title.trim().isEmpty()
                ? activity.getString(R.string.weather_default_city) : item.title.trim();
        final Weather cached = readWeather();
        long now = System.currentTimeMillis();
        if (cached != null && cached.city.equals(city) && now - cached.time < WEATHER_TTL_MS) {
            return;
        }
        weatherLoading = true;
        io.execute(new Runnable() {
            @Override
            public void run() {
                WeatherFetch result = null;
                try {
                    result = fetchWeather(city);
                } catch (Throwable ignored) {
                }
                final WeatherFetch fetched = result;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        weatherLoading = false;
                        if (destroyed) return;
                        if (fetched == null || fetched.weather == null) {
                            weatherErrorCity = city;
                            weatherErrorAt = System.currentTimeMillis();
                            weatherErrorCode = fetched != null && fetched.cityNotFound
                                    ? WEATHER_ERROR_CITY : WEATHER_ERROR_NETWORK;
                        } else {
                            saveWeather(fetched.weather);
                            weatherErrorCity = null;
                            weatherErrorAt = 0L;
                            weatherErrorCode = WEATHER_ERROR_NONE;
                        }
                        refresh();
                    }
                });
            }
        });
    }

    private static final class WeatherFetch {
        Weather weather;
        boolean cityNotFound;
    }

    private WeatherFetch fetchWeather(String city) throws Exception {
        WeatherFetch result = new WeatherFetch();
        double[] coordinates = geocodeCity(city);
        if (coordinates == null) {
            result.cityNotFound = true;
            return result;
        }
        // wttr.in accepts a "lat,lon" path and needs no API key. Asking by coordinate
        // avoids its own weak geocoder silently picking a different place for a
        // misspelled or unknown city.
        String url = String.format(Locale.US,
                "https://wttr.in/%.4f,%.4f?format=j1", coordinates[0], coordinates[1]);
        JSONObject root = new JSONObject(httpGet(url, 256 * 1024));
        JSONArray conditions = root.optJSONArray("current_condition");
        if (conditions == null || conditions.length() == 0) return result;
        JSONObject current = conditions.optJSONObject(0);
        if (current == null) return result;
        double temperature = current.optDouble("temp_C", Double.NaN);
        if (Double.isNaN(temperature)) return result;
        Weather weather = new Weather();
        weather.city = city;
        weather.temperature = temperature;
        weather.description = describeWeather(current.optInt("weatherCode", -1));
        weather.time = System.currentTimeMillis();
        result.weather = weather;
        return result;
    }

    /** Resolves a city name; a null result means the geocoder knows no such place. */
    private double[] geocodeCity(String city) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(city, "UTF-8") + "&count=1&language=zh&format=json";
        JSONObject root = new JSONObject(httpGet(url, 128 * 1024));
        JSONArray results = root.optJSONArray("results");
        if (results == null || results.length() == 0) return null;
        JSONObject place = results.optJSONObject(0);
        if (place == null) return null;
        double latitude = place.optDouble("latitude", Double.NaN);
        double longitude = place.optDouble("longitude", Double.NaN);
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) return null;
        return new double[]{latitude, longitude};
    }

    /** Maps the WWO condition codes returned by wttr.in to short Chinese labels. */
    private String describeWeather(int code) {
        switch (code) {
            case 113: return activity.getString(R.string.weather_sunny);
            case 116: return activity.getString(R.string.weather_mostly_sunny);
            case 119: return activity.getString(R.string.weather_partly_cloudy);
            case 122: return activity.getString(R.string.weather_cloudy);
            case 143: case 248: case 260: return activity.getString(R.string.weather_fog);
            case 176: case 263: case 266: case 293: case 296: case 353: return activity.getString(R.string.weather_light_rain);
            case 299: case 302: case 356: return activity.getString(R.string.weather_rain);
            case 305: case 308: case 359: return activity.getString(R.string.weather_heavy_rain);
            case 179: case 323: case 326: case 368: return activity.getString(R.string.weather_light_snow);
            case 329: case 332: case 371: return activity.getString(R.string.weather_snow);
            case 335: case 338: return activity.getString(R.string.weather_heavy_snow);
            case 182: case 185: case 281: case 284: case 311: case 314:
            case 317: case 320: case 362: case 365: case 374: case 377:
                return activity.getString(R.string.weather_sleet);
            case 200: case 386: case 389: case 392: case 395: return activity.getString(R.string.weather_thunder);
            case 227: case 230: return activity.getString(R.string.weather_blizzard);
            case 350: return activity.getString(R.string.weather_ice);
            default: return activity.getString(R.string.weather_unknown);
        }
    }

    private static String httpGet(String url, int max) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "DeepExplorer/1.9");
            input = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int n;
            while (total < max && (n = input.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                total += n;
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            if (input != null) try { input.close(); } catch (Throwable ignored) { }
            if (connection != null) try { connection.disconnect(); } catch (Throwable ignored) { }
        }
    }

    private Weather readWeather() {
        SharedPreferences prefs = activity.getSharedPreferences(WEATHER_FILE, Context.MODE_PRIVATE);
        String city = prefs.getString("city", "");
        long time = prefs.getLong("time", 0L);
        if (city == null || city.isEmpty() || time <= 0L) return null;
        Weather weather = new Weather();
        weather.city = city;
        weather.temperature = Double.longBitsToDouble(prefs.getLong("temp_bits", 0L));
        weather.description = prefs.getString("desc", "");
        weather.time = time;
        return weather;
    }

    private void saveWeather(Weather weather) {
        activity.getSharedPreferences(WEATHER_FILE, Context.MODE_PRIVATE).edit()
                .putString("city", weather.city)
                .putLong("temp_bits", Double.doubleToLongBits(weather.temperature))
                .putString("desc", weather.description)
                .putLong("time", weather.time)
                .apply();
    }

    // --------------------------------------------------------------- helper

    private void applyTypeface(View view) {
        if (view == null) return;
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            boolean medium = textView.getTypeface() != null && textView.getTypeface().isBold();
            textView.setTypeface(medium ? Typeface.create(typeface, Typeface.BOLD) : typeface);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyTypeface(group.getChildAt(i));
        }
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                activity.getResources().getDisplayMetrics());
    }
}
