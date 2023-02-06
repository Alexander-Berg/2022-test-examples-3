package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SalePromoPeriod;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SalePromoPeriodRepository;
public class SalePromoPeriodRepositoryTest extends FunctionalTest {
    @Autowired
    private SalePromoPeriodRepository salePromoPeriodRepository;

    @Test
    @DbUnitDataSet(before = "SalePromoPeriodRepositoryTest.truncate.before.csv",
            after = "SalePromoPeriodRepositoryTest.truncate.after.csv")
    public void testTruncate() {
        salePromoPeriodRepository.truncate();
    }

    @Test
    @DbUnitDataSet(after = "SalePromoPeriodRepositoryTest.insert.after.csv")
    public void testInsert() {
        SalePromoPeriod promo = new SalePromoPeriod(
                "123", 145L, LocalDate.of(2020, 1, 12),
                LocalDate.of(2020, 12, 31), "title of promo", false);
        salePromoPeriodRepository.insert(promo);
    }
}
