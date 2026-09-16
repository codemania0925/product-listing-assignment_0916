package com.example.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Products for running {@link App} locally.
 */
final class SampleData {

    private SampleData() {
    }

    static List<Product> products() {
        return List.of(
                new Product("MON-27-4K", "Monitor 27\" 4K", "monitors", new BigDecimal("329.00"), LocalDate.of(2026, 3, 1)),
                new Product("KEY-JP-01", "テンキーレスキーボード 日本語配列", "keyboards", new BigDecimal("89.00"), LocalDate.of(2026, 6, 15)),
                new Product("CAB-USBC-2M", "USB-C cable 2 m", "cables", new BigDecimal("12.50"), LocalDate.of(2026, 9, 1)),
                new Product("CAB-USBC-1M", "USB-C cable 1 m", "cables", new BigDecimal("9.50"), LocalDate.of(2026, 9, 1)),
                new Product("MON-32-DRAFT", null, "monitors", null, null));
    }
}
