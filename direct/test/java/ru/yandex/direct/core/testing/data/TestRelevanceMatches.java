package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.util.Collections;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;

import static java.time.LocalDateTime.now;

public class TestRelevanceMatches {

    public static final BigDecimal DEFAULT_PRICE_SEARCH = new BigDecimal("12.30");
    public static final BigDecimal DEFAULT_PRICE_CONTEXT = new BigDecimal("11.20");

    public static RelevanceMatch defaultRelevanceMatch() {
        return new RelevanceMatch()
                .withHrefParam1("param1")
                .withHrefParam1("param2")
                .withLastChangeTime(now())
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withAutobudgetPriority(3)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPrice(DEFAULT_PRICE_SEARCH)
                .withPriceContext(DEFAULT_PRICE_CONTEXT)
                .withRelevanceMatchCategories(Collections.emptySet());
    }
}
