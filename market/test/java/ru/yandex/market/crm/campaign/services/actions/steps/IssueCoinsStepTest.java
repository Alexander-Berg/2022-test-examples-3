package ru.yandex.market.crm.campaign.services.actions.steps;

import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.services.actions.contexts.CoinIssuanceStepContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.ActionExecutionContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.PlainStepStatusUpdaterImpl;
import ru.yandex.market.crm.campaign.services.email.NotificationEmailSender;
import ru.yandex.market.crm.campaign.test.StepStatusDAOMock;
import ru.yandex.market.crm.campaign.test.StepStatusUpdaterMock;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.external.loyalty.BunchCheckResponse;
import ru.yandex.market.crm.external.loyalty.CoinBunchSaveRequest;
import ru.yandex.market.crm.external.loyalty.LoyaltyCoinType;
import ru.yandex.market.crm.external.loyalty.MarketLoyaltyClient;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.TaskStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssueCoinsStepTest {
    private static final int PLANNED_COUNT = 100;
    private static final long PROMO_ID = 13L;

    private final MarketLoyaltyClient marketLoyaltyClient = mock(MarketLoyaltyClient.class);
    private final NotificationEmailSender emailSender = mock(NotificationEmailSender.class);
    private StepsStatusDAO stepsStatusDAO;
    private String bunchId;
    private IssueCoinsStep issueCoinsStep;
    private PlainAction action;
    private IssueBunchStepStatus statusAfter;
    private ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep step;
    private CoinIssuanceStepContext stepContext;
    private final Control<Void> control = mock(Control.class);
    private PlainStepStatusUpdaterImpl stepStatusUpdater;

    private static class FakeBunchIssueRequestBuilder extends BunchIssueRequestBuilder {
        FakeBunchIssueRequestBuilder() {
            super(false);
        }

        @Override
        public CoinBunchSaveRequest build(CoinIssuanceStepContext context, LoyaltyCoinType coinType, int plannedCount) {
            return new CoinBunchSaveRequest();
        }
    }

    @Before
    public void setUp() {
        stepsStatusDAO = new StepStatusDAOMock();
        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.DIRECT_ONLY, "segment_id"));

        bunchId = "77777";
        prepareSaveResponse();
        action = new PlainAction();
        action.setId("action");
        action.setConfig(config);

        step = new ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep();
        step.setPromoId(PROMO_ID);

        stepStatusUpdater = new PlainStepStatusUpdaterImpl(action.getId(), step.getId(),
                new StepStatusUpdaterMock(stepsStatusDAO));

        buildIssueCoinsStep(LoyaltyCoinType.AUTH);
    }

    /**
     * Для нулевого числа монет делать запрос бессмысленно, поэтому не делаем его.
     */
    @Test
    public void doNotProcessRequestWhenCountIsZero() throws Exception {
        saveStatus(initialStatus()
                .setPlannedCount(LoyaltyCoinType.AUTH.name(), 0));

        ExecutionResult result = runStep();
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());
        // Обращений в сервис loyalty быть не должно
        verify(marketLoyaltyClient, never()).saveCoinBunchRequest(any(CoinBunchSaveRequest.class));
        verify(marketLoyaltyClient, never()).checkCoinBunchRequestStatus(anyString());
    }

    /**
     * Перед запуском шага ещё неизвестен bunchId, поэтому шаг должен сделать запрос на генерацию
     * пачек монеток и получить в ответ этот bunchId.
     */
    @Test
    public void startIsRequestedWhenBunchIdIsUnknown() throws Exception {
        saveStatus(initialStatus()
                .setBunchId(null));
        ExecutionResult result = runStep();
        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertEquals(bunchId, statusAfter.getBunchId());
        assertEquals(Integer.valueOf(0), statusAfter.getProcessedCount());
    }

    /**
     * Все AUTH монетки сразу сгенерировались за один раунд
     */
    @Test
    public void testIssueSuccessfulForAuth() throws Exception {
        statusResponseIs(ok(PLANNED_COUNT));

        saveStatus(alreadyStartedStatus());
        ExecutionResult result = runStep();
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());
        assertEquals(Integer.valueOf(PLANNED_COUNT), statusAfter.getProcessedCount());
    }

    /**
     * Все NO_AUTH монетки сразу сгенерировались за один раунд
     */
    @Test
    public void testIssueSuccessfulForNonAuth() throws Exception {
        buildIssueCoinsStep(LoyaltyCoinType.NO_AUTH);
        statusResponseIs(ok(PLANNED_COUNT));

        saveStatus(alreadyStartedStatus()
                // надо игнорировать данные по другому типу монет
                .setPlannedCount(LoyaltyCoinType.AUTH.name(), 2 * PLANNED_COUNT)
                .setPlannedCount(LoyaltyCoinType.NO_AUTH.name(), PLANNED_COUNT));
        ExecutionResult result = runStep();
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());
        assertEquals(Integer.valueOf(PLANNED_COUNT), statusAfter.getProcessedCount());
    }

    /**
     * Сгенерировалась только часть монеток, но статус OK.
     */
    @Test
    public void testRepeatWhenNotDoneAndOk() throws Exception {
        statusResponseIs(ok(PLANNED_COUNT / 2));
        saveStatus(alreadyStartedStatus());
        ExecutionResult result = runStep();
        assertEquals(TaskStatus.WAITING, result.getNextStatus());
    }

    /**
     * Сгенерировались все монетки, но статус PROCESSING.
     */
    @Test
    public void testRepeatWhenNotDoneAndProcessing() throws Exception {
        statusResponseIs(processing(10));
        saveStatus(alreadyStartedStatus());
        ExecutionResult result = runStep();
        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertEquals(Integer.valueOf(10), statusAfter.getProcessedCount());
    }

    /**
     * Число обработанных и выпущенных монет по нескольким запросам складывается друг с другом
     */
    @Test
    public void sumProcessedCoinsForSeveralRequests() throws Exception {
        statusResponseIs(ok(10));
        saveStatus(initialStatus()
                .setPlannedCount(LoyaltyCoinType.AUTH.name(), 10)
                .setPlannedCount(LoyaltyCoinType.NO_AUTH.name(), 4));
        runStep();      // первый запрос в loyalty
        runStep();      // получили результат
        assertEquals(Integer.valueOf(10), statusAfter.getProcessedCount());

        bunchId = "777";
        prepareSaveResponse();
        statusResponseIs(ok(4));
        buildIssueCoinsStep(LoyaltyCoinType.NO_AUTH);
        runStep();      // второй запрос в loyalty
        runStep();      // получили результат
        assertEquals(Integer.valueOf(14), statusAfter.getProcessedCount());
    }

    /**
     * Ошибка генерации: код ERROR. Шаг падает с ошибкой, поскольку Лоялти таблицу не выгружают в таком случае
     */
    @Test
    public void testErrorCode() throws Exception {
        statusResponseIs(error("ERROR"));

        saveStatus(alreadyStartedStatus());
        ExecutionResult result = runStep();
        assertEquals(TaskStatus.FAILING, result.getNextStatus());
    }

    /**
     * Ошибки во время генерации
     */
    @Test
    public void testRuntimeErrors() throws Exception {
        BunchCheckResponse statusResponse = ok(PLANNED_COUNT / 2);
        statusResponse.setErrors(Map.of("WRONG_ID", 3));
        statusResponseIs(statusResponse);

        saveStatus(alreadyStartedStatus());
        ExecutionResult result = runStep();
        assertEquals(TaskStatus.WAITING, result.getNextStatus());

        assertEquals(Integer.valueOf(3), statusAfter.getErrorCounts().get("WRONG_ID"));
    }

    /**
     * Ошибки во время генерации добавляются к существующим
     */
    @Test
    public void testMergeRuntimeErrors() throws Exception {
        saveStatus(alreadyStartedStatus()
                .setErrorCounts(Map.of("WRONG_ID", 1, "UFO_DETECTED", 0)));
        BunchCheckResponse statusResponse = ok(PLANNED_COUNT / 2);
        statusResponse.setErrors(Map.of("WRONG_ID", 3));
        statusResponseIs(statusResponse);

        ExecutionResult result = runStep();
        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertEquals(Integer.valueOf(4), statusAfter.getErrorCounts().get("WRONG_ID"));
    }

    private void prepareSaveResponse() {
        when(marketLoyaltyClient.saveCoinBunchRequest(any(CoinBunchSaveRequest.class))).thenReturn(bunchId);
    }

    private void buildIssueCoinsStep(LoyaltyCoinType coinType) {
        issueCoinsStep = new IssueCoinsStep(
                coinType,
                marketLoyaltyClient,
                new FakeBunchIssueRequestBuilder(),
                emailSender,
                "https://test_url.ru"
        );
    }

    /**
     * Во многих тестах надо проверять только шаг check, как будто бы запрос на генерацию уже был сделан.
     * Для них используется этот метод
     */
    private IssueBunchStepStatus alreadyStartedStatus() {
        return initialStatus().setBunchId(bunchId);
    }

    private IssueBunchStepStatus initialStatus() {
        return new IssueBunchStepStatus()
                .setStepId(step.getId())
                .setPlannedCount(LoyaltyCoinType.AUTH.name(), PLANNED_COUNT)
                .setPlannedCount(LoyaltyCoinType.NO_AUTH.name(), 0)
                .setProcessedCount(LoyaltyCoinType.AUTH.name(), 0)
                .setProcessedCount(LoyaltyCoinType.NO_AUTH.name(), 0);
    }

    private void saveStatus(IssueBunchStepStatus newStatus) {
        stepsStatusDAO.upsert(action.getId(), newStatus);
        ActionExecutionContext parentContext = new ActionExecutionContext(action, step, newStatus,
                stepStatusUpdater, null, null, null,
                action.getOuterId() + "_" + step.getId());
        stepContext = new CoinIssuanceStepContext(parentContext, mock(YtFolders.class));
    }

    private ExecutionResult runStep() throws Exception {
        ExecutionResult result = issueCoinsStep.run(stepContext, null, control);
        statusAfter = loadSavedStatus();
        return result;
    }

    private void statusResponseIs(BunchCheckResponse response) {
        when(marketLoyaltyClient.checkCoinBunchRequestStatus(eq(bunchId))).thenReturn(response);
    }

    @NotNull
    private BunchCheckResponse ok(int processedCount) {
        BunchCheckResponse statusResponse = new BunchCheckResponse();
        statusResponse.setStatus("OK");
        statusResponse.setProcessedCount(processedCount);
        statusResponse.setErrors(Collections.emptyMap());
        return statusResponse;

    }

    @NotNull
    private BunchCheckResponse processing(int processedCount) {
        BunchCheckResponse statusResponse = new BunchCheckResponse();
        statusResponse.setStatus("PROCESSING");
        statusResponse.setProcessedCount(processedCount);
        statusResponse.setErrors(Collections.emptyMap());
        return statusResponse;
    }

    @NotNull
    private BunchCheckResponse error(String errorType) {
        BunchCheckResponse statusResponse = new BunchCheckResponse();
        statusResponse.setStatus(errorType);
        statusResponse.setErrors(Collections.emptyMap());
        return statusResponse;
    }

    private IssueBunchStepStatus loadSavedStatus() {
        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        assertNotNull(status);
        return status;
    }
}
