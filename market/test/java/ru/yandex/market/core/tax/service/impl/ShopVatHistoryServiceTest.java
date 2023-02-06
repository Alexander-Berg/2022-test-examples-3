package ru.yandex.market.core.tax.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.tax.model.ShopVat;
import ru.yandex.market.core.tax.service.ShopVatHistoryService;

import static java.util.Collections.singletonList;

public class ShopVatHistoryServiceTest extends FunctionalTest {

    @Autowired
    private ShopVatHistoryService tested;

    @Test
    @DbUnitDataSet(before = "ShopVatHistoryServiceTest.before.csv", after = "ShopVatHistoryServiceTest.after.csv")
    void testVatHistory() {
        ShopVat vat = new ShopVat();

        tested.logCreatedVats(1L, singletonList(vat));
        tested.logUpdatedVats(2L, singletonList(vat));
    }
}
