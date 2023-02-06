package ru.yandex.market.core.supplier.banner.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.banner.model.filter.SupplierBannerFilter;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierBannerDaoTest extends FunctionalTest {
    @Autowired
    SupplierBannerDao supplierBannerDao;
    @Test
    void fetchTargetedSupplierIds() {
        var filter = new SupplierBannerFilter.Builder().setId("id").build();
        var ids = supplierBannerDao.fetchTargetedSupplierIds(filter);
        assertThat(ids).isEmpty();
    }
}
