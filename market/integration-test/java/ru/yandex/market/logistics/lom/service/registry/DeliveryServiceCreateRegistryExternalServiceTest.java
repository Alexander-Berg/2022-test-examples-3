package ru.yandex.market.logistics.lom.service.registry;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.delivery.Register;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.registry.BaseCreateRegistryExternalService;
import ru.yandex.market.logistics.lom.jobs.processor.registry.DeliveryServiceCreateRegistryExternalService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createRegisterDs;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createResourceId;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createSender;

@DisplayName("Создание реестра в службе доставки")
class DeliveryServiceCreateRegistryExternalServiceTest extends BaseCreateRegistryExternalServiceTest {

    @Autowired
    private DeliveryServiceCreateRegistryExternalService service;

    @Autowired
    private DeliveryClient deliveryClient;

    @Test
    @DisplayName("Запрос на создание реестра - успешный кейс")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createTaskSuccessful() {
        service.processPayload(PAYLOAD);
        verify(deliveryClient).createRegister(
            eq(createRegisterDs()),
            eq(createPartner()),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Запрос на создание реестра, реестр уже существует и создан в партнере")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry.xml")
    @DatabaseSetup("/service/externalvalidation/before/created_registry.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/before/created_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void registryAlreadyCreatedAtPartner() {
        ProcessingResult processingResult = service.processPayload(PAYLOAD);
        softly.assertThat(processingResult).usingRecursiveComparison().isEqualTo(
            ProcessingResult.unprocessed("Registry is already successfully created at partner, registryId = 1")
        );
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Запрос на создание реестра от нескольких отправителей")
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_multiple_senders.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createTaskMultipleSenders() throws GatewayApiException {
        service.processPayload(PAYLOAD);
        Register registerDs = createRegisterDs(
            createSender().build(),
            List.of(
                createResourceId("1001-LOinttest-1", "test-external-id").build(),
                createResourceId("1002-LOinttest-2", "second-test-external-id").build()
            )
        );
        verify(deliveryClient).createRegister(eq(registerDs), eq(createPartner()), eq(EXPECTED_CLIENT_REQUEST_META));
    }

    @Test
    @DisplayName("Запрос на создание реестра - отгрузка в прошлом (точность день)")
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry.xml")
    void processTaskForPastShipment() throws GatewayApiException {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), ZoneId.systemDefault());

        softly.assertThatThrownBy(() -> service.processPayload(PAYLOAD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Could not create registry for application 1 in past. Shipment date is 2019-06-11");

        assertBuildRegistryWithOrdersEventLogged("ERROR", "Could not create a registry for application in past");
        verify(deliveryClient, never()).createRegister(any(), any(), any());
    }

    @Test
    @DisplayName("Запрос на создание реестра - заказ не создан у партнёра")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry_order_in_process.xml")
    void processTaskForOrderWoExtId() {
        softly.assertThatThrownBy(() -> service.processPayload(PAYLOAD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Orders in shipment application 1 are incomplete");

        assertBuildRegistryWithOrdersEventLogged("ERROR", "Not all shipment orders are ready");
        verify(deliveryClient, never()).createRegister(any(), any(), any());
    }

    @Test
    @DisplayName("Запрос на создание реестра - отгрузка без заказов")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry_no_orders.xml")
    void processTaskForEmptyShipment() {
        service.processPayload(PAYLOAD);
        verify(deliveryClient, never()).createRegister(any(), any(), any());
        assertBuildRegistryWOOrdersEventLogged("WARN", "The shipment application 1 does not contain any orders");
    }

    @Test
    @DisplayName("Запрос на создание реестра - реестр уже существует")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_w_registry.xml")
    void processTaskForExistingRegistry() {
        service.processPayload(PAYLOAD);
        verify(deliveryClient).createRegister(
            eq(createRegisterDs()),
            eq(createPartner()),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Логирование финальной ошибки")
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry.xml")
    @SneakyThrows
    void processFinalFailure() {
        Exception exception = mock(Exception.class);
        when(exception.getMessage()).thenReturn("Root cause");
        String className = ClassUtils.getShortClassName(exception, null);

        service.processFinalFailure(PAYLOAD, exception);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t" +
                    "format=json-exception\t" +
                    "code=SEND_REGISTRY_TO_SD_ERROR\t" +
                    "payload={" +
                    "\\\"eventMessage\\\":\\\"Failed to send a register\\\"," +
                    "\\\"exceptionMessage\\\":\\\"" + className + ": Root cause\\\"," +
                    "\\\"stackTrace\\\":\\\"\\\"}\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=BUSINESS_REGISTRY_EVENT\t" +
                    "entity_types=shipmentApplication,partner,platform\t" +
                    "entity_values=shipmentApplication:1,partner:20,platform:YANDEX_DELIVERY"
            );
    }

    @Override
    BaseCreateRegistryExternalService getService() {
        return service;
    }
}
