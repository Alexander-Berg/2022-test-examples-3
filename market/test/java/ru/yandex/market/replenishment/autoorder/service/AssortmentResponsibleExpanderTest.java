package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.AssortmentResponsibleExpander;
public class AssortmentResponsibleExpanderTest extends FunctionalTest {

    @Autowired
    AssortmentResponsibleExpander assortmentResponsibleExpander;

    @Test
    @DbUnitDataSet(before = "AssortmentResponsibleExpanderTest.before.csv",
            after = "AssortmentResponsibleExpanderTest.after.csv")
    public void testExpanding() {
        assortmentResponsibleExpander.load();
    }
}
