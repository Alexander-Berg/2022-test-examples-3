package ru.yandex.market.billing.factoring.dao;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;
import ru.yandex.market.billing.factoring.model.PayoutSchedule;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тест для {@link PayoutScheduleDao}
 */
class PayoutScheduleDaoTest extends FunctionalTest {

    @Autowired
    public PayoutScheduleDao payoutScheduleDao;

    @DisplayName("Получение частот выплат")
    @Test
    @DbUnitDataSet(before = "PayoutScheduleDaoTest.testGetPayoutSchedules.csv")
    public void testGetPayoutSchedules() {
        List<PayoutSchedule> payoutSchedules = payoutScheduleDao.getPayoutSchedules();

        assertEquals(3, payoutSchedules.size());

        PayoutSchedule payoutSchedule = payoutScheduleDao.getPayoutSchedule(PayoutFrequency.DAILY);

        assertNotNull(payoutSchedule);
        assertEquals(PayoutFrequency.DAILY, payoutSchedule.getFrequency());
    }
}
