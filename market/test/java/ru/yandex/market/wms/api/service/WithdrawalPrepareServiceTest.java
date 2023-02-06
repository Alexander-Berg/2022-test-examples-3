package ru.yandex.market.wms.api.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WithdrawalPrepareServiceTest extends IntegrationTest {

    @Autowired
    private AnomalyWithdrawalPrepareService anomalyWithdrawalPrepareService;

    @Test
    @DatabaseSetup("/anomaly-withdrawal/1/before.xml")
    @ExpectedDatabase(value = "/anomaly-withdrawal/1/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void prepareWithdrawal() {
        String orderKey = "0000000001";
        anomalyWithdrawalPrepareService.prepareWithdrawal(orderKey);
    }

    @Test
    @DatabaseSetup("/anomaly-withdrawal/2/before.xml")
    @ExpectedDatabase(value = "/anomaly-withdrawal/2/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void prepareWithdrawalNoAnomalyLot() {
        String orderKey = "0000000001";
        anomalyWithdrawalPrepareService.prepareWithdrawal(orderKey);
    }

    @Test
    @DatabaseSetup("/anomaly-withdrawal/2/before.xml")
    @ExpectedDatabase(value = "/anomaly-withdrawal/2/before.xml", assertionMode = NON_STRICT_UNORDERED)
    void prepareWithdrawalWrongOrderKey() {
        String orderKey = "0000000111";

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> anomalyWithdrawalPrepareService.prepareWithdrawal(orderKey));

        String expectedMessage = "Order %s not found".formatted(orderKey);
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
