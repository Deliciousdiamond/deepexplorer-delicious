package com.deliciousdiamond.deepexplorer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal DeepSeek chat-completions client with SSE streaming.
 *
 * <p>Every request owns its connection and stream instead of sharing mutable fields, so
 * cancelling an old request can never tear down the connection of a newer one. Callbacks
 * are only delivered while the request that produced them is still the current one.</p>
 */
public final class AiClient {
    public interface Listener {
        void onStart();

        void onChunk(String text);

        void onDone(String fullText);

        void onError(String message);
    }

    private static final class Request {
        final int id;
        volatile boolean cancelled;
        volatile HttpURLConnection connection;
        volatile InputStream stream;
        Thread thread;

        Request(int id) {
            this.id = id;
        }
    }

    private volatile Request current;
    private int nextId;

    public void stream(final String endpoint, final String apiKey, final String model,
                       final JSONArray messages, final Listener listener) {
        final Request request = new Request(++nextId);
        Request previous = current;
        current = request;
        if (previous != null) cancelRequest(previous);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                runRequest(request, endpoint, apiKey, model, messages, listener);
            }
        }, "deepseek-sse");
        request.thread = thread;
        thread.setDaemon(true);
        thread.start();
    }

    private void runRequest(final Request request, String endpoint, String apiKey, String model,
                            JSONArray messages, Listener listener) {
        InputStream input = null;
        try {
            if (!isActive(request)) return;
            if (apiKey == null || apiKey.trim().isEmpty()) {
                if (!isActive(request)) return;
                listener.onError("请先在设置中填写 DeepSeek API Key");
                return;
            }
            JSONObject body = new JSONObject();
            String chosenModel = model == null || model.isEmpty() ? "deepseek-chat" : model;
            body.put("model", chosenModel);
            body.put("messages", messages);
            body.put("stream", true);
            String lower = chosenModel.toLowerCase();
            boolean reasoning = "deepseek-reasoner".equals(chosenModel)
                    || lower.contains("reasoner") || lower.startsWith("o1") || lower.startsWith("o3");
            if (!reasoning) body.put("temperature", 0.7);
            body.put("max_tokens", 2048);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);

            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            request.connection = conn;
            if (!isActive(request)) return;
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setRequestProperty("User-Agent", "DeepExplorer/1.9 Android");

            OutputStream out = conn.getOutputStream();
            out.write(payload);
            out.flush();
            out.close();

            int code = conn.getResponseCode();
            if (code != 200) {
                String detail = readLimited(conn.getErrorStream(), 4096);
                if (isActive(request)) listener.onError(friendlyError(code, detail));
                return;
            }

            input = conn.getInputStream();
            request.stream = input;
            if (!isActive(request)) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            listener.onStart();

            StringBuilder full = new StringBuilder();
            String line;
            while (isActive(request) && (line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty()) continue;
                if ("[DONE]".equals(data)) break;
                try {
                    JSONObject chunk = new JSONObject(data);
                    JSONArray choices = chunk.optJSONArray("choices");
                    if (choices == null || choices.length() == 0) continue;
                    JSONObject choice = choices.optJSONObject(0);
                    if (choice == null) continue;
                    JSONObject delta = choice.optJSONObject("delta");
                    if (delta == null || delta.isNull("content")) continue;
                    String text = delta.optString("content", "");
                    if (!text.isEmpty()) {
                        full.append(text);
                        if (!isActive(request)) return;
                        listener.onChunk(text);
                    }
                } catch (Exception ignored) {
                    // Ignore keep-alives or a malformed SSE line; the next line is independent.
                }
            }
            if (isActive(request)) listener.onDone(full.toString());
        } catch (Throwable t) {
            if (isActive(request)) listener.onError(networkMessage(t));
        } finally {
            if (input != null) {
                try { input.close(); } catch (Throwable ignored) { }
            }
            if (request.connection != null) {
                try { request.connection.disconnect(); } catch (Throwable ignored) { }
            }
            request.stream = null;
            request.connection = null;
        }
    }

    /** True while this request is the newest one and has not been cancelled. */
    private boolean isActive(Request request) {
        return request != null && request == current && !request.cancelled;
    }

    private static void cancelRequest(Request request) {
        request.cancelled = true;
        try {
            if (request.stream != null) request.stream.close();
        } catch (Throwable ignored) {
        }
        try {
            if (request.connection != null) request.connection.disconnect();
        } catch (Throwable ignored) {
        }
        Thread thread = request.thread;
        if (thread != null) thread.interrupt();
    }

    public void cancel() {
        Request request = current;
        current = null;
        if (request != null) cancelRequest(request);
    }

    public boolean isStreaming() {
        Request request = current;
        return request != null && !request.cancelled
                && request.thread != null && request.thread.isAlive();
    }

    private static String networkMessage(Throwable t) {
        String message = t == null ? "" : String.valueOf(t.getMessage());
        if (message.toLowerCase().contains("timeout")) return "请求超时，请稍后再试。";
        return "网络连接失败，请检查网络后重试。";
    }

    private static String friendlyError(int code, String detail) {
        if (code == 401) return "API Key 无效或已过期。";
        if (code == 402) return "API 余额不足。";
        if (code == 429) return "请求过于频繁，请稍后再试。";
        String fromBody = null;
        try {
            JSONObject object = new JSONObject(detail);
            JSONObject error = object.optJSONObject("error");
            if (error != null) fromBody = error.optString("message", null);
        } catch (Exception ignored) {
        }
        if (fromBody != null && !fromBody.isEmpty()) return "请求失败：" + fromBody;
        return "请求失败（HTTP " + code + "）";
    }

    private static String readLimited(InputStream input, int max) {
        if (input == null) return "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;
            int total = 0;
            while (total < max) {
                n = input.read(buffer, 0, Math.min(buffer.length, max - total));
                if (n <= 0) break;
                out.write(buffer, 0, n);
                total += n;
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        } finally {
            try { input.close(); } catch (IOException ignored) { }
        }
    }
}
