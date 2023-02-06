package ru.yandex.market.vendors.analytics.core.dao.brand;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;

/**
 * Функциональные тесты для класса {@link BrandDao}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "PostgresBrandDAOTest.before.csv")
public class PostgresBrandDaoTest extends FunctionalTest {

    @Autowired
    private BrandDao brandDAO;

    void getBrandNamesTest() {
        Map<Long, String> expected = ImmutableMap.of(
                3L, "Груша",
                4L, "GMC"
        );
        Map<Long, String> actual = brandDAO.getBrandNames(ImmutableList.of(3L, 4L));
        Assertions.assertEquals(expected, actual);
    }

}
