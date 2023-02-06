package ru.yandex.market.admin.service.dao;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.SerializableID;
import ru.yandex.market.admin.ui.model.StringID;
import ru.yandex.market.admin.ui.model.supplier.UISupplierSearch;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тесты для {@link SupplierUIDao}.
 */
class SupplierUIDaoTest extends FunctionalTest {

    @Autowired
    private SupplierUIDao supplierUIDao;

    public static final SerializableID ID = new StringID("ID");
    public static final SerializableID NAME = new StringID("NAME");
    public static final SerializableID BUSINESS_ID = new StringID("BUSINESS_ID");

    @Test
    @DisplayName("Проверка поиска")
    @DbUnitDataSet(before = "SupplierUIDaoTest.before.csv")
    void searchSupplier() {
        final List<UISupplierSearch> suppliers = supplierUIDao.searchSupplier("1", new SortingInfo<>("name",
                SortingOrder.ASC), 0, 10);
        assertThat(suppliers, notNullValue());
        assertThat(suppliers, hasSize(2));

        assertThat(suppliers.get(0).getLongField(ID), equalTo(2L));
        assertThat(suppliers.get(0).getStringField(NAME), equalTo("11may"));

        assertThat(suppliers.get(1).getLongField(ID), equalTo(1L));
        assertThat(suppliers.get(1).getStringField(NAME), equalTo("supplier1"));
    }

    @Test
    @DisplayName("Поиск работает для поставщиков без бизнеса (удаленные)")
    @DbUnitDataSet(before = "SupplierUIDaoTest.deleted.before.csv")
    void searchDeletedSupplier() {
        final List<UISupplierSearch> suppliers = supplierUIDao.searchSupplier("11", new SortingInfo<>("name",
                SortingOrder.ASC), 0, 10);
        assertThat(suppliers, notNullValue());
        assertThat(suppliers, hasSize(1));

        assertThat(suppliers.get(0).getLongField(ID), equalTo(11L));
        assertThat(suppliers.get(0).getStringField(NAME), equalTo("supplier1"));
        assertNull(suppliers.get(0).getLongField(BUSINESS_ID));
    }
}
