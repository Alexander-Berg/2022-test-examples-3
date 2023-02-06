/*
 * (C) 2017 Yandex Market LLC
 */
package ru.yandex.market.billing.price;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
class PapiOfferPriceExportServiceRemoveOutdatedTest extends FunctionalTest {

    @Autowired
    private PapiOfferPriceExportService papiOfferPriceExportService;

    @Test
    @DbUnitDataSet(
            before = "PapiOfferPriceExportServiceRemoveOutdatedTest.before.csv",
            after = "PapiOfferPriceExportServiceRemoveOutdatedTest.after.csv")
    void test() {
        papiOfferPriceExportService.removeOutdatedExports(
                OffsetDateTime.parse("2017-12-01T16:00:00+03:00").toInstant(), true
        );
    }
}
