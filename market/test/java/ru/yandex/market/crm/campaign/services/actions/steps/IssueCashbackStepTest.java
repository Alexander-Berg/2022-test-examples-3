package ru.yandex.market.crm.campaign.services.actions.steps;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCashbackStep;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.services.actions.contexts.CashbackIssuanceStepContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.ActionExecutionContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.PlainStepStatusUpdaterImpl;
import ru.yandex.market.crm.campaign.services.email.NotificationEmailSender;
import ru.yandex.market.crm.campaign.test.StepStatusDAOMock;
import ru.yandex.market.crm.campaign.test.StepStatusUpdaterMock;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.external.loyalty.BunchCheckResponse;
import ru.yandex.market.crm.external.loyalty.MarketLoyaltyClient;
import ru.yandex.market.crm.external.loyalty.WalletBunchSaveRequest;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.TaskStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssueCashbackStepTest {

    private static final String BUNCH_ID = "77777";
    private static final Long PROMO_ID = 1484884L;
    private static final long CASHBACK_AMOUNT = 100500L;
    private static final int PLANNED_COUNT = 100;
    private static final String STEP_ID = "step";
    private static final String ACTION_ID = "action";

    private final MarketLoyaltyClient marketLoyaltyClient = mock(MarketLoyaltyClient.class);
    private final Control<Void> control = mock(Control.class);
    private final YtFolders mockedYtFolders = mock(YtFolders.class);

    private PlainAction action;
    private PlainStepStatusUpdaterImpl stepStatusUpdater;
    private StepStatusDAOMock stepsStatusDAO;
    private IssueCashbackStep step;
    private ru.yandex.market.crm.campaign.services.actions.steps.IssueCashbackStep issueCashbackStep;
    private NotificationEmailSender emailSender;

    @Before
    public void setUp() {
        stepsStatusDAO = new StepStatusDAOMock();
        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.DIRECT_ONLY, "segment_id"));

        prepareCoinRequestFolder();
        prepareSaveResponse();
        action = new PlainAction();
        action.setId(ACTION_ID);
        action.setConfig(config);

        step = new ru.yandex.market.crm.campaign.domain.actions.steps.IssueCashbackStep();
        step.setId(STEP_ID);
        step.setPromoId(PROMO_ID);
        step.setCashbackCount(CASHBACK_AMOUNT);

        stepStatusUpdater = new PlainStepStatusUpdaterImpl(action.getId(), step.getId(),
                new StepStatusUpdaterMock(stepsStatusDAO));

        emailSender = mock(NotificationEmailSender.class);

        issueCashbackStep = new ru.yandex.market.crm.campaign.services.actions.steps.IssueCashbackStep(
                marketLoyaltyClient,
                "segment_issue",
                emailSender,
                "https://test_url"
        );
    }

    /**
     * Перед запуском шага ещё неизвестен bunchId, поэтому шаг должен сделать запрос на генерацию
     * пачек монеток и получить в ответ этот bunchId.
     */
    @Test
    public void startIsRequestedWhenBunchIdIsUnknown() throws Exception {
        var stepContext = saveStatusAndGetContext(initialStatus());
        ExecutionResult result = issueCashbackStep.run(stepContext, null, control);
        var statusAfter = loadSavedStatus();

        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertEquals(BUNCH_ID, statusAfter.getBunchId());
        assertEquals(Integer.valueOf(0), statusAfter.getProcessedCount());
    }

    @Test
    public void testSaveBunchRequestArguments() throws Exception {
        var stepContext = saveStatusAndGetContext(initialStatus());
        issueCashbackStep.run(stepContext, null, control);

        ArgumentCaptor<WalletBunchSaveRequest> captor = ArgumentCaptor.forClass(WalletBunchSaveRequest.class);
        verify(marketLoyaltyClient).saveWalletBunchRequest(captor.capture());

        var saveRequest = captor.getValue();
        assertNotNull(saveRequest);
        assertEquals(PROMO_ID, saveRequest.getPromoId());
        assertEquals(ACTION_ID + "_" + STEP_ID, saveRequest.getCampaignName());
        assertEquals(PLANNED_COUNT, saveRequest.getCount().intValue());
        assertEquals(String.format("%s_%s", ACTION_ID, STEP_ID), saveRequest.getInput());
    }

    /**
     * Сгенерировались все монетки, но статус PROCESSING.
     */
    @Test
    public void testRepeatWhenNotDoneAndProcessing() throws Exception {
        statusResponseIs(processing(10));
        var context = saveStatusAndGetContext(alreadyStartedStatus());
        ExecutionResult result = issueCashbackStep.run(context, null, control);

        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertEquals(Integer.valueOf(10), loadSavedStatus().getProcessedCount());
    }

    /**
     * Ошибка генерации: код ERROR. Шаг падает с ошибкой, поскольку Лоялти таблицу не выгружают в таком случае
     */
    @Test
    public void testErrorCode() throws Exception {
        statusResponseIs(error("ERROR"));

        var context = saveStatusAndGetContext(alreadyStartedStatus());
        ExecutionResult result = issueCashbackStep.run(context, null, control);
        var stepStatus = loadSavedStatus();

        assertEquals(TaskStatus.FAILING, result.getNextStatus());
    }

    /**
     * Ошибки во время генерации
     */
    @Test
    public void testRuntimeErrors() throws Exception {
        var statusResponse = ok(PLANNED_COUNT / 2);
        statusResponse.setErrors(Map.of("WRONG_ID", 3));
        statusResponseIs(statusResponse);

        var context = saveStatusAndGetContext(alreadyStartedStatus());
        ExecutionResult result = issueCashbackStep.run(context, null, control);
        assertEquals(TaskStatus.WAITING, result.getNextStatus());

        assertEquals(Integer.valueOf(3), loadSavedStatus().getErrorCounts().get("WRONG_ID"));
    }

    /**
     * Ошибки во время генерации
     */
    @Test
    public void testBudgetExhaustedErrors() throws Exception {
        var statusResponse = ok(PLANNED_COUNT / 2);
        statusResponse.setErrors(Map.of("EMISSION_BUDGET_EXCEEDED", 3));
        statusResponseIs(statusResponse);

        var context = saveStatusAndGetContext(alreadyStartedStatus());

        ExecutionResult result = issueCashbackStep.run(context, null, control);

        verify(emailSender, times(1)).sendEmail(any(), any(), any());
        assertEquals(TaskStatus.WAITING, result.getNextStatus());

        assertEquals(Integer.valueOf(3), loadSavedStatus().getErrorCounts().get("EMISSION_BUDGET_EXCEEDED"));
    }

    /**
     * Ошибки во время генерации добавляются к существующим
     */
    @Test
    public void testMergeRuntimeErrors() throws Exception {
        var context = saveStatusAndGetContext(alreadyStartedStatus()
                .setErrorCounts(Map.of("WRONG_ID", 1, "UFO_DETECTED", 0)));
        var statusResponse = ok(PLANNED_COUNT / 2);
        statusResponse.setErrors(Map.of("WRONG_ID", 3));
        statusResponseIs(statusResponse);

        ExecutionResult result = issueCashbackStep.run(context, null, control);
        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertEquals(Integer.valueOf(4), loadSavedStatus().getErrorCounts().get("WRONG_ID"));
    }

    private IssueBunchStepStatus alreadyStartedStatus() {
        return initialStatus().setBunchId(BUNCH_ID);
    }

    private IssueBunchStepStatus initialStatus() {
        return new IssueBunchStepStatus()
                .setStepId(step.getId())
                .setPlannedCount("CASHBACK", PLANNED_COUNT)
                .setProcessedCount("CASHBACK", 0);
    }

    private CashbackIssuanceStepContext saveStatusAndGetContext(IssueBunchStepStatus newStatus) {
        stepsStatusDAO.upsert(action.getId(), newStatus);
        return getCashbackIssuanceStepContext(newStatus);
    }

    @Nonnull
    private CashbackIssuanceStepContext getCashbackIssuanceStepContext(IssueBunchStepStatus newStatus) {
        ActionExecutionContext parentContext = new ActionExecutionContext(action, step, newStatus,
                stepStatusUpdater, null, null, null,
                action.getOuterId() + "_" + step.getId());
        return new CashbackIssuanceStepContext(parentContext, mockedYtFolders);
    }

    private IssueBunchStepStatus loadSavedStatus() {
        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        assertNotNull(status);
        return status;
    }

    private void statusResponseIs(BunchCheckResponse response) {
        when(marketLoyaltyClient.checkWalletBunchRequestStatus(eq(BUNCH_ID))).thenReturn(response);
    }

    @Nonnull
    private BunchCheckResponse ok(int processedCount) {
        BunchCheckResponse statusResponse = new BunchCheckResponse();
        statusResponse.setStatus("OK");
        statusResponse.setProcessedCount(processedCount);
        statusResponse.setErrors(Collections.emptyMap());
        return statusResponse;
    }

    @Nonnull
    private BunchCheckResponse processing(int processedCount) {
        BunchCheckResponse statusResponse = new BunchCheckResponse();
        statusResponse.setStatus("PROCESSING");
        statusResponse.setProcessedCount(processedCount);
        statusResponse.setErrors(Collections.emptyMap());
        return statusResponse;
    }

    @Nonnull
    private BunchCheckResponse error(String errorType) {
        BunchCheckResponse statusResponse = new BunchCheckResponse();
        statusResponse.setStatus(errorType);
        statusResponse.setErrors(Collections.emptyMap());
        return statusResponse;
    }

    private void prepareSaveResponse() {
        when(marketLoyaltyClient.saveWalletBunchRequest(any(WalletBunchSaveRequest.class))).thenReturn(BUNCH_ID);
    }

    private void prepareCoinRequestFolder() {
        when(mockedYtFolders.getCoinRequestFolder()).thenReturn(YPath.cypressRoot());
    }
}
