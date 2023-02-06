package ru.yandex.market.wms.radiator.test;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;

public interface TestData {

    long VENDOR_ID = 100L;

    String M_SKU_100_01 = "M_SKU_100_01";
    String M_SKU_100_02 = "M_SKU_100_02";
    String M_SKU_100_03 = "M_SKU_100_03";
    String M_SKU_100_04 = "M_SKU_100_04";

    String PACK101 = "P_101";
    String PACK102 = "P_102";
    String PACK103 = "P_103";
    String PACK104 = "P_104";

    static UnitId unitId(String mSku) {
        return unitId(mSku, VENDOR_ID);
    }

    static UnitId unitId(String mSku, long vendorId) {
        return new UnitId(mSku, vendorId, mSku);
    }
}
