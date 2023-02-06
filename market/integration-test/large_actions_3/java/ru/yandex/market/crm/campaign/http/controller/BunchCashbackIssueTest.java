package ru.yandex.market.crm.campaign.http.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.RandomUtils;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.Action;
import ru.yandex.market.crm.campaign.domain.actions.ActionStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCashbackStep;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.LoyaltyTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.external.loyalty.BunchCheckResponse;
import ru.yandex.market.crm.external.loyalty.LoyaltyUtils;
import ru.yandex.market.crm.external.loyalty.WalletBunchSaveRequest;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.loyalty.Cashback;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.util.ActionVars;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;

public class BunchCashbackIssueTest extends AbstractControllerLargeTest {

    private static final long CASHBACK_AMOUNT = 123L;
    private static final String CONFIRMATION_TIME = "2021-03-29T16:53:52.601+0300";

    private IssueCashbackStep step;
    private Action action;
    private YPath generatedCashbackTable;

    // Идентификатор должен быть новым для каждого теста
    private final long promoId = RandomUtils.nextIntInRange(0, 100_000_000);

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private YtSchemaTestHelper schemaTestHelper;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private YtClient ytClient;

    @Inject
    private StepsStatusDAO stepsStatusDAO;

    @Inject
    private LoyaltyTestHelper loyaltyTestHelper;

    @BeforeEach
    public void setUp() {
        schemaTestHelper.preparePlusDataTable();
        schemaTestHelper.prepareAccessDataTables();
        schemaTestHelper.prepareMetrikaAppFactsTable();
        schemaTestHelper.preparePassportProfilesTable();
        schemaTestHelper.prepareEmailOwnershipFactsTable();
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.PUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.YUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCampaignDir();
        schemaTestHelper.prepareSubscriptionFactsTable();
    }

    /**
     * Смоук тест выдачи кешбэка
     */
    @Test
    public void testIssueCashbackForPUIDStep() throws Exception {
        prepareActionAndStep(CASHBACK_AMOUNT);
        List<StepOutputRow> cashbackToIssue = Arrays.asList(
                outputRow(UidType.PUID, "1"),
                outputRow(UidType.PUID, "2"),
                outputRow(UidType.PUID, "3"),
                outputRow(UidType.PUID, "4"),
                outputRow(UidType.PUID, "5")
        );
        prepareSegmentationResult(cashbackToIssue);

        AtomicInteger saveRequestCounter = new AtomicInteger(0);
        AtomicInteger statusRequestCounter = new AtomicInteger(0);
        loyaltyTestHelper.issueCashbackBunch("982734",
                this.bunchAuthRequestValidator(saveRequestCounter, 5),
                bunchId -> {
                    statusRequestCounter.incrementAndGet();
                    issueCashback(generatedCashbackTable, cashbackToIssue);
                    var response = new BunchCheckResponse();
                    response.setStatus("OK");
                    response.setProcessedCount(5);
                    response.setErrors(Collections.emptyMap());
                    return response;
                }
        );

        requestLaunch();
        waitForStepStatus(step.getId(), this::stepIsFinished);

        Assertions.assertEquals(1, saveRequestCounter.get());
        Assertions.assertEquals(1, statusRequestCounter.get());

        IssueBunchStepStatus status = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
        Assertions.assertEquals(Integer.valueOf(5), status.getPlannedCount());
        Assertions.assertEquals(Integer.valueOf(5), status.getIssuedCount());

        List<StepOutputRow> result = readResults(step);
        Assertions.assertEquals(5, result.size());
        assertCashbackVarFilled(result, 1);
    }

    private void assertCashbackVarFilled(List<StepOutputRow> result, Integer expectedCashbackIssues) {
        for (StepOutputRow row : result) {
            StepOutputRow.Data data = row.getData();
            Assertions.assertNotNull(data);

            Map<String, YTreeNode> vars = data.getVars();
            Assertions.assertNotNull(vars);

            YTreeNode cashbackList = vars.get(ActionVars.CASHBACK_ISSUES);
            Assertions.assertNotNull(cashbackList);

            Assertions.assertTrue(cashbackList.isListNode());
            Optional.ofNullable(expectedCashbackIssues)
                    .ifPresent(expectedCoins1 -> Assertions.assertEquals(expectedCashbackIssues.intValue(),
                            cashbackList.asList().size()));

            for (YTreeNode cashbackNode : cashbackList.asList()) {
                var cashback = Cashback.deserialize(cashbackNode);

                Assertions.assertEquals(CASHBACK_AMOUNT, cashback.getAmount());
                Assertions.assertEquals(LocalDateTime.parse(CONFIRMATION_TIME, DATETIME_FORMATTER),
                        cashback.getConfirmationDate());
            }
        }
    }

