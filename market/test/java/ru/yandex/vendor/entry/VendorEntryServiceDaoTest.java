package ru.yandex.vendor.entry;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.entry.model.EntryVendorStatus;
import ru.yandex.vendor.entry.model.VendorEntryFilter;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/EntryControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class VendorEntryServiceDaoTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    VendorEntryServiceDao dao;

    @Test
    void getVendorEntriesCount() {
        var filter = new VendorEntryFilter();
        filter.setTextForSearch("РОАР");
        filter.setStatus(List.of(EntryVendorStatus.NEW));
        var result = dao.getVendorEntriesCount(filter);
        assertThat(result).isEqualTo(2);
    }
}
