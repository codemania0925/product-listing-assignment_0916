package com.example.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One product in the catalog. Drafts may not have a name, a price, or a listing date yet.
 *
 * @param sku      unique stock keeping unit such as "MON-27-4K"; never null
 * @param name     the display name, or null for a draft without one
 * @param category the category such as "monitors"; never null
 * @param price    the price, or null for a draft that is not priced yet
 * @param listedAt the day the product went on sale, or null for a draft that is not on sale yet
 */
public record Product(String sku, String name, String category, BigDecimal price, LocalDate listedAt) {

    /**
     * 購入者に表示する名前。名前がまだ無い下書きでは SKU で代用する。
     */
    public String displayName() {
        return name != null ? name : sku;
    }
}
