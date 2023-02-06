package ru.yandex.market.logistics.lom.service.registry;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.jobs.model.ShipmentApplicationIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.registry.BaseCreateRegistryExternalService;
import ru.yandex.market.logistics.lom.service.AbstractExternalServiceTest;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createShipmentApplicationIdPayload;

abstract class BaseCreateRegistryExternalServiceTest extends AbstractExternalServiceTest {

    private static final String LOG_TEMPLATE_WO_ORDERS =
        "level=%s\t" +
            "format=plain\t" +
            "code=BUILD_REGISTRY_ERROR\t" +
            "payload=%s\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=BUSINESS_REGISTRY_EVENT\t" +
            "entity_types=shipmentApplication,partner,platform\t" +
            "entity_values=shipmentApplication:1,partner:20,platform:\n";

    private static final String LOG_TEMPLATE_WITH_ORDERS =
        "level=%s\t" +
            "format=plain\t" +
            "code=BUILD_REGISTRY_ERROR\t" +
            "payload=%s\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=BUSINESS_REGISTRY_EVENT\t" +
            "entity_types=shipmentApplication,partner,platform\t" +
            "entity_values=shipmentApplication:1,partner:20,platform:YANDEX_DELIVERY\n";

    private static final Exception EXCEPTION = mock(Exception.class);

    static final ShipmentApplicationIdPayload PAYLOAD = createShipmentApplicationIdPayload(1, "123");

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-11T00:00:00.00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Запрос на создание реестра - обработка неудачи")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_w_registry.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/failed_creating_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processFinalFailureTest() {
        getService().processFinalFailure(PAYLOAD, EXCEPTION);
    }

    @Test
    @DisplayName("Запрос на создание реестра - обработка неудачи, если реестр так и не был создан")
    @SneakyThrows
    @DatabaseSetup("/service/externalvalidation/before/valid_shipment_application_wo_registry.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/before/valid_shipment_application_wo_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processFinalFailureForApplicationWithoutRegistry() {
        getService().processFinalFailure(PAYLOAD, EXCEPTION);
    }

    void assertBuildRegistryWOOrdersEventLogged(String level, String payload) {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(String.format(LOG_TEMPLATE_WO_ORDERS, level, payload));
    }

    void assertBuildRegistryWithOrdersEventLogged(String level, String payload) {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(String.format(LOG_TEMPLATE_WITH_ORDERS, level, payload));
    }

    abstract BaseCreateRegistryExternalService getService();
}
