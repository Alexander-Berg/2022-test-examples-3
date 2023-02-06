package ru.yandex.market.ff.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.dbqueue.service.ValidateWithRegistryProcessingService;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.market.ff.util.CalendaringServiceUtils;

public class ValidateWithRegistryProcessingServiceTest extends IntegrationTest {

    @Autowired
    private ValidateWithRegistryProcessingService validateWithRegistryProcessingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup(value = "classpath:service/with-registry-validation/1/before.xml")
    @ExpectedDatabase(value = "classpath:service/with-registry-validation/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successValidationOfShadowWithdraw() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(5, csClient);
        transactionTemplate.execute(status -> {
            validateWithRegistryProcessingService.processPayload(new ValidateRequestPayload(1));
            return null;
        });
    }

    @Test
    @DatabaseSetup(value = "classpath:service/with-registry-validation/identifiers-must-be-created/before.xml")
    @ExpectedDatabase(value = "classpath:service/with-registry-validation/identifiers-must-be-created/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void identifiersMustBeCreated() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(5, csClient);
        transactionTemplate.execute(status -> {
            validateWithRegistryProcessingService.processPayload(new ValidateRequestPayload(1));
            return null;
        });
    }
}
