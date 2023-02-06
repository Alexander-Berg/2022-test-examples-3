package ru.yandex.market.replenishment.autoorder.repository;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.TenderPrice;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SkuPriceRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SkuPriceRepositoryTest extends FunctionalTest {

    private static final double EPS = 1E-3;

    @Autowired
    private SkuPriceRepository skuPriceRepository;

    @Test
    @DbUnitDataSet(before = "SkuPriceRepositoryNewTest.getTenderPrices.before.csv")
    public void getTenderPrices() {
        final Map<Long, TenderPrice> result = skuPriceRepository.getTenderPrices(Set.of(1L, 2L, 3L, 4L, 5L));
        assertEquals(4, result.size());

        final TenderPrice tenderPrice1 = result.get(1L);
        assertEquals(10., tenderPrice1.getSalePrice(), EPS);
        assertEquals(5., tenderPrice1.getPurchaseResultPrice(), EPS);
        assertEquals(11., tenderPrice1.getCompetitorPrice(), EPS);

        final TenderPrice tenderPrice2 = result.get(2L);
        assertEquals(30., tenderPrice2.getSalePrice(), EPS);
        assertEquals(25., tenderPrice2.getPurchaseResultPrice(), EPS);
        assertEquals(12., tenderPrice2.getCompetitorPrice(), EPS);

        final TenderPrice tenderPrice4 = result.get(4L);
        assertEquals(50., tenderPrice4.getSalePrice(), EPS);
        assertEquals(45., tenderPrice4.getPurchaseResultPrice(), EPS);
        assertNull(tenderPrice4.getCompetitorPrice());

        final TenderPrice tenderPrice5 = result.get(5L);
        assertNull(tenderPrice5.getSalePrice());
        assertNull(tenderPrice5.getPurchaseResultPrice());
        assertNull(tenderPrice5.getCompetitorPrice());
    }

}
