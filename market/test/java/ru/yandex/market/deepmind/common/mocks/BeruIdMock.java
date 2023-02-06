package ru.yandex.market.deepmind.common.mocks;

import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;

/**
 * @author dmserebr
 * @date 08/05/2020
 */
public class BeruIdMock extends BeruId {
    public static final int DEFAULT_PROD_FP_ID = 465852;
    public static final int DEFAULT_PROD_BIZ_ID = 924574;

    public BeruIdMock() {
        super(DEFAULT_PROD_FP_ID, DEFAULT_PROD_BIZ_ID);
    }

    public BeruIdMock(int id) {
        super(id, DEFAULT_PROD_BIZ_ID);
    }

    public BeruIdMock(int id, int businessId) {
        super(id, businessId);
    }
}
