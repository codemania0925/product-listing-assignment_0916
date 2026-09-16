package com.example.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

// 応答が壊れるとクライアントは待ち続けるため、HTTP テストがビルドを止めないよう上限を設ける。
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
class AppTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void servesTheFirstPage() throws Exception {
        server = App.start(0, new InMemoryProductRepository(List.of(
                new Product("CAB-USBC-2M", "USB-C cable 2 m", "cables", new BigDecimal("12.50"),
                        LocalDate.of(2026, 3, 1)))));

        HttpResponse<String> response = get("/products");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals("CAB-USBC-2M",
                new ObjectMapper().readTree(response.body()).get("products").get(0).get("sku").asText());
    }

    @Test
    void rejectsInvalidPage() throws Exception {
        server = App.start(0, new InMemoryProductRepository(List.of()));

        assertEquals(400, get("/products?page=0").statusCode());
    }

    /** 一覧の組み立てに失敗したらサーバエラーを返す。空ページでも接続切断でもない。 */
    @Test
    void answersWithAServerErrorWhenTheCatalogFails() throws Exception {
        server = App.start(0, () -> {
            throw new IllegalStateException("database is down");
        });

        assertEquals(500, get("/products").statusCode());
    }

    /** バグ2: ASCII 以外の名前はバイト数が文字数より多く、本文が途中で切られていた。 */
    @Test
    void servesUtf8JsonForNamesOutsideAscii() throws Exception {
        String name = "テンキーレスキーボード 日本語配列";
        server = App.start(0, new InMemoryProductRepository(List.of(
                new Product("KEY-JP-01", name, "keyboards", new BigDecimal("89.00"), LocalDate.of(2026, 6, 15)))));

        HttpResponse<byte[]> response = getBytes("/products");

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        String body = new String(response.body(), StandardCharsets.UTF_8);
        assertEquals(name, new ObjectMapper().readTree(body).get("products").get(0).get("name").asText());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(path, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> getBytes(String path) throws Exception {
        return send(path, HttpResponse.BodyHandlers.ofByteArray());
    }

    private <T> HttpResponse<T> send(String path, HttpResponse.BodyHandler<T> bodyHandler) throws Exception {
        URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + path);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).build();
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build().send(request, bodyHandler);
    }
}
