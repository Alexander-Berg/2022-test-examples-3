package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistics.lom.jobs.model.ReturnRegistryIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.CreateReturnRegistryProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Отправка возвратного реестра партнёру")
@DatabaseSetup("/jobs/executor/createReturnRegister/after/new_return_registry.xml")
class CreateReturnRegistryServiceTest extends AbstractExternalServiceTest {

    private static final ReturnRegistryIdPayload PAYLOAD = PayloadFactory.createReturnRegistryIdPayload(1L, "123");

    @Autowired
    CreateReturnRegistryProcessor createReturnRegistryProcessor;

    @Autowired
    FulfillmentClient fulfillmentClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Отправка возвратного реестра")
    @SneakyThrows
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_has_return_register.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/returnregistry/after/return_register_creation_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnRegistrySending() {
        createReturnRegistryProcessor.processPayload(PAYLOAD);
        verify(fulfillmentClient).createReturnRegister(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnRegister("external-id-from-SC", "133")),
            eq(new Partner(133L)),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Возвратный реестр не отправляется, если в нём нет заказов")
    @SneakyThrows
    void noOrdersToSendReturnRegistry() {
        createReturnRegistryProcessor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=WARN\t" +
                    "format=plain\t" +
                    "code=BUILD_RETURN_REGISTRY_ERROR\t" +
                    "payload=No orders to send with return registry\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=BUSINESS_REGISTRY_EVENT\t" +
                    "entity_types=platform,returnRegistry\t" +
                    "entity_values=platform:,returnRegistry:1\n"
            );
    }

    @Test
    @DisplayName("При отправке возвратного реестра используется идентификатор из возвратного сегмента вейбилла")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_with_return_waybill_segment.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_has_return_register.xml",
        type = DatabaseOperation.REFRESH
    )
    void sendReturnRegistryOnlyForOrdersContainingScInWaybill() throws Exception {
        createReturnRegistryProcessor.processPayload(PAYLOAD);
        verify(fulfillmentClient).createReturnRegister(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnRegister("external-id-from-return-SC", "1")),
            eq(new Partner(133L)),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Логирование финальной ошибки")
    @SneakyThrows
    void processFinalFailure() {
        Exception exception = mock(Exception.class);
        when(exception.getMessage()).thenReturn("Root cause");
        String className = ClassUtils.getShortClassName(exception, null);

        createReturnRegistryProcessor.processFinalFailure(PAYLOAD, exception);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t" +
                    "format=json-exception\t" +
                    "code=SEND_RETURN_REGISTRY_TO_SC_ERROR\t" +
                    "payload={" +
                    "\\\"eventMessage\\\":\\\"Failed to send a return register\\\"," +
                    "\\\"exceptionMessage\\\":\\\"" + className + ": Root cause\\\"," +
                    "\\\"stackTrace\\\":\\\"\\\"}\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=BUSINESS_REGISTRY_EVENT\t" +
                    "entity_types=platform,returnRegistry\t" +
                    "entity_values=platform:,returnRegistry:1"
            );
    }
}
