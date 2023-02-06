package ru.yandex.market.vendors.analytics.core.dao.price;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.model.sales.prices.PriceSegmentBounds;

/**
 * Функциональные тесты для класса {@link PriceSegmentDAO}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "PostgresPriceSegmentDAOTest.before.csv")
public class PostgresPriceSegmentDaoTest extends FunctionalTest {

    @Autowired
    private PriceSegmentDAO priceSegmentDAO;

    @Test
    void getBrandNamesTest() {
        Map<Integer, PriceSegmentBounds> expected = new ImmutableMap.Builder<Integer, PriceSegmentBounds>()
                .put(1, new PriceSegmentBounds(0, 1100L))
                .put(2, new PriceSegmentBounds(1100L, 1200L))
                .put(3, new PriceSegmentBounds(1200L, 1300L))
                .put(4, new PriceSegmentBounds(1300L, 1400L))
                .put(5, new PriceSegmentBounds(1400L, 1500L))
                .put(6, new PriceSegmentBounds(1500L, 1600L))
                .put(7, new PriceSegmentBounds(1600L, 1700L))
                .put(8, new PriceSegmentBounds(1700L, null))
                .build();
        Map<Integer, PriceSegmentBounds> actual = priceSegmentDAO.getCategoryPriceSegments(2);
        Assertions.assertEquals(expected, actual);
    }

}
