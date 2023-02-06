package ru.yandex.market.wms.api.controller.logistic;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PutOutboundRegistryIntegrationTests extends IntegrationTest {

    @Test
    @DisplayName("Создать реестр изъятия")
    @DatabaseSetup(value = "/empty-db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/put-outbound-registry/simple/after",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @SneakyThrows
    public void successPutOutboundRegistryTest() {
        assertHttpCall(put("/TENANT_NAME/outbound-registry"),
                status().isOk(),
                "put-outbound-registry/simple/request.json");
    }

    @Test
    @DisplayName("Создать реестр изъятия для корректного догруза")
    @DatabaseSetup(
            value = "/put-outbound-registry/additional-load-success/full-prepared-orders.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/put-outbound-registry/additional-load-success/after",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @SneakyThrows
    public void successAdditionalLoadPutOutboundRegistryTest() {
        assertHttpCall(put("/TENANT_NAME/outbound-registry"),
                status().isOk(),
                "put-outbound-registry/request.json");
    }

    @Test
    @DisplayName("Ошибка при создании неполного изъятия для догруза")
    @DatabaseSetup(
            value = "/put-outbound-registry/additional-load-failure/full-prepared-orders.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/put-outbound-registry/additional-load-failure/after",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @SneakyThrows
    public void failureAdditionalLoadPutOutboundRegistryTest() {
        assertHttpCall(put("/TENANT_NAME/outbound-registry"),
                status().is5xxServerError(),
                "put-outbound-registry/request.json");
    }

    @Test
    @DisplayName("Создание неполного изъятия для догруза при отключенной валидации")
    @DatabaseSetup(
            value = "/put-outbound-registry/additional-load-without-check-sucess/full-prepared-orders.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/put-outbound-registry/additional-load-without-check-sucess/after",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @SneakyThrows
    public void successAdditionalLoadWithoutCheckPutOutboundRegistryTest() {
        assertHttpCall(put("/TENANT_NAME/outbound-registry"),
                status().isOk(),
                "put-outbound-registry/request.json");
    }
}
