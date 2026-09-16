package com.example.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class CatalogJsonWriterTest {

    @Test
    void writesPageAsJson() throws Exception {
        CatalogPage page = new CatalogPage(List.of(
                new Product("CAB-USBC-2M", "USB-C cable 2 m", "cables", new BigDecimal("12.50"),
                        LocalDate.of(2026, 3, 1))), 1, 10, false);

        JsonNode json = new ObjectMapper().readTree(new CatalogJsonWriter().write(page));

        assertEquals(1, json.get("page").asInt());
        assertEquals(10, json.get("pageSize").asInt());
        assertFalse(json.get("hasNext").asBoolean());
        JsonNode product = json.get("products").get(0);
        assertEquals("CAB-USBC-2M", product.get("sku").asText());
        assertEquals("USB-C cable 2 m", product.get("name").asText());
        assertEquals("cables", product.get("category").asText());
        assertEquals(12.5, product.get("price").asDouble());
        assertEquals("2026-03-01", product.get("listedAt").asText());
    }

    /** バグ1: 下書きは名前・価格・出品日をまだ持たない。 */
    @Test
    void writesADraftWithItsSkuAsNameAndNullForTheMissingFields() throws Exception {
        CatalogPage page = new CatalogPage(List.of(
                new Product("MON-32-DRAFT", null, "monitors", null, null)), 1, 10, false);

        JsonNode product = productOf(page);

        assertEquals("MON-32-DRAFT", product.get("sku").asText());
        assertEquals("MON-32-DRAFT", product.get("name").asText());
        assertTrue(product.get("price").isNull(), "price は文字列の null ではなく JSON の null であること");
        assertTrue(product.get("listedAt").isNull(), "listedAt は文字列の null ではなく JSON の null であること");
    }

    private static JsonNode productOf(CatalogPage page) throws Exception {
        return new ObjectMapper().readTree(new CatalogJsonWriter().write(page)).get("products").get(0);
    }
}
