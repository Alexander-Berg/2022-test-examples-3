package ru.yandex.market.ff.dbqueue.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CreatePlanRegistryPayload;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static org.mockito.Mockito.when;

public class CreatePlanRegistryProcessingServiceTest extends IntegrationTest {

    @Autowired
    private CreatePlanRegistryProcessingService createPlanRegistryProcessingService;

    @Test
    @Transactional
    @DatabaseSetup("classpath:db-queue/service/create-plan-registry-processing/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-plan-registry-processing/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void createTaskToPutInboundTest() {
        createPlanRegistryProcessingService.processPayload(new CreatePlanRegistryPayload(2L));
    }

    @Test
    @Transactional
    @DatabaseSetup("classpath:db-queue/service/create-plan-registry-put-outbound-processing/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-plan-registry-put-outbound-processing/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void createTaskToPutOutboundTest() {
        createPlanRegistryProcessingService.processPayload(new CreatePlanRegistryPayload(2L));
    }

    @Test
    @Transactional
    @DatabaseSetup("classpath:db-queue/service/create-plan-registry-put-outbound-processing/" +
            "before-with-different-suppliers.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-plan-registry-put-outbound-processing/" +
            "after-with-different-suppliers.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void createTaskToPutOutboundWithDifferentSuppliersTest() {
        createPlanRegistryProcessingService.processPayload(new CreatePlanRegistryPayload(2L));
    }

    @Test
    @Transactional
    @DatabaseSetup("classpath:db-queue/service/create-plan-registry-assortment/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-plan-registry-assortment/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void processPayloadWithIsNeedSort() {
        environmentParamService.setParam("supply-put-inbound-date-param",
                List.of(LocalDateTime.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        createPlanRegistryProcessingService.processPayload(new CreatePlanRegistryPayload(1L));
    }

    @Test
    @Transactional
    @DatabaseSetup("classpath:db-queue/service/create-plan-regostry-with-incorrect-status/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-plan-regostry-with-incorrect-status/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void processPayloadWithIncorrectStatus() {
        createPlanRegistryProcessingService.processPayload(new CreatePlanRegistryPayload(1L));
    }

    @Test
    @Transactional
    @DatabaseSetup("classpath:db-queue/service/create-plan-registry-with-update-slot/before.xml")
    void processPayloadWithUpdateSlot() {
        when(csClient.getSlotByExternalIdentifiers(Set.of("1"), "FFWF-test", null))
                .thenReturn(new BookingListResponse(List.of(
                        new BookingResponse(199, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 6, 8, 0, 0, 0, ZoneId.of("UTC")),
                                ZonedDateTime.of(2018, 1, 6, 8, 30, 0, 0, ZoneId.of("UTC")),
                                BookingStatus.UPDATING, null, 100L),
                        new BookingResponse(200, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 5, 7, 0, 0, 0, ZoneId.of("UTC")),
                                ZonedDateTime.of(2018, 1, 5, 7, 30, 0, 0, ZoneId.of("UTC")),
                                BookingStatus.ACTIVE, null, 100L))));

        Assertions.assertThrows(RuntimeException.class, () ->
                        createPlanRegistryProcessingService.processPayload(new CreatePlanRegistryPayload(1L)),
                "Could not create plan registry, " +
                        "because there is an unfinished update of a time slot. " +
                        "Will try again later...");
    }
}