    private static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOffset("+HHmm", "")
            .toFormatter();

    private void issueCashback(YPath path, List<StepOutputRow> cashback) {
        List<YTreeMapNode> issuedCashback = cashback.stream()
                .map(this::intoIssuedCashback)
                .collect(Collectors.toList());
        ytClient.write(path, YTreeMapNode.class, issuedCashback);
    }

    private YTreeMapNode intoIssuedCashback(StepOutputRow data) {
        return YTree.mapBuilder()
                .key("uid").value(Long.parseLong(data.getIdValue()))
                .key("amount").value(CASHBACK_AMOUNT)
                .key("confirmation_time").value(CONFIRMATION_TIME)
                .key("reference_id").value(data.getIdValue())
                .buildMap();
    }

    private Consumer<WalletBunchSaveRequest> bunchAuthRequestValidator(AtomicInteger counter, int saveCount) {
        return bunchSaveRequest -> {
            counter.incrementAndGet();
            Assertions.assertEquals(action.getOuterId() + "_" + step.getId(), bunchSaveRequest.getCampaignName());
            Assertions.assertFalse(bunchSaveRequest.getUniqueKey().isEmpty());
            Assertions.assertEquals(tableName(), bunchSaveRequest.getInput());
            Assertions.assertEquals(tableName(), bunchSaveRequest.getOutput());
            Assertions.assertEquals(tableName() + "_errors", bunchSaveRequest.getErrorsOutput());
            Assertions.assertEquals(Long.valueOf(promoId), bunchSaveRequest.getPromoId());
            Assertions.assertEquals("", bunchSaveRequest.getPromoAlias());
            Assertions.assertEquals(Integer.valueOf(saveCount), bunchSaveRequest.getCount());
            Assertions.assertEquals(LoyaltyUtils.OUTPUT_FORMAT_YT, bunchSaveRequest.getOutputFormat());
            Assertions.assertNull(bunchSaveRequest.getEmail());
        };
    }

    private void prepareActionAndStep(long cashbackAmount) {
        step = ActionTestHelper.issueCashback(promoId, cashbackAmount);
        action = prepareAction(step);

        generatedCashbackTable = coinTable("output", tableName());
    }

    private YPath coinTable(String dir, String name) {
        return ytFolders.getCoinRequestFolder().child(dir).child(name);
    }

    private String tableName() {
        return String.join("_", action.getId(), step.getId());
    }

    private void prepareSegmentationResult(List<StepOutputRow> rows) {
        actionTestHelper.prepareSegmentationResult(action.getId(), rows);
    }

    private Action prepareAction(ActionStep... steps) {
        return actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
    }

    private void requestLaunch() throws Exception {
        mockMvc.perform(
                post("/api/actions/{actionId}/steps/{stepId}/launch", action.getId(), step.getId())
        )
                .andExpect(status().isOk())
                .andDo(print());
    }

    private void waitForStepStatus(String stepId, Function<StepStatus<?>, Boolean> stepStatusMatcher) throws InterruptedException {
        actionTestHelper.waitFor(action.getId(), Duration.ofMinutes(25), receivedAction -> {
            ActionStatus actionStatus = receivedAction.getStatus();
            Assertions.assertNotNull(actionStatus);

            StepStatus<?> stepStatus = actionStatus.getSteps().get(stepId);
            if (stepStatus == null) {
                return false;
            }

            return stepStatusMatcher.apply(stepStatus);
        });
    }

    private List<StepOutputRow> readResults(IssueCashbackStep step) {
        return ytClient.read(
                actionTestHelper.getStepOutputPath(action.getId(), step.getId()),
                StepOutputRow.class
        );
    }

    private boolean stepIsFinished(StepStatus<?> stepStatus) {
        StageStatus stageStatus = stepStatus.getStageStatus();

        Assertions.assertTrue(
                stageStatus == StageStatus.IN_PROGRESS || stageStatus == StageStatus.FINISHED,
                "Step status is " + stageStatus);

        return stageStatus == StageStatus.FINISHED;
    }
}
