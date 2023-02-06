package ru.yandex.market.logistics.management.service.export.dynamic.source.repository;

import java.time.LocalDate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;

public abstract class Constants extends AbstractContextualTest {
    public static final LocalDate START_DATE = LocalDate.of(2020, 9, 7);

    static final PlatformClient BERU = new PlatformClient()
        .setId(1L)
        .setName("Беру");
    static final PlatformClient BRINGLY = new PlatformClient()
        .setId(2L)
        .setName("Брингли");
}
