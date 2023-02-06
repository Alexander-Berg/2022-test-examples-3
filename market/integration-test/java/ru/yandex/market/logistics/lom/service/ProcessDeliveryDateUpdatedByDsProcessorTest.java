package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessDeliveryDateUpdatedByDsProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_NULL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class ProcessDeliveryDateUpdatedByDsProcessorTest extends AbstractContextualTest {

    private static final Instant FIXED_TIME = Instant.parse("2021-03-01T10:00:00Z");

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private ProcessDeliveryDateUpdatedByDsProcessor processDeliveryDateUpdatedByDsProcessor;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient, lmsClient);
    }

    @Test
    @DisplayName("Процессинг обновления даты от СД")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/process_delivery_date_updated_by_ds/after/updated_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getDeliveryDate() throws Exception {
        processDeliveryDateUpdatedByDsProcessor.processPayload(
            createChangeOrderRequestPayload(
                1,
                "45",
                1
            )
        );

        verify(deliveryClient).getOrdersDeliveryDateAsync(
            eq(getResourceIds("O1", "DELIVERY-2-O1")),
            eq(new Partner(20L)),
            any()
        );

        sendDates("service/process_delivery_date_updated_by_ds/response/delivery_dates.json")
            .andExpect(status().isOk());

        // Второй запрос для проверки игнорирования повторных колбэков
        sendDates("service/process_delivery_date_updated_by_ds/response/delivery_dates.json")
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Процессинг обновления даты от СД, 2 запроса")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/process_delivery_date_updated_by_ds/after/updated_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getDeliveryDateDuplicate() throws Exception {
        processDeliveryDateUpdatedByDsProcessor.processPayload(
            createChangeOrderRequestPayload(
                1,
                "11",
                1
            )
        );

        verify(deliveryClient).getOrdersDeliveryDateAsync(
            eq(getResourceIds("O1", "DELIVERY-2-O1")),
            eq(new Partner(20L)),
            any()
        );

        sendDates("service/process_delivery_date_updated_by_ds/response/delivery_dates.json")
            .andExpect(status().isOk());

        processDeliveryDateUpdatedByDsProcessor.processPayload(
            createChangeOrderRequestPayload(
                1,
                "111",
                2
            )
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Процессинг обновления даты от СД (последняя миля)")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/process_delivery_date_updated_by_ds/after/updated_order_last_mile.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getDeliveryDateLastMile() throws Exception {
        doThrow(new HttpTemplateException(404, "")).when(lmsClient).getPartnerExternalParam(
            20L,
            PartnerExternalParamType.LAST_MILE_RECIPIENT_DEADLINE
        );
        processDeliveryDateUpdatedByDsProcessor.processPayload(
            createChangeOrderRequestPayload(
                2,
                "45",
                1
            )
        );

        verify(deliveryClient).getOrdersDeliveryDateAsync(
            eq(getResourceIds("O2", "DELIVERY-2-O2")),
            eq(new Partner(20L)),
            any()
        );

        sendDates("service/process_delivery_date_updated_by_ds/response/delivery_dates_last_mile.json")
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @Test
    @DisplayName("Процессинг обновления даты от СД, нет интервала времени доставки")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/process_delivery_date_updated_by_ds/after/updated_order_without_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getDeliveryDateWithoutInterval() throws Exception {
        processDeliveryDateUpdatedByDsProcessor.processPayload(
            createChangeOrderRequestPayload(
                1,
                "45",
                1
            )
        );

        verify(deliveryClient).getOrdersDeliveryDateAsync(
            eq(getResourceIds("O1", "DELIVERY-2-O1")),
            eq(new Partner(20L)),
            any()
        );

        sendDates("service/process_delivery_date_updated_by_ds/response/delivery_dates_without_interval.json")
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Процессинг обновления даты от СД, невалидный ответ от СД")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/process_delivery_date_updated_by_ds/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getDeliveryDateInvalid() throws Exception {
        processDeliveryDateUpdatedByDsProcessor.processPayload(
            createChangeOrderRequestPayload(
                1,
                "45",
                1
            )
        );

        verify(deliveryClient).getOrdersDeliveryDateAsync(
            eq(getResourceIds("O1", "DELIVERY-2-O1")),
            eq(new Partner(20L)),
            any()
        );

        sendDates("service/process_delivery_date_updated_by_ds/response/delivery_dates_invalid.json")
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationErrorMatcher("orderDeliveryDates[0].deliveryDate", NOT_NULL_ERROR_MESSAGE));

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @Test
    @DisplayName("Процессинг обновления даты от СД, дата в прошлом")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/process_delivery_date_updated_by_ds/after/delivery_date_in_past.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getDeliveryDateInPast() throws Exception {
        processDeliveryDateUpdatedByDsProcessor.processPayload(
            createChangeOrderRequestPayload(
                2,
                "12",
                1
            )
        );

        verify(deliveryClient).getOrdersDeliveryDateAsync(
            eq(getResourceIds("O2", "DELIVERY-2-O2")),
            eq(new Partner(20L)),
            any()
        );

        sendDates("service/process_delivery_date_updated_by_ds/response/delivery_dates_in_past.json")
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Поддерживается получение даты только по одному заказу")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void invalidOrdersCount(String requestPath) throws Exception {
        sendDates(requestPath)
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Expecting exactly one delivery date, process state: 101"));
    }

    private static Stream<Arguments> invalidOrdersCount() {
        return Stream.of(
            Arguments.of("service/process_delivery_date_updated_by_ds/response/delivery_dates_multiple.json"),
            Arguments.of("service/process_delivery_date_updated_by_ds/response/delivery_dates_empty.json")
        );
    }

    @Nonnull
    private List<ResourceId> getResourceIds(String barcode, String partnerExternalId) {
        return List.of(
            ResourceId.builder()
                .setYandexId(barcode)
                .setPartnerId(partnerExternalId)
                .build()
        );
    }

    @Nonnull
    private ResultActions sendDates(String requestPath) throws Exception {
        return mockMvc.perform(
            put("/orders/ds/getDeliveryDatesSuccess")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

}
