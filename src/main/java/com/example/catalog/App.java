package com.example.catalog;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A small HTTP API for the product listing: {@code GET /products?page=1&pageSize=10}.
 */
public class App {

    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 50;
    static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    /**
     * Starts the server on localhost. Use port 0 to pick any free port.
     */
    public static HttpServer start(int port, ProductRepository repository) throws IOException {
        ProductCatalog catalog = new ProductCatalog(repository);
        CatalogJsonWriter writer = new CatalogJsonWriter();

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/products", exchange -> {
            try {
                handle(exchange, catalog, writer);
            } catch (IOException | RuntimeException e) {
                LOG.log(Level.SEVERE, e, () -> "Could not serve " + exchange.getRequestURI());
                sendStatus(exchange, 500);
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = start(8080, new InMemoryProductRepository(SampleData.products()));
        System.out.println("Listening on http://localhost:" + server.getAddress().getPort() + "/products");
    }

    private static void handle(HttpExchange exchange, ProductCatalog catalog, CatalogJsonWriter writer)
            throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        Integer page = parsePositiveInt(query.get("page"), 1);
        Integer pageSize = parsePositiveInt(query.get("pageSize"), DEFAULT_PAGE_SIZE);
        if (page == null || pageSize == null || pageSize > MAX_PAGE_SIZE) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        // Content-Length は文字数ではなくバイト数。ASCII 以外の文字は UTF-8 で 1 バイトに収まらない。
        byte[] json = writer.write(catalog.getPage(page, pageSize)).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_JSON);
        exchange.sendResponseHeaders(200, json.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(json);
        }
    }

    /**
     * 本文なしで応答を返す。すでに応答の送信が始まっている場合は何もしない。
     */
    private static void sendStatus(HttpExchange exchange, int status) {
        try {
            if (exchange.getResponseCode() == -1) {
                exchange.sendResponseHeaders(status, -1);
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Could not send the error response", e);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            query.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return query;
    }

    /**
     * Returns the default when the value is missing, and null when it is not a positive integer.
     */
    private static Integer parsePositiveInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
