package ru.yandex.market.promoboss.dao;

import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(before = "AbstractPromoTest.before.csv")
public abstract class AbstractPromoTest extends AbstractDaoTest {
    protected static final Long PROMO_ID_1 = 1L;
    protected static final Long PROMO_ID_2 = 2L;
}
