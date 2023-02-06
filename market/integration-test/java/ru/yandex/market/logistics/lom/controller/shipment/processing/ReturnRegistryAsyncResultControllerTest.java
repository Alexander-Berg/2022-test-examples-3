package ru.yandex.market.logistics.lom.controller.shipment.processing;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Пуш ответа на отправку возвратного реестра от LGW")
@ParametersAreNonnullByDefault
@DatabaseSetup("/service/business_process_state/fulfillment_create_return_registry_async_request_sent.xml")
class ReturnRegistryAsyncResultControllerTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Успешное создание возвратного реестра")
    @DatabaseSetup("/controller/shipment/processing/returnregistries/before/registry_in_processing_status.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/returnregistries/after/registry_in_created_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSuccessValid() {
        performCall("createSuccess")
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное создание реестра - реестр не существует")
    void createSuccessRegistryNotFound() {
        performCall("createSuccess")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [RETURN_REGISTRY] with id [1]"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное создание возвратного реестра - реестр был создан с ошибкой ранее")
    @DatabaseSetup("/controller/shipment/processing/returnregistries/before/registry_in_error_status.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/returnregistries/after/registry_in_created_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSuccessRegistryInErrorStatus() {
        performCall("createSuccess")
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка при создании возвратного реестра")
    @DatabaseSetup("/controller/shipment/processing/returnregistries/before/registry_in_processing_status.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/returnregistries/before/registry_in_error_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createErrorValid() {
        performCall("createError")
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка при создании реестра - реестр не существует")
    void createErrorRegistryNotFound() {
        performCall("createError")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [RETURN_REGISTRY] with id [1]"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка при создании возвратного реестра - реестр был успешно создан ранее")
    @DatabaseSetup("/controller/shipment/processing/returnregistries/after/registry_in_created_status.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/returnregistries/after/registry_in_created_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createErrorRegistryInSuccessStatus() {
        performCall("createError")
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Nonnull
    @SneakyThrows
    private ResultActions performCall(String method) {
        return performCall(method, "controller/shipment/processing/returnregistries/request/request.json");
    }

    @Nonnull
    @SneakyThrows
    private ResultActions performCall(String method, String fileName) {
        return mockMvc.perform(
            put(String.format("/returnRegistries/%s", method))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(fileName))
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {"createSuccess", "createError"})
    @DisplayName("Невалидный запрос")
    void badRequest(String method) throws Exception {
        performCall(method, "controller/common/empty_object.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/common/sequence_id_is_missing.json"));
    }
}
