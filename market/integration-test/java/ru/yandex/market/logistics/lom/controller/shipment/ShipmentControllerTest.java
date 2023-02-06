package ru.yandex.market.logistics.lom.controller.shipment;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class ShipmentControllerTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    private static final String SUCCESS_RESPONSE = "{\"errors\":[]}";

    @BeforeEach
    void setup() {
        when(lmsClient.getPartner(3L))
            .thenReturn(Optional.of(LmsFactory.createPartnerResponse(3, 50L)));

        clock.setFixed(Instant.parse("2019-05-24T21:00:00.00Z"), MOSCOW_ZONE);
    }

    @Test
    @ExpectedDatabase(value = "/controller/shipment/after/create_shipment.xml", assertionMode = NON_STRICT)
    @DisplayName("Создание заявки на забор")
    void createShipmentSuccess() throws Exception {
        ShipmentTestUtil.createShipment(mockMvc, "create_shipment.json", status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.GET_ACCEPTANCE_CERTIFICATE,
            PayloadFactory.createRegistryIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @ExpectedDatabase(value = "/controller/shipment/after/shipment_own_delivery.xml", assertionMode = NON_STRICT)
    @DisplayName("Создание заявки в собственную СД")
    void createOwnDeliveryShipmentSuccess() throws Exception {
        when(lmsClient.getPartner(3L))
            .thenReturn(Optional.of(LmsFactory.createPartnerResponse(3, 50L, PartnerType.OWN_DELIVERY)));
        ShipmentTestUtil.createShipment(mockMvc, "create_own_delivery_shipment.json", status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.GET_ACCEPTANCE_CERTIFICATE,
            PayloadFactory.createRegistryIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/shipment/after/shipment_own_delivery_not_billable.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Создание заявки в собственную СД, не billable balanceContractId")
    void createOwnDeliveryShipmentNotBillableSuccess() throws Exception {
        when(lmsClient.getPartner(3L))
            .thenReturn(Optional.of(LmsFactory.createPartnerResponse(3, 50L, PartnerType.OWN_DELIVERY)));
        ShipmentTestUtil.createShipment(mockMvc, "create_own_delivery_shipment_not_billable.json", status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.GET_ACCEPTANCE_CERTIFICATE,
            PayloadFactory.createRegistryIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DatabaseSetup("/controller/shipment/before/create_shipment_that_already_exists.xml")
    @ExpectedDatabase(value = "/controller/shipment/after/create_shipment.xml", assertionMode = NON_STRICT)
    @DisplayName("Создание заявки на отгрузку, которая уже существует")
    void createShipmentThatAlreadyExists() throws Exception {
        ShipmentTestUtil.createShipment(mockMvc, "create_shipment.json", status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/shipment/before/shipment_withdraw_no_billing.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/after/create_withdraw_shipment.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Создание заявки на забор, для существующей отгрузки")
    void createWithdrawShipmentThatAlreadyExists() throws Exception {
        ShipmentTestUtil.createShipment(mockMvc, "create_withdraw_shipment.json", status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/shipment/before/shipment_withdraw_with_billing.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/after/shipment_withdraw_with_billing.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Создание заявки на забор, при существующей отмененной заявке")
    void createWithdrawShipmentThatAlreadyExistsWithBilling() throws Exception {
        ShipmentTestUtil.createShipment(mockMvc, "create_withdraw_shipment.json", status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/shipment/after/create_without_courier_shipment.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Создание заявки на забор без указания типа курьера")
    void createShipmentWithoutCourierSuccess() throws Exception {
        ShipmentTestUtil.createShipment(mockMvc, "create_without_courier_shipment.json", status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/shipment/before/create_shipment_application_that_already_exists.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/before/create_shipment_application_that_already_exists.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Создание заявки, которая уже существует")
    void createShipmentApplicationThatAlreadyExists() throws Exception {
        ShipmentTestUtil.createShipment(
            mockMvc,
            "create_shipment_application_that_already_exists.json",
            status().isBadRequest()
        );
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("level=ERROR\t" +
                "format=json-exception\t" +
                "code=SHIPMENT_APPLICATION_CREATION_ERROR\t" +
                "payload={\\\"eventMessage\\\":\\\"Cannot create shipment application " +
                "ShipmentApplicationDto(id=null, shipment=ShipmentDto(id=null, marketIdFrom=1, marketIdTo=2, " +
                "partnerIdTo=3, shipmentType=IMPORT, shipmentDate=2019-05-25, warehouseFrom=1, warehouseTo=2, " +
                "fake=null), requisiteId=1001, externalId=null, " +
                "interval=TimeIntervalDto(from=12:00, to=14:00, tzOffset=null), " +
                "status=null, korobyteDto=KorobyteDto(length=20, width=30, height=43, weightGross=0.5), " +
                "courier=CourierDto(type=CAR, contact=ContactDto(lastName=test-last-name-1, " +
                "firstName=test-first-name-1, middleName=null, phone=1234567890, extension=null, " +
                "personalFullnameId=null, personalPhoneId=null), " +
                "car=CarDto(number=A001BC23, brand=Renault)), cost=0, comment=test-comment, balanceContractId=1001, " +
                "balancePersonId=10001, locationZoneId=null)\\\",\\\"exceptionMessage\\\":" +
                "\\\"BadRequestException: Shipment application for this shipment already exists with id = 1\\\"");
    }

    @Test
    @DatabaseSetup("/controller/shipment/before/create_shipment_application_with_cancelled.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/after/create_shipment_application_with_cancelled.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Создание заявки на отгрузку, у которой уже есть отмененная заявка")
    void createShipmentApplicationWithCancelled() throws Exception {
        ShipmentTestUtil.createShipment(mockMvc, "create_shipment.json", status().isOk());
    }

    @Test
    @DisplayName("Создание заявки на следующий день после 21:00 успешно")
    void createShipmentApplicationAfterCutoff() throws Exception {

        clock.setFixed(Instant.parse("2019-05-25T18:05:00.00Z"), MOSCOW_ZONE);

        ShipmentTestUtil.createShipment(
            mockMvc,
            "create_withdraw_shipment.json",
            status().isOk()
        );
    }

    @Test
    @DisplayName("Создание заявки после 21:00 разрешено, если отгрузка минимум через день")
    void createShipmentApplicationAfterCutoffForDayAfterTomorrow() throws Exception {

        clock.setFixed(Instant.parse("2019-05-23T18:05:00.00Z"), MOSCOW_ZONE);

        ShipmentTestUtil.createShipment(
            mockMvc,
            "create_withdraw_shipment.json",
            status().isOk()
        );
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_valid_orders.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/after/confirm_shipment_registry_sent.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Подтверждение отгрузки - если заказ создан у партнера")
    void confirmShipmentApplication() {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.OK)
            .andExpect(content().string(SUCCESS_RESPONSE));
    }

    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_fake_shipment.xml")
    @ExpectedDatabase(value = "/controller/shipment/after/confirmed_fake_shipment.xml", assertionMode = NON_STRICT)
    @DisplayName("Подтверждение фэйковой отгрузки")
    void confirmShipmentApplicationFake() throws Exception {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.OK)
            .andExpect(content().string(SUCCESS_RESPONSE));
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_valid_orders.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/after/confirm_shipment_registry_sent.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Подтверждение отгрузки - идемпотенонсть успешного кейса")
    void confirmShipmentApplicationIdempotency() {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.OK)
            .andExpect(content().string(SUCCESS_RESPONSE));
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.OK)
            .andExpect(content().string(SUCCESS_RESPONSE));
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_invalid_statuses.xml")
    @DisplayName("Подтверждение отгрузки - ошибка для невалидных статусов заявки")
    void confirmShipmentApplicationInvalidStatuses() {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.BAD_REQUEST, 1L)
            .andExpect(content().json(
                "{\"errors\":[{\"entityType\":\"SHIPMENT_APPLICATION\",\"id\":1,\"errorCode\":\"INVALID_STATUS\"}]}",
                true
            ));

        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.BAD_REQUEST, 2L)
            .andExpect(content().json(
                "{\"errors\":[{\"entityType\":\"SHIPMENT_APPLICATION\",\"id\":2,\"errorCode\":\"INVALID_STATUS\"}]}",
                true
            ));
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_valid_orders.xml")
    @DisplayName("Подтверждение отгрузки - время катофа не влияет на отгрузку")
    void confirmShipmentApplicationAfterCutoff() {

        // заказ в БД от 25 числа
        clock.setFixed(Instant.parse("2019-05-26T06:00:00.00Z"), ZoneId.systemDefault());

        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.OK)
            .andExpect(content().string(SUCCESS_RESPONSE));
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_valid_orders_not_created.xml")
    @DisplayName("Подтверждение отгрузки - заказы валидны но еще не созданы в службе")
    @ExpectedDatabase(
        value = "/controller/shipment/after/confirm_shipment_order_created.xml",
        assertionMode = NON_STRICT
    )
    void confirmShipmentApplicationOrdersValidNotCreated() {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.BAD_REQUEST)
            .andExpect(content().string(
                "{\"errors\":[" +
                    "{\"entityType\":\"ORDER\",\"id\":1,\"errorCode\":\"INVALID_STATUS\"}" + // ENQUEUED
                    "]}"
            ));

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("level=WARN\t" +
                "format=json-exception\t" +
                "code=SHIPMENT_APPLICATION_CONFIRMATION_ERROR\t" +
                "payload={\\\"eventMessage\\\":\\\"Invalid shipment application\\\",\\\"exceptionMessage\\\":" +
                "\\\"EntitiesValidationException: EntitiesValidationException(" +
                "errors=[EntityError(entityType=ORDER, id=1, errorCode=INVALID_STATUS)])");
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_invalid_order_statuses.xml")
    @DisplayName("Подтверждение отгрузки - заказы в невалидных статусах")
    void confirmShipmentApplicationOrdersInvalidStatuses() {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.BAD_REQUEST)
            .andExpect(content().json(
                "{\"errors\":[" +
                    "{\"entityType\":\"ORDER\",\"id\":1,\"errorCode\":\"INVALID_STATUS\"}," + // CANCELLED
                    "{\"entityType\":\"ORDER\",\"id\":2,\"errorCode\":\"VALIDATION_ERROR\"}," + // VALIDATION_ERROR
                    "{\"entityType\":\"ORDER\",\"id\":3,\"errorCode\":\"DRAFT\"}," + // DRAFT
                    "{\"entityType\":\"ORDER\",\"id\":4,\"errorCode\":\"INVALID_STATUS\"}," + // PROCESSING_ERROR
                    "{\"entityType\":\"ORDER\",\"id\":5,\"errorCode\":\"INVALID_STATUS\"}" + // SENDER_SENT
                    "]}"));
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_no_orders.xml")
    @DisplayName("Подтверждение отгрузки - заявка без заказов")
    void confirmShipmentApplicationNoOrders() {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.BAD_REQUEST)
            .andExpect(content().json(
                "{\"errors\":[{\"entityType\":\"SHIPMENT_APPLICATION\",\"id\":1,\"errorCode\":\"NO_ORDERS\"}]}",
                true
            ));
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/controller/shipment/before/confirm_shipment_unsupported_partner_type.xml")
    @DisplayName("Подтверждение отгрузки - неподдерживаемый тип партнёра")
    void confirmShipmentUnsupportedPartnerType() {
        ShipmentTestUtil.confirmShipment(mockMvc, HttpStatus.BAD_REQUEST)
            .andExpect(content().json(
                "{\"message\":\"Shipment application 1 with partner type FULFILLMENT processing is not supported." +
                    " Supported types: [DELIVERY, SORTING_CENTER, OWN_DELIVERY]\"}",
                true
            ));
    }

}
