package ru.yandex.market.api.cpa.yam.service.impl;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;


@DbUnitDataSet(before = "PrepayRequestContractHelperServiceTest.before.csv")
public class PrepayRequestContractHelperServiceTest extends FunctionalTest {

    @Autowired
    private PrepayRequestContractHelperService prepayRequestContractHelperService;

    @Test
    @DbUnitDataSet(after = "PrepayRequestContractHelperServiceTest.insertNew.after.csv")
    void insertNewBalanceDataTest() {
        PrepayRequest prepayRequest = new PrepayRequest(20, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED,
                10, 100L, "+7(999)999-99-99");
        prepayRequestContractHelperService.updateContractInfo(Collections.singletonList(prepayRequest));
    }

    @Test
    @DbUnitDataSet(after = "PrepayRequestContractHelperServiceTest.deleteNull.after.csv")
    void deleteNullBalanceDataTest() {
        PrepayRequest prepayRequest = new PrepayRequest(10, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED,
                10, null, "+7(999)999-99-99");
        prepayRequestContractHelperService.updateContractInfo(Collections.singletonList(prepayRequest));
    }

    @Test
    @DbUnitDataSet(after = "PrepayRequestContractHelperServiceTest.update.after.csv")
    void updateBalanceDataTest() {
        PrepayRequest prepayRequest = new PrepayRequest(10, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED,
                10, 200L, "+7(999)999-99-99");
        prepayRequestContractHelperService.updateContractInfo(Collections.singletonList(prepayRequest));
    }
}
