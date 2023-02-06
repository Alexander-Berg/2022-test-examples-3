package ru.yandex.market.ff.controller.health;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.health.cache.RequestProblemCache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Интеграционные тесты для {@link RequestProblemController}.
 *
 * @author avetokhin 20/03/18.
 */
public class RequestProblemControllerTest extends MvcIntegrationTest {

    @Autowired
    private RequestProblemCache requestProblemCache;

    @BeforeEach
    void init() {
        requestProblemCache.invalidateCache();
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/creation_sla_ok.xml")
    public void supplyCreationSlaOk() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created", "0;ok");
        assertReturnedMessageCorrect("supply-creation-sla-for-not-created", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/creation_sla_ok_with_cs_booking.xml")
    public void supplyCreationSlaOkWithCsBooking() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created", "0;ok");
        assertReturnedMessageCorrect("supply-creation-sla-for-not-created", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/creation_sla_not_ok_with_cs_booking.xml")
    public void supplyCreationSlaNotOkWithCsBooking() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created",
                "2;Supply creation SLA exceeded. Current creation interval is 1320 s (2 requests). " +
                        "Problem requests: [1, 2]");
        MvcResult mvcResult = perform("supply-creation-sla-for-not-created").andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertThat(response, containsString("2;Supply creation SLA exceeded. Supplies:"));
        if (!response.contains("[3, 4]") && !response.contains("[4, 3]")) {
            assertThat(true, equalTo(false));
        }
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/creation_sla_exceeded_for_one_request.xml")
    public void supplyCreationSlaForAlreadyCreatedOneRequestNotCrit() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created",
                "0;ok");
        assertReturnedMessageCorrect("supply-creation-sla-for-not-created", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/creation_sla_exceeded_for_two_requests.xml")
    public void supplyCreationSlaForAlreadyCreatedTwoRequestsCrit() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created",
                "2;Supply creation SLA exceeded. Current creation interval is 2040 s (2 requests). " +
                        "Problem requests: [1, 3]");
        assertReturnedMessageCorrect("supply-creation-sla-for-not-created", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/creation_sla_exceeded_for_not_created.xml")
    public void supplyCreationSlaForNotCreatedCrit() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created", "0;ok");
        assertReturnedMessageCorrect("supply-creation-sla-for-not-created",
                "2;Supply creation SLA exceeded. Supplies: [1]");
    }

    @Test
    @DatabaseSetup(
            "classpath:controller/request-problem/creation_sla_exceeded_for_not_created_but_sent_to_service.xml"
    )
    public void supplyCreationSlaForNotCreatedButSentToServiceCrit() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created", "0;ok");
        assertReturnedMessageCorrect("supply-creation-sla-for-not-created",
                "2;Supply creation SLA exceeded. Supplies: [1]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/creation_sla_ok_in_case_of_slot_change.xml")
    public void supplyCreationSlaForAlreadyCreatedOkInCaseOfSlotChange() throws Exception {
        assertReturnedMessageCorrect("supply-creation-sla-for-already-created", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/validation_sla_ok.xml")
    public void supplyValidationSlaOk() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated", "0;ok");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/validation_sla_exceeded.xml")
    public void supplyValidationSlaForAlreadyValidatedCrit() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated",
                "2;Supply validation SLA exceeded. Current validation interval is 120 s (2 requests). " +
                        "Problem requests: [1, 3]");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/validation_sla_exceeded_for_not_validated.xml")
    public void supplyValidationSlaForNotValidatedCrit() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated", "0;ok");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated",
                "2;Supply validation SLA exceeded. Supplies: [1]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/shadow_supply_validation_sla_exceeded.xml")
    public void shadowSupplyValidationSlaCrit() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated",
                "2;Supply validation SLA exceeded. Current validation interval is 120 s (2 requests). " +
                        "Problem requests: [1, 3]");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/shadow_supply_validation_sla_exceeded_for_not_validated.xml")
    public void shadowSupplyValidationSlaForNotCreatedCrit() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated", "0;ok");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated",
                "2;Supply validation SLA exceeded. Supplies: [1]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/shadow_withdraw_validation_sla_exceeded_for_one_request.xml")
    public void shadowWithdrawValidationSlaForOneRequestNotCrit() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated",
                "0;ok");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/shadow_withdraw_validation_sla_exceeded_for_two_requests.xml")
    public void shadowWithdrawValidationSlaForTwoRequestsCrit() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated",
                "2;Supply validation SLA exceeded. Current validation interval is 120 s (2 requests). " +
                        "Problem requests: [1, 3]");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/shadow_withdraw_validation_sla_exceeded_for_not_validated.xml")
    public void shadowWithdrawValidationSlaForNotCreatedCrit() throws Exception {
        assertReturnedMessageCorrect("supply-validation-sla-for-already-validated", "0;ok");
        assertReturnedMessageCorrect("supply-validation-sla-for-not-validated",
                "2;Supply validation SLA exceeded. Supplies: [1]");
    }

    @Test
    public void longDelayedProcessTimeTasksOnEmptyDb() throws Exception {
        assertReturnedMessageCorrect("long-delayed-tasks", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/delayed_db_queue_tasks.xml")
    public void longDelayedProcessTimeTasks() throws Exception {
        assertReturnedMessageCorrect("long-delayed-tasks",
                "1;Db-queue tasks with long delayed process time: [1, 2, 3]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/invalid_rejected_manually_created_utilization_transfers.xml")
    public void invalidAndRejectedManuallyCreatedUtilizationTransfers() throws Exception {
        assertReturnedMessageCorrect("rejected-or-invalid-utilization-transfers",
                "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/invalid_rejected_force_utilization_transfers.xml")
    public void invalidAndRejectedForceUtilizationTransfers() throws Exception {
        assertReturnedMessageCorrect("rejected-or-invalid-utilization-transfers",
                "2;There are some rejected utilization transfers: [1]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/invalid_rejected_cis_quarantine_transfers.xml")
    public void invalidAndRejectedCisQuarantineTransfers() throws Exception {
        assertReturnedMessageCorrect("rejected-or-invalid-cis-quarantine-transfers",
                "2;There are some rejected transfers from cis quarantine: [1]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/no_utilization_transfers_only_in_inactive_outbounds.xml")
    public void noUtilizationTransfersOnlyInInactiveOutbounds() throws Exception {
        assertReturnedMessageCorrect("utilization-transfers-only-in-inactive-outbounds",
                "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/some_utilization_transfers_only_in_inactive_outbounds.xml")
    public void someUtilizationTransfersOnlyInInactiveOutbounds() throws Exception {
        assertReturnedMessageCorrect("utilization-transfers-only-in-inactive-outbounds",
                "2;There are some utilization transfers only in inactive outbounds: [1, 3]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/no_utilization_transfers_without_outbounds_too_long.xml")
    public void noUtilizationTransfersWithoutOutboundsForTooLong() throws Exception {
        assertReturnedMessageCorrect("utilization-transfers-without-outbounds-too-long",
                "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/some_utilization_transfers_without_outbounds_too_long.xml")
    public void someUtilizationTransfersWithoutOutboundsForTooLong() throws Exception {
        assertReturnedMessageCorrect("utilization-transfers-without-outbounds-too-long",
                "2;There are some utilization transfers without active outbounds for too long: [4]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/shadow_withdraw_in_validated_status_too_long_ok.xml")
    public void shadowWithdrawInValidatedStatusTooLongOk() throws Exception {
        assertReturnedMessageCorrect("shadow-withdraw-in-validated-status-too-long",
                "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/shadow_withdraw_in_validated_status_too_long_crit.xml")
    public void shadowWithdrawInValidatedStatusTooLongCrit() throws Exception {
        MvcResult mvcResult = perform("shadow-withdraw-in-validated-status-too-long").andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertThat(response, containsString("2;Shadow withdrawals with identifiers"));
        assertThat(response, containsString("have been in the validated status for too long."));
        if (!response.contains("[1, 2]") && !response.contains("[2, 1]")) {
            assertThat(true, equalTo(false));
        }
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/utilization-transfers-stack-in-transitional-statuses.xml")
    public void utilizationTransfersStackInTransitionalStatuses() throws Exception {
        assertReturnedMessageCorrect("utilization-transfers-stack-in-transitional-statuses",
                "2;Next utilization transfers in transition statuses more than 120 minutes: [2]");

    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/utilization-transfers-stack-in-transitional-statuses-ok.xml")
    public void utilizationTransfersStackInTransitionalStatusesOk() throws Exception {
        assertReturnedMessageCorrect("utilization-transfers-stack-in-transitional-statuses", "0;ok");

    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/auto-transfers-not-started-after-deadline-ok.xml")
    public void autoTransfersNotStartedAfterDeadlineOk() throws Exception {
        assertReturnedMessageCorrect("auto-transfers-not-started-after-deadline", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/auto-transfers-not-started-after-deadline.xml")
    public void autoTransfersNotStartedAfterDeadline() throws Exception {
        assertReturnedMessageCorrect("auto-transfers-not-started-after-deadline",
            "2;Next auto transfers do not start more than 1460 minutes: [1, 2]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/requests-stack-in-sent-to-service-status.xml")
    public void requestsStackInSentToServiceStatusCrit() throws Exception {
        assertReturnedMessageCorrect("requests-stack-in-sent-to-service-status",
                "2;Next requests more than 120 minutes: SENT_TO_SERVICE: [2]\nPLAN_REGISTRY_SENT: [3, 4]");

    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/requests-stack-in-sent-to-service-status-ok.xml")
    public void requestsStackInSentToServiceStatusOk() throws Exception {
        assertReturnedMessageCorrect("requests-stack-in-sent-to-service-status", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/initial-acceptance-not-finished-ok.xml")
    public void initialAcceptanceNotFinishedWhenOk() throws Exception {
        assertReturnedMessageCorrect("initial-acceptance-not-finished", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/initial-acceptance-not-finished-not-ok.xml")
    public void initialAcceptanceNotFinishedWhenNotOk() throws Exception {
        assertReturnedMessageCorrect("initial-acceptance-not-finished",
                "2;Initial acceptance details not loaded for requests [1]");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/queue-tasks-lock-for-too-long-ok.xml")
    public void queueTasksLockForTooLongWhenOk() throws Exception {
        assertReturnedMessageCorrect("queue-tasks-lock-for-too-long", "0;ok");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-problem/queue-tasks-lock-for-too-long-not-ok.xml")
    public void queueTasksLockForTooLongWhenNotOk() throws Exception {
        assertReturnedMessageCorrect("queue-tasks-lock-for-too-long",
                "2;There are queue_tasks_lock taken for too long for queues: [CREATE_TRANSFER_FROM_PREPARED]");
    }


    private ResultActions perform(final String method) throws Exception {
        return mockMvc.perform(
                get("/health/" + method)
        ).andDo(print());
    }

    private void assertReturnedMessageCorrect(String method, String message) throws Exception {
        MvcResult mvcResult = perform(method).andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(message));
    }
}
