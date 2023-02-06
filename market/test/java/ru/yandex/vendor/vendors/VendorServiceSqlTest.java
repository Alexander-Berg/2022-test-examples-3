package ru.yandex.vendor.vendors;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class VendorServiceSqlTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    VendorServiceSql sql;

    @Test
    void selectVendors() {
        var result = sql.selectVendors(null, null, null, null, null, Set.of(100L));
        assertThat(result).isEmpty();
    }
}
