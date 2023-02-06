package ru.yandex.market.logistics.lom.controller.shipment.processing;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createRegistryIdPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("RegistryAsyncResultController пуш ответа от LGW")
abstract class AbstractRegistryAsyncResultControllerTest extends AbstractContextualTest {

    @Autowired
    private BusinessProcessStateRepository businessProcessStateRepository;

    @BeforeEach
    void setUp() {
        businessProcessStateRepository.save(
            new BusinessProcessState()
                .setStatus(BusinessProcessStatus.ASYNC_REQUEST_SENT)
                .setQueueType(getQueueType())
                .setSequenceId(1007L)
        );
    }

    @SneakyThrows
    @Test
    @DisplayName("Успешное создание реестра - DP->CREATED, отправка запроса на получение АПП по созданному реестру")
    @DatabaseSetup("/controller/shipment/processing/registries/before/shipment_application_in_process.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/registries/after/shipment_application_registry_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSuccessValid() {
        performCall(
            "createSuccess",
            "controller/shipment/processing/registries/request/success.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.GET_ACCEPTANCE_CERTIFICATE,
            createRegistryIdPayload(1L, "1", 1L)
        );
    }

    @SneakyThrows
    @Test
    @DisplayName("Повторный успешный ответ - перезаписать external_id")
    @DatabaseSetup("/controller/shipment/processing/registries/before/shipment_application_registry_created.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/registries/after/shipment_application_registry_overwritten.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSuccessOverwriteId() {
        performCall(
            "createSuccess",
            "controller/shipment/processing/registries/request/success.json"
        )
            .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    @DisplayName("Успешное создание реестра - реестр в ошибочном статусе")
    @DatabaseSetup("/controller/shipment/processing/registries/before/registry_error.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/registries/after/shipment_application_registry_overwritten.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSuccessRegistryError() {
        performCall(
            "createSuccess",
            "controller/shipment/processing/registries/request/success.json"
        )
            .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    @DisplayName("Успешное создание реестра - реестр не найден")
    void createSuccessRegistryNotFound() {
        performCall(
            "createSuccess",
            "controller/shipment/processing/registries/request/success.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [REGISTRY] with id [1]"));
    }

    @SneakyThrows
    @Test
    @DisplayName("Ошибка создания реестра - в статусе CREATED")
    @DatabaseSetup("/controller/shipment/processing/registries/before/shipment_application_registry_created_error.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/registries/after/shipment_application_registry_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createErrorCreated() {
        performCall(
            "createError",
            "controller/shipment/processing/registries/request/error.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @SneakyThrows
    @Test
    @DisplayName("Ошибка создания реестра - реестр не найден")
    void createErrorRegistryNotFound() {
        performCall(
            "createError",
            "controller/shipment/processing/registries/request/error.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json(
                "{\"message\":\"Failed to find [REGISTRY] with id [1]\"," +
                    "\"resourceType\":\"REGISTRY\",\"identifier\":\"[1]\"}",
                true
            ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationArguments")
    @DisplayName("Невалидный запрос")
    void badRequest(String method, String fileName) throws Exception {
        performCall(method, "controller/common/empty_object.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(fileName));
    }

    @Nonnull
    static Stream<Arguments> validationArguments() {
        return Stream.of(
            Arguments.of(
                "createSuccess",
                "controller/shipment/processing/registries/request/response/create_success_validation_error.json"
            ),
            Arguments.of(
                "createError",
                "controller/common/sequence_id_is_missing.json"
            )
        );
    }

    @Nonnull
    abstract ResultActions performCall(String method, String requestFileName) throws Exception;

    @Nonnull
    abstract QueueType getQueueType();
}
