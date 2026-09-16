package com.example.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * Writes a {@link CatalogPage} as JSON for the front-end team.
 *
 * <p>商品データは店舗側の入力なので、文字列はすべて {@link #appendString} を通し、RFC 8259 に従って
 * エスケープする。ASCII 以外の文字はそのまま書き出す（応答は UTF-8 で送るため）。
 */
public class CatalogJsonWriter {

    public String write(CatalogPage page) {
        List<Product> products = page.products();
        StringBuilder json = new StringBuilder(64 + 128 * products.size());
        json.append("{\"page\":").append(page.page())
                .append(",\"pageSize\":").append(page.pageSize())
                .append(",\"hasNext\":").append(page.hasNext())
                .append(",\"products\":[");
        for (int i = 0; i < products.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            appendProduct(json, products.get(i));
        }
        return json.append("]}").toString();
    }

    private static void appendProduct(StringBuilder json, Product product) {
        json.append("{\"sku\":");
        appendString(json, product.sku());
        json.append(",\"name\":");
        appendString(json, product.displayName());
        json.append(",\"category\":");
        appendString(json, product.category());
        json.append(",\"price\":");
        appendNumber(json, product.price());
        json.append(",\"listedAt\":");
        appendString(json, product.listedAt() == null ? null : product.listedAt().toString());
        json.append('}');
    }

    /**
     * エスケープ済みの JSON 文字列を追記する。値が無い場合はリテラルの {@code null} を書く。
     */
    private static void appendString(StringBuilder json, String value) {
        if (value == null) {
            json.append("null");
            return;
        }
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }

    /**
     * JSON 数値を指数表記なしで追記する。値が無い場合はリテラルの {@code null} を書く。
     * {@code 1E+3} のような表記がフロントに渡らないようにするため。
     */
    private static void appendNumber(StringBuilder json, BigDecimal value) {
        json.append(value == null ? "null" : value.toPlainString());
    }
}
