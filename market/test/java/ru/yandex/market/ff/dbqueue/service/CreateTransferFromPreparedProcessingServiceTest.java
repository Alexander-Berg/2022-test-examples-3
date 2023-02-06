package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.dbqueue.exceptions.CreateTransferForPreparedForNotTransferException;
import ru.yandex.market.ff.model.dbqueue.CreateTransferFromPreparedPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateTransferFromPreparedProcessingServiceTest extends IntegrationTest {

    @Autowired
    private CreateTransferFromPreparedProcessingService createTransferFromPreparedProcessingService;

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-transfer-from-prepared/before-not-transfer.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-transfer-from-prepared/before-not-transfer.xml",
            assertionMode = NON_STRICT)
    public void processForNotTransfer() {
        assertThrows(CreateTransferForPreparedForNotTransferException.class, () ->
                createTransferFromPreparedProcessingService.processPayload(new CreateTransferFromPreparedPayload(1)));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-transfer-from-prepared/before-incorrect-status.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-transfer-from-prepared/before-incorrect-status.xml",
            assertionMode = NON_STRICT)
    public void processInNotPreparedForCreationStatus() {
        createTransferFromPreparedProcessingService.processPayload(new CreateTransferFromPreparedPayload(1));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-transfer-from-prepared/before-correct.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/create-transfer-from-prepared/after-correct.xml",
            assertionMode = NON_STRICT)
    public void processForCorrectTransfer() {
        createTransferFromPreparedProcessingService.processPayload(new CreateTransferFromPreparedPayload(1));
    }
}
