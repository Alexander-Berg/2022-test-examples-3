package ru.yandex.market.core.supplier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Функциональные тесты для {@link SupplierDao}.
 *
 * @author avetokhin 01/03/18.
 */
@DbUnitDataSet(before = "SupplierDao.before.csv")
class SupplierDaoTest extends FunctionalTest {

    @Autowired
    private SupplierDao supplierDao;

    @Test
    void getSuppliersByIds() {
        final Map<Long, SupplierInfo> suppliers = supplierDao.getSuppliersByIds(Arrays.asList(1L, 2L, 5L));
        assertThat(suppliers, notNullValue());
        assertThat(suppliers.size() , equalTo(2));

        assertThat(suppliers.get(1L).getId(), equalTo(1L));
        assertThat(suppliers.get(1L).getName(), equalTo("supplier"));

        assertThat(suppliers.get(2L).getId(), equalTo(2L));
        assertThat(suppliers.get(2L).getName(), equalTo("supplier_2"));
    }

    @Test
    void test_getAllSuppliers() {
        Set<Long> actualResult = supplierDao.getAllSupplierIds();

        assertThat(actualResult.size(), equalTo(4));

        assertThat(actualResult,
                containsInAnyOrder(1L, 2L, 3L, 4L)
        );
    }

    @Test
    void test_getSupplierCreatedAt() {
        Instant createdAt = supplierDao.getSupplierCreatedAt(1L);
        assertThat(createdAt,
                equalTo(LocalDateTime.of(2018, 1, 1, 0, 0)
                        .atZone(ZoneId.systemDefault()).toInstant()));
    }
}
