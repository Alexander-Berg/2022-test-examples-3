package ru.yandex.market.logistics.lom.controller.shipment.processing;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

abstract class AbstractShipmentAsyncResultErrorControllerTest extends AbstractContextualTest {
    @Autowired
    private BusinessProcessStateRepository businessProcessStateRepository;

    @BeforeEach
    void setUp() {
        businessProcessStateRepository.save(
            new BusinessProcessState()
                .setStatus(BusinessProcessStatus.ASYNC_REQUEST_SENT)
                .setQueueType(getQueueType())
                .setSequenceId(1004L)
        );
    }

    @Test
    @DisplayName("Успешный сценарий - забор")
    @DatabaseSetup("/controller/shipment/processing/createIntake/error/before/shipment_application_wo_external_id.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/error/after/shipment_application_intake_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeError() throws Exception {
        performCall(
            "createIntakeError",
            "controller/shipment/processing/createIntake/error/request/create_intake_error.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Айди уже задан - забор")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/error/before/shipment_application_intake_already_set.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/error/after/shipment_application_intake_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeError_alreadySet() throws Exception {
        performCall(
            "createIntakeError",
            "controller/shipment/processing/createIntake/error/request/create_intake_error.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Shipment application 1 externalId is already set (e1)"));
    }

    @Test
    @DisplayName("Пустой запрос - забор")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/error/before/shipment_application_intake_already_set.xml"
    )
    void createIntakeError_empty() throws Exception {
        performCall(
            "createIntakeError",
            "controller/shipment/processing/createIntake/error/request/create_intake_error_empty.json"
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Идемпотентность - забор")
    @DatabaseSetup("/controller/shipment/processing/createIntake/error/before/shipment_application_intake_error.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/error/after/shipment_application_intake_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeError_idempotency() throws Exception {
        performCall(
            "createIntakeError",
            "controller/shipment/processing/createIntake/error/request/create_intake_error.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешный сценарий - самопривоз")
    @DatabaseSetup(
        "/controller/shipment/processing/createSelfExport/error/before/shipment_application_wo_external_id.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createSelfExport/error/after/shipment_application_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSelfExportError() throws Exception {
        performCall(
            "createSelfExportError",
            "controller/shipment/processing/createSelfExport/error/request/create_self_export_error.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Айди уже задан - самопривоз")
    @DatabaseSetup(
        "/controller/shipment/processing/createSelfExport/error/before/shipment_application_se_success_already_set.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createSelfExport/error/after/shipment_application_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSelfExportError_alreadySet() throws Exception {
        performCall(
            "createSelfExportError",
            "controller/shipment/processing/createSelfExport/error/request/create_self_export_error.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Shipment application 1 externalId is already set (e1)"));

    }

    @Test
    @DisplayName("Идемпотентность - самопривоз")
    @DatabaseSetup("/controller/shipment/processing/createSelfExport/error/before/shipment_application_error.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createSelfExport/error/after/shipment_application_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSelfExportError_idempotency() throws Exception {
        performCall(
            "createSelfExportError",
            "controller/shipment/processing/createSelfExport/error/request/create_self_export_error.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Пустой запрос - самопривоз")
    @DatabaseSetup(
        "/controller/shipment/processing/createSelfExport/error/before/shipment_application_se_success_already_set.xml"
    )
    void createSelfExportError_empty() throws Exception {
        performCall(
            "createSelfExportError",
            "controller/shipment/processing/createSelfExport/error/request/create_self_export_error_empty.json"
        )
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {"createIntakeError", "createSelfExportError"})
    @DisplayName("Невалидный запрос")
    void badRequest(String method) throws Exception {
        performCall(method, "controller/common/empty_object.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(
                "controller/shipment/processing/createSelfExport/error/response/validation_error.json"
            ));
    }

    @Nonnull
    abstract ResultActions performCall(@Nonnull String method, String requestFileName) throws Exception;

    @Nonnull
    abstract QueueType getQueueType();
}
