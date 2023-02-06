package ru.yandex.market.core.delivery.tariff.db;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.delivery.failure.RegionGroupFailureReason;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.RegionGroupPaymentType;
import ru.yandex.market.core.delivery.tariff.db.dao.RegionGroupStatusDao;
import ru.yandex.market.core.delivery.tariff.model.RegionGroupStatus;
import ru.yandex.market.core.param.model.ParamCheckStatus;

/**
 * Тесты для {@link RegionGroupStatusDao}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RegionGroupStatusDaoTest extends FunctionalTest {

    @Autowired
    private RegionGroupStatusDao regionGroupStatusDao;

    @Test
    @DisplayName("Получение статуса без причин отключения")
    @DbUnitDataSet(before = "csv/RegionGroupStatusDao.get.before.csv")
    void testGetWithoutReasons() {
        final List<RegionGroupStatus> statuses = regionGroupStatusDao.getStatuses(Collections.singletonList(1001L));

        final Date updatedAt = Date.from(LocalDate.of(2018, 3, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        final RegionGroupStatus expected = new RegionGroupStatus(1001L, ParamCheckStatus.NEW, updatedAt, Collections.emptySet(), Collections.emptySet(), null);
        Assertions.assertEquals(1, statuses.size());
        Assertions.assertEquals(expected, statuses.get(0));
    }

    @Test
    @DisplayName("Получение статуса, у которого есть причины отключения")
    @DbUnitDataSet(before = "csv/RegionGroupStatusDao.get_with_reasons.before.csv")
    void testGetWithReasons() {
        final List<RegionGroupStatus> statuses = regionGroupStatusDao.getStatuses(Collections.singletonList(1001L));

        final Date updatedAt = Date.from(LocalDate.of(2018, 3, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        final RegionGroupStatus expected = new RegionGroupStatus(1001L, ParamCheckStatus.NEW, updatedAt,
                ImmutableSet.of(RegionGroupFailureReason.NO_DELIVERY, RegionGroupFailureReason.INVALID_DELIVERY_COST),
                ImmutableSet.of(RegionGroupPaymentType.COURIER_CARD, RegionGroupPaymentType.COURIER_CASH),
                "assessor msg");
        Assertions.assertEquals(1, statuses.size());
        Assertions.assertEquals(expected, statuses.get(0));
    }

    @Test
    @DisplayName("Создание статуса группы регионов")
    @DbUnitDataSet(before = "csv/RegionGroupStatusDao.create.before.csv", after = "csv/RegionGroupStatusDao.create.after.csv")
    void testCreate() {
        final Date updatedAt = Date.from(LocalDate.of(2018, 3, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        regionGroupStatusDao.createStatus(new RegionGroupStatus(1001L, ParamCheckStatus.NEW, updatedAt,
                ImmutableSet.of(RegionGroupFailureReason.NO_DELIVERY),
                ImmutableSet.of(RegionGroupPaymentType.COURIER_CASH),
                "assessor msg"));
    }

    @Test
    @DisplayName("Удаление статуса")
    @DbUnitDataSet(before = "csv/RegionGroupStatusDao.delete.before.csv", after = "csv/RegionGroupStatusDao.delete.after.csv")
    void testDelete() {
        regionGroupStatusDao.deleteStatus(1001L);
    }

}
