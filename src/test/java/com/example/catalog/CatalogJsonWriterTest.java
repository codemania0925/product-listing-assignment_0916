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

    /** バグ2: {@code Monitor 27" 4K} は名前の途中で JSON 文字列を閉じてしまっていた。 */
    @Test
    void escapesQuotesAndBackslashesInNames() throws Exception {
        String name = "Monitor 27\" 4K \\ \"wide\"";

        assertEquals(name, productOf(pageWithName(name)).get("name").asText());
    }

    /** バグ2: 制御文字は JSON 文字列の中にそのまま書けない。 */
    @Test
    void escapesControlCharactersInNames() throws Exception {
        String name = "two\nlines\tand a bell" + (char) 0x07;

        assertEquals(name, productOf(pageWithName(name)).get("name").asText());
    }

    /** バグ2: ASCII 以外の名前もそのまま読める形で出力する。 */
    @Test
    void keepsNamesOutsideAsciiReadable() throws Exception {
        String name = "テンキーレスキーボード 日本語配列";

        assertEquals(name, productOf(pageWithName(name)).get("name").asText());
    }

    private static CatalogPage pageWithName(String name) {
        return new CatalogPage(List.of(
                new Product("MON-27-4K", name, "monitors", new BigDecimal("329.00"), LocalDate.of(2026, 3, 1))),
                1, 10, false);
    }

    private static JsonNode productOf(CatalogPage page) throws Exception {
        return new ObjectMapper().readTree(new CatalogJsonWriter().write(page)).get("products").get(0);
    }
}
