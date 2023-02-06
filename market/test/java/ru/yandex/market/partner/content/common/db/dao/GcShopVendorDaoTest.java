package ru.yandex.market.partner.content.common.db.dao;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.GcShopVendorDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcShopVendor;

public class GcShopVendorDaoTest extends BaseDbCommonTest {
    @Autowired
    private GcShopVendorDao gcShopVendorDao;

    @Test
    public void vendorNameUniqueTest() {
        gcShopVendorDao.insert(new GcShopVendor(
            null, //id,
            1, //shopId,
            "test vendor", //vendorName,
            null //marketVendorId
        ));
        gcShopVendorDao.insert(new GcShopVendor(
            null, //id,
            1, //shopId,
            "test vendor 2", //vendorName,
            null //marketVendorId
        ));

        Assertions.assertThatThrownBy(() ->
            gcShopVendorDao.insert(new GcShopVendor(
                null, //id,
                1, //shopId,
                "test vendor", //vendorName,
                null //marketVendorId
            ))
        ).hasMessageContaining("Key (shop_id, vendor_name)=(1, test vendor) already exists");
    }

    @Test
    public void marketVendorIdUniqueTest() {
        gcShopVendorDao.insert(new GcShopVendor(
            null, //id,
            1, //shopId,
            "test vendor 3", //vendorName,
            1L //marketVendorId
        ));
        Assertions.assertThatThrownBy(() ->
            gcShopVendorDao.insert(new GcShopVendor(
                null, //id,
                1, //shopId,
                "test vendor 4", //vendorName,
                1L //marketVendorId
            ))
        ).hasMessageContaining("Key (shop_id, market_vendor_id)=(1, 1) already exists");
    }
}
