package com.example.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * Writes a {@link CatalogPage} as JSON for the front-end team.
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
     * JSON 文字列を追記する。値が無い場合はリテラルの {@code null} を書く。
     */
    private static void appendString(StringBuilder json, String value) {
        if (value == null) {
            json.append("null");
            return;
        }
        json.append('"').append(value).append('"');
    }

    /**
     * JSON 数値を指数表記なしで追記する。値が無い場合はリテラルの {@code null} を書く。
     */
    private static void appendNumber(StringBuilder json, BigDecimal value) {
        json.append(value == null ? "null" : value.toPlainString());
    }
}
