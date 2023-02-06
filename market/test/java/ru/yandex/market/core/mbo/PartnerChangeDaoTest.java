package ru.yandex.market.core.mbo;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.mbo.model.PartnerChangeRecord;

public class PartnerChangeDaoTest extends FunctionalTest {
    @Autowired
    private PartnerChangeDao partnerChangeDao;

    @Test
    @DbUnitDataSet(before = "PartnerChangeDaoTest.before.csv")
    void getEventForPartnerTest() {
        partnerChangeDao.getEventForPartner(
                Set.of(100L, 101L, 102L, 103L),
                PartnerChangeRecord.UpdateType.UNITED_CATALOG
        );
    }
}
