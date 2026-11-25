package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import net.neoforged.fml.loading.FMLPaths;

final class Live2DPage implements AutoCloseable {
    private static final String WEB = "/fancyl2d/web/";
    static final Path CONFIG = FMLPaths.CONFIGDIR.get().resolve("fancyl2d");
    static final Path MODELS = CONFIG.resolve("models");
    static final Path CORE = CONFIG.resolve("live2dcubismcore.min.js");

    enum Status { LOADING, READY, ERROR }
    private final HttpServer server;
    private final String origin;
    private final String prefix = "/" + UUID.randomUUID() + "/";
    private final ModelSource model;
    private final String url;
    private volatile Status status = Status.LOADING;

    Live2DPage(ModelSource model, Live2DSettings settings) throws IOException {
        this.model = model;
        var config = settings.pageConfig("model/" + encode(model.fileName()).replace("+", "%20"));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        url = origin + prefix + "index.html?config=" + encode(config.toString());
        server.createContext(prefix, this::handle);
        server.setExecutor(task -> Thread.startVirtualThread(task));
        server.start();
    }

    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }

    String url() { return url; }
    Status status() { return status; }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            URI uri = exchange.getRequestURI();
            if (!origin.substring("http://".length()).equals(exchange.getRequestHeaders().getFirst("Host"))
                || !uri.getPath().startsWith(prefix)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String name = uri.getPath().substring(prefix.length());
            FancyL2D.LOGGER.debug("Live2D page: {} {}", exchange.getRequestMethod(), name);
            if (exchange.getRequestMethod().equals("POST") && (name.equals("ready") || name.equals("error"))) {
                if (!origin.equals(exchange.getRequestHeaders().getFirst("Origin"))) {
                    FancyL2D.LOGGER.debug("Rejected Live2D status callback from origin {}", exchange.getRequestHeaders().getFirst("Origin"));
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                if (name.equals("error")) {
                    String detail = new String(exchange.getRequestBody().readNBytes(1024), StandardCharsets.UTF_8);
                    FancyL2D.LOGGER.warn("Live2D browser: {}", detail.replaceAll("[\\p{Cntrl}]", " "));
                } else {
                    FancyL2D.LOGGER.debug("Live2D page ready");
                }
                status = name.equals("ready") ? Status.READY : Status.ERROR;
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try (var stream = switch (name) {
                case "core.js" -> Files.newInputStream(CORE);
                case "index.html" -> Live2DPage.class.getResourceAsStream(WEB + name);
                default -> {
                    if (name.startsWith("model/")) {
                        yield model.open(name.substring("model/".length()));
                    }
                    if (!name.matches("assets/[A-Za-z0-9_.-]+\\.(?:js|css)")) yield null;
                    yield Live2DPage.class.getResourceAsStream(WEB + name);
                }
            }) {
                if (stream == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                var headers = exchange.getResponseHeaders();
                headers.set("Content-Type", contentType(name));
                headers.set("Cache-Control", "no-store");
                headers.set("X-Content-Type-Options", "nosniff");
                headers.set("Referrer-Policy", "same-origin");
                headers.set("Content-Security-Policy", "default-src 'none'; script-src 'self' 'unsafe-eval'; style-src 'self'; connect-src 'self'; img-src 'self' blob: data:; media-src 'self'; frame-ancestors 'none'; base-uri 'none'");
                exchange.sendResponseHeaders(200, 0);
                stream.transferTo(exchange.getResponseBody());
            } catch (IOException | IllegalArgumentException failure) {
                if (exchange.getResponseCode() < 0) exchange.sendResponseHeaders(404, -1);
            }
        }
    }

    private static String contentType(String name) {
        return switch (name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)) {
            case "html" -> "text/html; charset=utf-8";
            case "js" -> "application/javascript; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "json" -> "application/json";
            case "png" -> "image/png";
            case "jpeg", "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "mp3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    @Override public void close() { server.stop(0); }
}
