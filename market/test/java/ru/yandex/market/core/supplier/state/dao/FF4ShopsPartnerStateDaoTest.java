package ru.yandex.market.core.supplier.state.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

public class FF4ShopsPartnerStateDaoTest extends FunctionalTest {

    @Autowired
    private FF4ShopsPartnerStateDao stateDao;

    @Test
    @DbUnitDataSet(before = "FF4ShopsPartnerStateDaoTest.before.csv")
    void sqlCheck() {
        stateDao.partnerStates(
                1L,
                List.of(ParamCheckStatus.DONT_WANT),
                List.of(DeliveryServiceType.DROPSHIP),
                false,
                SeekSliceRequest.firstN(1)
        );
    }
}
