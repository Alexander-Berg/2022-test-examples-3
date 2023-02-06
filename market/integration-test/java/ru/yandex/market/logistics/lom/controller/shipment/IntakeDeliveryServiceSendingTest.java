package ru.yandex.market.logistics.lom.controller.shipment;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Intake;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.consumer.DeliveryServiceShipmentConsumer;
import ru.yandex.market.logistics.lom.jobs.exception.DbQueueJobExecutionException;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.model.ShipmentApplicationIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.DeliveryServiceShipmentProcessingService;
import ru.yandex.market.logistics.lom.service.AbstractExternalServiceTest;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createShipmentApplicationIdPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты отправки заявки на забор в службу доставки")
@DatabaseSetup("/service/excludedshipmentrelation/business_process_state.xml")
class IntakeDeliveryServiceSendingTest extends AbstractExternalServiceTest {

    private static final ShipmentApplicationIdPayload DELIVERY_SERVICE_SHIPMENT_PAYLOAD =
        createShipmentApplicationIdPayload(1L, "123");

    @Autowired
    private DeliveryServiceShipmentConsumer deliveryServiceShipmentConsumer;

    @Autowired
    private DeliveryServiceShipmentProcessingService deliveryServiceShipmentProcessingService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private DeliveryClient deliveryClient;

    @BeforeEach
    void setup() throws Exception {

        clock.setFixed(Instant.parse("2019-05-23T21:00:00.00Z"), MOSCOW_ZONE);

        when(lmsClient.getLogisticsPoint(1L))
            .thenReturn(Optional.of(
                LmsFactory.createLogisticsPointResponse(1L, 2L, "point", PointType.WAREHOUSE).build()
            ));

        when(lmsClient.getPartner(3L))
            .thenReturn(Optional.of(LmsFactory.createPartnerResponse(3, 50L)));

        mockMvc.perform(
                post("/shipments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/shipment/request/create_without_courier_shipment.json"))
            )
            .andExpect(status().isOk());
    }

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/shipment/after/shipment_sent_to_delivery_service.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Успешная отправка заявки на забор в службу")
    void sendingIntakeTest() throws Exception {
        assertThat(deliveryServiceShipmentConsumer.execute(createTask(1)))
            .isEqualTo((TaskExecutionResult.finish()));

        verify(deliveryClient).createIntake(
            eq(createIntake()),
            eq(new Partner(3L)),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/shipment/after/shipment_fail_sent_to_delivery_service.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Ошибка после 8 попыток отправки в LGW")
    void sendingIntakeTestNotFoundLogisticsPointFail() {
        when(lmsClient.getLogisticsPoint(1L))
            .thenReturn(Optional.empty());

        assertThat(deliveryServiceShipmentConsumer.execute(createTask(1)))
            .isEqualTo((TaskExecutionResult.fail()));

        assertThat(deliveryServiceShipmentConsumer.execute(createTask(8)))
            .isEqualTo((TaskExecutionResult.finish()));
    }

    @Test
    @DisplayName("Не найден склад")
    void sendingIntakeTestNotFoundLogisticsPoint() {
        when(lmsClient.getLogisticsPoint(1L))
            .thenReturn(Optional.empty());

        softly.assertThatThrownBy(
                () -> deliveryServiceShipmentProcessingService.processPayload(DELIVERY_SERVICE_SHIPMENT_PAYLOAD)
            )
            .isInstanceOf(DbQueueJobExecutionException.class)
            .hasCauseInstanceOf(ResourceNotFoundException.class)
            .matches(
                throwable -> "Failed to find [LOGISTICS_POINT] with id [1]".equals(throwable.getCause().getMessage())
            );
    }

    @Test
    @DisplayName("Ошибка клиента")
    void sendingIntakeTestClientError() throws GatewayApiException {
        doThrow(new GatewayApiException("Exception")).when(deliveryClient)
            .createIntake(any(), any(), any());

        softly.assertThatThrownBy(
                () -> deliveryServiceShipmentProcessingService.processPayload(DELIVERY_SERVICE_SHIPMENT_PAYLOAD)
            )
            .isInstanceOf(DbQueueJobExecutionException.class)
            .hasCauseInstanceOf(GatewayApiException.class);

        verify(deliveryClient).createIntake(eq(createIntake()), eq(new Partner(3L)), eq(EXPECTED_CLIENT_REQUEST_META));
    }

    @Nonnull
    private Intake createIntake() {
        return new Intake(
            ResourceId.builder().setYandexId("1").build(),
            new Warehouse(
                ResourceId.builder().setYandexId("1").build(),
                new Location.LocationBuilder("Россия", "Новосибирск", "Регион")
                    .setHousing("11")
                    .setHouse("11")
                    .setZipCode("649220")
                    .setLocationId(1)
                    .setStreet("Николаева")
                    .setBuilding("")
                    .setRoom("")
                    .setSubRegion("Округ")
                    .build(),
                null,
                List.of(new WorkTime(1, List.of(TimeInterval.of(
                    LocalTime.of(10, 0),
                    LocalTime.of(18, 0)
                )))),
                new Person.PersonBuilder("Иван", "Иванов")
                    .setPatronymic("Иванович")
                    .build(),
                List.of(new Phone("+79232435555", "777"))
            ),
            new DateTimeInterval(
                OffsetDateTime.of(2019, 5, 25, 12, 0, 0, 0, ZoneOffset.of("+03:00")),
                OffsetDateTime.of(2019, 5, 25, 14, 0, 0, 0, ZoneOffset.of("+03:00"))
            ),
            0.1F,
            0.5F,
            "test-comment"
        );
    }

    @Nonnull
    private Task<ShipmentApplicationIdPayload> createTask(int attemptsCount) {
        return TaskFactory.createTask(
            QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
            DELIVERY_SERVICE_SHIPMENT_PAYLOAD,
            attemptsCount
        );
    }
}
