package com.example.catalog;

/**
 * Writes a {@link CatalogPage} as JSON for the front-end team.
 */
public class CatalogJsonWriter {

    public String write(CatalogPage page) {
        StringBuilder json = new StringBuilder();
        json.append("{\"page\":").append(page.page())
                .append(",\"pageSize\":").append(page.pageSize())
                .append(",\"hasNext\":").append(page.hasNext())
                .append(",\"products\":[");
        for (int i = 0; i < page.products().size(); i++) {
            Product product = page.products().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"sku\":\"").append(product.sku())
                    .append("\",\"name\":\"").append(product.name())
                    .append("\",\"category\":\"").append(product.category())
                    .append("\",\"price\":").append(product.price())
                    .append(",\"listedAt\":\"").append(product.listedAt())
                    .append("\"}");
        }
        return json.append("]}").toString();
    }
}
