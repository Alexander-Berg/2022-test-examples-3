package ru.yandex.market.promoboss.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.model.PromoEvent;

public class PromoEventDaoTest extends AbstractPromoRepositoryTest {

    private static final Long PROMO_ID_1 = 1L;

    @Autowired
    private PromoEventDao promoEventDao;

    @Test
    @DbUnitDataSet(
            after = "PromoEventDaoTest.insertTest.after.csv"
    )
    void insertTest() {
        promoEventDao.save(PromoEvent.create(PROMO_ID_1, 123L));
    }

    @Test
    @DbUnitDataSet(
            before = "PromoEventDaoTest.updateTest.before.csv",
            after = "PromoEventDaoTest.updateTest.after.csv"
    )
    void updateTest() {
        promoEventDao.save(PromoEvent.update(PROMO_ID_1, 124L));
    }
}
