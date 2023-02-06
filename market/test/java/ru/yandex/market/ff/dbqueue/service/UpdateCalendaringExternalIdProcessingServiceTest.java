package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.UpdateCalendaringExternalIdPayload;
import ru.yandex.market.logistics.calendaring.client.dto.UpdateExternalIdRequest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.verify;

public class UpdateCalendaringExternalIdProcessingServiceTest extends IntegrationTest {

    @Autowired
    private UpdateCalendaringExternalIdProcessingService service;

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-calendaring-external-id/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-calendaring-external-id/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateCalendaringExternalIdSuccess() {
        service.processPayload(new UpdateCalendaringExternalIdPayload(
                200,
                0,
                1
        ));
        verify(csClient).updateExternalId(new UpdateExternalIdRequest(
                200L,
                "0",
                "FFWF-test",
                "1"
        ));
    }
}
