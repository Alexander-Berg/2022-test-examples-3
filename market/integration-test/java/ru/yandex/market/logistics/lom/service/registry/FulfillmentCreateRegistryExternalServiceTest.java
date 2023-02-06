package ru.yandex.market.logistics.lom.service.registry;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.lom.exception.RegistryOrdersNotReadyException;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.registry.BaseCreateRegistryExternalService;
import ru.yandex.market.logistics.lom.jobs.processor.registry.FulfillmentCreateRegistryExternalService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createRegister;

@DisplayName("Создание реестра в сортировочном центре")
class FulfillmentCreateRegistryExternalServiceTest extends BaseCreateRegistryExternalServiceTest {
    @Autowired
    private FulfillmentCreateRegistryExternalService service;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    @DisplayName("Запрос на создание реестра - запрос на рассмотрении партнёра")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    void createTaskSuccessful() {
        service.processPayload(PAYLOAD);
        verify(fulfillmentClient).createRegister(
            eq(createRegister()),
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
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Запрос на создание реестра - отгрузка в прошлом (точность день)")
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry.xml")
    void processTaskForPastShipment() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), ZoneId.systemDefault());
        softly.assertThatThrownBy(() -> service.processPayload(PAYLOAD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Could not create registry for application 1 in past. Shipment date is 2019-06-11");

        assertBuildRegistryWithOrdersEventLogged("ERROR", "Could not create a registry for application in past");
    }

    @Test
    @DisplayName("Запрос на создание реестра - заказ не создан у партнёра")
    @DatabaseSetup(
        "/service/externalvalidation/before/valid_shipment_application_wo_registry_order_in_process.xml"
    )
    void processTaskForOrderWoExtId() {
        softly.assertThatThrownBy(() -> service.processPayload(PAYLOAD))
            .isInstanceOf(RegistryOrdersNotReadyException.class)
            .hasMessage("Orders in shipment application 1 are incomplete");

        assertBuildRegistryWithOrdersEventLogged("ERROR", "Not all shipment orders are ready");
    }

    @Test
    @DisplayName("Запрос на создание реестра - реестр уже существует")
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_w_registry.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/before/valid_shipment_application_w_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    void processTaskForExistingRegistry() throws Exception {
        service.processPayload(PAYLOAD);
        verify(fulfillmentClient).createRegister(
            eq(createRegister()),
            eq(createPartner()),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Запрос на создание реестра - отгрузка без заказов")
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry_no_orders.xml")
    void processTaskForEmptyShipment() {
        service.processPayload(PAYLOAD);

        assertBuildRegistryWOOrdersEventLogged("WARN", "The shipment application 1 does not contain any orders");
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
                    "code=SEND_REGISTRY_TO_SC_ERROR\t" +
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
