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
import ru.yandex.market.logistics.lom.jobs.model.ShipmentApplicationIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.WithdrawTransactionProcessor;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createShipmentApplicationIdPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

abstract class AbstractShipmentAsyncResultSuccessControllerTest extends AbstractContextualTest {
    @Autowired
    private WithdrawTransactionProcessor withdrawTransactionProcessor;

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
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/success/before/shipment_application_wo_external_id.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/success/after/shipment_application_intake_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeSuccess() throws Exception {
        performCall(
            "createIntakeSuccess",
            "controller/shipment/processing/createIntake/success/request/create_intake_success.json"
        )
            .andExpect(status().isOk());

        checkAsyncWithdrawTransactionProcessor();
    }

    @Test
    @DisplayName("Успешный сценарий - забор, при существовании транзакции")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/success/before/shipment_application_transaction_exists.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/success/after/shipment_application_intake_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeTransactionExists() throws Exception {
        performCall(
            "createIntakeSuccess",
            "controller/shipment/processing/createIntake/success/request/create_intake_success.json"
        )
            .andExpect(status().isOk());
        checkAsyncWithdrawTransactionProcessor();
    }

    @Test
    @DisplayName("Успешный сценарий - забор, но ошибка при создании транзакции")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/success/before/shipment_application_charge_not_exists.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/success/after/" +
            "shipment_application_intake_success_no_tx.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeNoCharge() throws Exception {
        performCall(
            "createIntakeSuccess",
            "controller/shipment/processing/createIntake/success/request/create_intake_success.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_WITHDRAW_TRANSACTION,
            PayloadFactory.createShipmentApplicationIdPayload(1L, "1", 1L)
        );

        softly.assertThatThrownBy(
            () -> withdrawTransactionProcessor.processPayload(createShipmentApplicationIdPayload(1L))
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Service type WITHDRAW charge doesn't exist. Billing entity id: 100.");
    }

    @Test
    @DisplayName("Айди уже задан - забор")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/success/before/shipment_application_intake_already_set.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/success/after/" +
            "shipment_application_intake_success_no_tx.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeSuccessAlreadySet() throws Exception {
        performCall(
            "createIntakeSuccess",
            "controller/shipment/processing/createIntake/success/request/create_intake_success_already_set.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Shipment application 1 externalId is already set (e1)"));
    }

    @Test
    @DisplayName("Обработка отмененной заявки")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/success/before/shipment_application_intake_cancelled.xml"
    )
    void createIntakeSuccessCancelled() throws Exception {
        performCall(
            "createIntakeSuccess",
            "controller/shipment/processing/createIntake/success/request/create_intake_success_already_set.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message")
                .value("Failed to create shipment async. Shipment application 1 status is CANCELLED."));
    }

    @Test
    @DisplayName("Пустой запрос - забор")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/success/before/shipment_application_intake_already_set.xml"
    )
    void createIntakeSuccess_empty() throws Exception {
        performCall(
            "createIntakeSuccess",
            "controller/shipment/processing/createIntake/success/request/create_intake_success_empty.json"
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Идемпотентность - забор")
    @DatabaseSetup(
        "/controller/shipment/processing/createIntake/success/before/shipment_application_intake_already_set.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createIntake/success/after/" +
            "shipment_application_intake_success_no_tx.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createIntakeSuccess_idempotency() throws Exception {
        performCall(
            "createIntakeSuccess",
            "controller/shipment/processing/createIntake/success/request/create_intake_success.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешный сценарий - самопривоз")
    @DatabaseSetup(
        "/controller/shipment/processing/createSelfExport/success/before/shipment_application_wo_external_id.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createSelfExport/success/after/shipment_application_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSelfExportSuccess() throws Exception {
        performCall(
            "createSelfExportSuccess",
            "controller/shipment/processing/createSelfExport/success/request/create_self_export_success.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Айди уже задан - самопривоз")
    @DatabaseSetup(
        "/controller/shipment/processing/createSelfExport/success/before/" +
            "shipment_application_se_success_already_set.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createSelfExport/success/after/shipment_application_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSelfExportSuccess_alreadySet() throws Exception {
        performCall(
            "createSelfExportSuccess",
            "controller/shipment/processing/createSelfExport/success/request/" +
                "create_self_export_success_already_set.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Shipment application 1 externalId is already set (e1)"));
    }

    @Test
    @DisplayName("Идемпотентность - самопривоз")
    @DatabaseSetup(
        "/controller/shipment/processing/createSelfExport/success/before/" +
            "shipment_application_se_success_already_set.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipment/processing/createSelfExport/success/after/shipment_application_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createSelfExportSuccess_idempotency() throws Exception {
        performCall(
            "createSelfExportSuccess",
            "controller/shipment/processing/createSelfExport/success/request/create_self_export_success.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Пустой запрос - самопривоз")
    @DatabaseSetup(
        "/controller/shipment/processing/createSelfExport/success/before/" +
            "shipment_application_se_success_already_set.xml"
    )
    void createSelfExportSuccess_empty() throws Exception {
        performCall(
            "createSelfExportSuccess",
            "controller/shipment/processing/createSelfExport/success/request/create_self_export_success_empty.json"
        )
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {"createIntakeSuccess", "createSelfExportSuccess"})
    @DisplayName("Невалидный запрос")
    void badRequest(String method) throws Exception {
        performCall(method, "controller/common/empty_object.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(
                "controller/shipment/processing/createSelfExport/success/response/validation_error.json"
            ));
    }

    @Nonnull
    abstract ResultActions performCall(@Nonnull String method, String requestFileName) throws Exception;

    @Nonnull
    abstract QueueType getQueueType();

    void checkAsyncWithdrawTransactionProcessor() {
        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L, "1", 1L);
        queueTaskChecker.assertQueueTaskCreated(QueueType.CREATE_WITHDRAW_TRANSACTION, payload);
        withdrawTransactionProcessor.processPayload(payload);
    }
}
