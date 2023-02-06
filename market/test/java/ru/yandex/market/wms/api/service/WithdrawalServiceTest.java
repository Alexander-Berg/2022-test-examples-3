package ru.yandex.market.wms.api.service;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.api.service.async.AnomalyWithdrawalService;
import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class WithdrawalServiceTest extends IntegrationTest {

    @Autowired
    private AnomalyWithdrawalService anomalyWithdrawalService;

    @Test
    @DatabaseSetup("/anomaly-withdrawal/3/before.xml")
    @ExpectedDatabase(value = "/anomaly-withdrawal/3/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void cancelPreparedWithdrawal() {
        String orderKey = "0000000001";
        anomalyWithdrawalService.cancel(Collections.singleton(orderKey));
    }
}
