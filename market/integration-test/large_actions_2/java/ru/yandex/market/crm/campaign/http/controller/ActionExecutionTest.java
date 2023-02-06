package ru.yandex.market.crm.campaign.http.controller;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionStatus;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.conditions.ActionSegmentGroupPart;
import ru.yandex.market.crm.campaign.domain.actions.conditions.SegmentConditionPart;
import ru.yandex.market.crm.campaign.domain.actions.status.BuildSegmentStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.FoldByCryptaStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.ActionYtPaths;
import ru.yandex.market.crm.campaign.services.actions.PlainActionsService;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.loggers.TestExecutedActionsLogger;
import ru.yandex.market.crm.campaign.test.loggers.TestExecutedActionsLogger.LogEntry;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.core.domain.segment.Condition;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentAlgorithmPart;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper.IdRelation;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.services.actions.ActionConstants.SEGMENT_STEP_ID;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.DEFAULT_VARIANT;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.foldByCrypta;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.mobileUsersCondition;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.subscribedCondition;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.variant;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.accessMarketFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.accessEntry;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.plusData;

/**
 * @author apershukov
 */
public class ActionExecutionTest extends AbstractControllerLargeTest {

    private static Set<Uid> extractUids(Collection<StepOutputRow> rows) {
        return rows.stream()
                .map(row -> Uid.of(row.getIdType(), row.getIdValue()))
                .collect(Collectors.toSet());
    }

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;
    private static final long PUID_4 = 444;

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";

    private static final String YUID_1 = "111";
    private static final String YUID_2 = "222";
    private static final String YUID_3 = "333";
    private static final String YUID_4 = "444";

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";
    private static final String DEVICE_ID_3 = "device_id_3";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";
    private static final String DEVICE_ID_HASH_3 = "device_id_hash_3";

    private static final String EMAIL_1 = "email_1@yandex-team.ru";
    private static final String EMAIL_2 = "email_2@yandex-team.ru";

    @Inject
    private SegmentService segmentService;

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private YtSchemaTestHelper schemaTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private YtClient ytClient;

    @Inject
    private PlainActionsService actionsService;

    @Inject
    private TestExecutedActionsLogger executedActionsLogger;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @BeforeEach
    public void setUp() {
        schemaTestHelper.prepareUserTables();
        schemaTestHelper.preparePlusDataTable();
        schemaTestHelper.prepareAccessDataTables();
        schemaTestHelper.preparePassportProfilesTable();
        schemaTestHelper.prepareEmailOwnershipFactsTable();
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.PUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.YUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareGlobalControlSplitsTable();
        schemaTestHelper.prepareCampaignDir();
        schemaTestHelper.prepareSubscriptionFactsTable();
        schemaTestHelper.prepareChytPassportUuidsTable();
        schemaTestHelper.prepareChytPassportEmailsTable();
        schemaTestHelper.prepareChytUuidsWithTokensTable();
        schemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    /**
     * Выполнение акции, состоящей только из шага сегментации
     */
    @Test
    public void testExecuteActionWithSegmentStepOnly() throws Exception {
        Segment segment = prepareSegment(
                plusFilter()
        );

        userTestHelper.addPlusData(
                plusData(PUID_1),
                plusData(PUID_2)
        );

        PlainAction action = prepareAction(segment.getId());

        requestExecution(action);
        waitExecuted(action.getId(), Collections.singleton(SEGMENT_STEP_ID));

        var segmentStepDir = getSegmentStepDirectory(action.getId());
        YPath resultPath = segmentStepDir.child(DEFAULT_VARIANT);
        List<StepOutputRow> rows = ytClient.read(resultPath, StepOutputRow.class);

        assertEquals(2, rows.size());

        assertEquals(PUID_1, Long.parseLong(rows.get(0).getIdValue()));
        assertEquals(UidType.PUID, rows.get(0).getIdType());
        assertNotNull(rows.get(0).getData());
        assertNotNull(rows.get(0).getData().getVars());

        assertEquals(PUID_2, Long.parseLong(rows.get(1).getIdValue()));
        assertEquals(UidType.PUID, rows.get(1).getIdType());
        assertNotNull(rows.get(1).getData());
        assertNotNull(rows.get(1).getData().getVars());

        action = actionsService.getAction(action.getId());
        BuildSegmentStepStatus stepStatus = (BuildSegmentStepStatus) action.getStatus()
                .getSteps().get(SEGMENT_STEP_ID);
        assertNotNull(stepStatus);

        Map<UidType, Long> counts = stepStatus.getCounts();
        assertNotNull(counts);
        assertEquals(2L, (long) counts.get(UidType.PUID));

        assertExpirationTimeIsNotSet(segmentStepDir);
    }

    /**
     * Выполнение акции с шагом "Дедубликация по крипте"
     */
    @Test
    public void testExecuteActionWithFoldByCryptaStep() throws Exception {
        userTestHelper.addFapiAccessLogEntries(
                accessEntry(null, PUID_1),
                accessEntry(null, PUID_2),
                accessEntry(YUID_1, null),
                accessEntry(YUID_2, null)
        );

        userTestHelper.saveLinks(String.valueOf(PUID_1), "puid", "111", "crypta_id");
        userTestHelper.saveLinks(YUID_1, "yandexuid", "111", "crypta_id");
        userTestHelper.saveLinks(YUID_2, "yandexuid", "222", "crypta_id");

        Segment segment = prepareSegment(
                accessMarketFilter()
        );

        ActionStep foldByCrypta = foldByCrypta();
        PlainAction action = prepareAction(segment.getId(), foldByCrypta);

        List<StepOutputRow> rows = executeSingleVariantAction(action);

        Set<Uid> uids = extractUids(rows);

        Set<Uid> expected = ImmutableSet.of(
                Uid.asPuid(PUID_1),
                Uid.asPuid(PUID_2),
                Uid.asYuid(YUID_2)
        );

        assertEquals(expected, uids);

        var actionId = action.getId();
        action = actionsService.getAction(actionId);

        FoldByCryptaStepStatus stepStatus = (FoldByCryptaStepStatus) action.getStatus().getSteps()
                .get(foldByCrypta.getId());
        assertNotNull(stepStatus);

        Map<UidType, Long> counts = stepStatus.getCounts();
        assertNotNull(counts);
        assertEquals(2L, (long) counts.get(UidType.PUID));
        assertEquals(1L, (long) counts.get(UidType.YUID));

        var paths = new ActionYtPaths(ytFolders.getActionPath(actionId));
        assertExpirationTimeIsNotSet(paths.getStepDirectory(SEGMENT_STEP_ID));
        assertExpirationTimeIsNotSet(paths.getStepDirectory(foldByCrypta.getId()));
    }

    /**
     * Выполнение акции с двумя вариантами и контрольной группой
     */
    @Test
    public void testExecuteActionWithVariants() throws Exception {
        YTreeMapNode[] plusAccounts = LongStream.rangeClosed(1, 50)
                .mapToObj(UserTestHelper::plusData)
                .toArray(YTreeMapNode[]::new);

        userTestHelper.addPlusData(plusAccounts);

        Segment segment = prepareSegment(
                plusFilter()
        );

        ActionStep step1 = foldByCrypta();
        ActionStep step2 = foldByCrypta();

        PlainAction action = actionTestHelper.prepareActionWithVariants(segment.getId(), LinkingMode.NONE,
                variant("variant_a", 40, step1),
                variant("variant_b", 40, step2)
        );

        Map<String, List<StepOutputRow>> results = executeAction(action);

        Map<String, Set<Uid>> ids = results.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        e -> e.getValue().stream()
                                .map(x -> Uid.of(x.getIdType(), x.getIdValue()))
                                .collect(Collectors.toSet())
                ));

        Set<Uid> ids1 = ids.get(step1.getId());
        assertNotNull(ids1);
        Assertions.assertFalse(ids1.isEmpty());

        Set<Uid> ids2 = ids.get(step2.getId());
        assertNotNull(ids2);
        Assertions.assertFalse(ids2.isEmpty());

        Assertions.assertTrue(ids1.size() + ids2.size() < plusAccounts.length, "Control group is empty");

        List<LogEntry> entries = executedActionsLogger.getEntries();
        assertEquals(plusAccounts.length, entries.size());

        for (LogEntry entry : entries) {
            Uid uid = entry.getUid();

            if (ids1.contains(uid)) {
                assertEquals("variant_a", entry.getVariant());
            } else if (ids2.contains(uid)) {
                assertEquals("variant_b", entry.getVariant());
            } else {
                assertEquals(action.getId() + "_control", entry.getVariant());
            }
        }
    }

    /**
     * Идентфикаторы связанные с одним и тем же crypta id попадают в один вариант
     */
    @Test
    public void testDistributeVariantsByCryptaIds() throws Exception {
        long[] puids = LongStream.rangeClosed(1, 10).toArray();

        userTestHelper.addPlusData(
                LongStream.of(puids)
                        .mapToObj(UserTestHelper::plusData)
                        .toArray(YTreeMapNode[]::new)
        );

        userTestHelper.saveLinks(UserTestHelper.PUID, UserTestHelper.CRYPTA_ID,
                LongStream.of(puids)
                        .mapToObj(String::valueOf)
                        .map(puid -> new IdRelation(puid, "crypta_id"))
                        .toArray(IdRelation[]::new)
        );

        Segment segment = prepareSegment(
                plusFilter()
        );

        PlainAction action = actionTestHelper.prepareActionWithVariants(segment.getId(), LinkingMode.NONE,
                variant("variant_a", 50),
                variant("variant_b", 50)
        );

        requestExecution(action);
        waitExecuted(action.getId(), Collections.singleton(SEGMENT_STEP_ID));

        YPath stepDirectory = getSegmentStepDirectory(action.getId());
        List<StepOutputRow> variantARows = ytClient.read(stepDirectory.child("variant_a"), StepOutputRow.class);
        List<StepOutputRow> variantBRows = ytClient.read(stepDirectory.child("variant_b"), StepOutputRow.class);

        Assertions.assertTrue(
                variantARows.size() == puids.length || variantBRows.size() == puids.length,
                "Ids are in different variants");
    }

    /**
     * Шаг дедубликация помимо данных из крипты использует данные из LiluCRM
     */
    @Disabled
    @Test
    public void testFoldByCrmData() throws Exception {
        User user1 = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asPuid(PUID_1))
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asYuid(YUID_2))
                                .addEdge(0, 1)
                                .addEdge(0, 2)
                );

        User user2 = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_3))
                                .addNode(Uid.asYuid(YUID_4))
                                .addEdge(0, 1)
                );

        userTestHelper.addUsers(user1, user2);
        userTestHelper.finishUsersPreparation();

        ActionStep foldByCrypta = foldByCrypta();
        PlainAction action = actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, foldByCrypta);

        prepareSegmentationResult(action.getId(), Arrays.asList(
                outputRow(UidType.PUID, String.valueOf(PUID_1)),
                outputRow(UidType.PUID, String.valueOf(PUID_2)),
                outputRow(UidType.YUID, YUID_1),
                outputRow(UidType.YUID, YUID_2),
                outputRow(UidType.YUID, YUID_3),
                outputRow(UidType.YUID, YUID_4)
        ));

        List<Uid> results = execute(action, foldByCrypta).stream()
                .map(x -> Uid.of(x.getIdType(), x.getIdValue()))
                .collect(Collectors.toList());

        assertEquals(3, results.size(), "Invalid result size. Result: " + results);

        Assertions.assertTrue(results.contains(Uid.asPuid(PUID_1)));
        Assertions.assertTrue(results.contains(Uid.asPuid(PUID_2)));
        Assertions.assertTrue(results.contains(Uid.asYuid(YUID_3)) || results.contains(Uid.asYuid(YUID_4)));
    }

    /**
     * При включенной дедубликации в конфиге акции схлапываются идентификаторы с одним crypta id,
     * при этом корректно сохранются счетчики по идентификаторам в статусе шага построения сегмента
     */
    @Test
    public void testFoldByCrypta() throws Exception {
        userTestHelper.addFapiAccessLogEntries(
                accessEntry(null, PUID_1),
                accessEntry(null, PUID_2),
                accessEntry(YUID_1, null),
                accessEntry(YUID_2, null)
        );

        userTestHelper.saveLinks(String.valueOf(PUID_1), "puid", "111", "crypta_id");
        userTestHelper.saveLinks(YUID_1, "yandexuid", "111", "crypta_id");
        userTestHelper.saveLinks(YUID_2, "yandexuid", "222", "crypta_id");

        var segment = prepareSegment(
                accessMarketFilter()
        );

        var action = prepareAction(segment.getId());
        actionTestHelper.enableFoldByCrypta(action);

        requestExecution(action);
        waitExecuted(action.getId(), Collections.singleton(SEGMENT_STEP_ID));

        var resultPath = getSegmentStepDirectory(action.getId()).child(DEFAULT_VARIANT);
        var rows = ytClient.read(resultPath, StepOutputRow.class);

        var uids = extractUids(rows);

        var expected = ImmutableSet.of(
                Uid.asPuid(PUID_1),
                Uid.asPuid(PUID_2),
                Uid.asYuid(YUID_2)
        );

        assertEquals(expected, uids);

        action = actionsService.getAction(action.getId());

        var stepStatus = (BuildSegmentStepStatus) action.getStatus()
                .getSteps().get(SEGMENT_STEP_ID);
        assertNotNull(stepStatus);

        var variant = action.getConfig().getVariants().get(0).getId();
        var counts = stepStatus.getInResultByVariantCounts().get(variant);
        assertNotNull(counts);
        assertEquals(2, counts.get(UidType.PUID));
        assertEquals(1, counts.get(UidType.YUID));

        var discardedByFoldingCounts = stepStatus.getDiscardedByFoldingCounts();
        assertNotNull(discardedByFoldingCounts);
        Assertions.assertNull(discardedByFoldingCounts.get(UidType.PUID));
        assertEquals(1, discardedByFoldingCounts.get(UidType.YUID));
    }

    /**
     * При установленной фильтрации по условиям в конфиге акции остаются те идентификаторы,
     * которые удовлетворяют условиям фильтра, при этом корректно сохранются счетчики по идентификаторам
     * в статусе шага построения сегмента
     */
    @Test
    public void testMultifilterInBuildSegmentStep() throws Exception {
        userTestHelper.addFapiAccessLogEntries(
                accessEntry(null, PUID_1),
                accessEntry(null, PUID_2),
                accessEntry(null, PUID_3),
                accessEntry(null, PUID_4),
                accessEntry(YUID_1, null)
        );

        var segment = prepareSegment(
                accessMarketFilter()
        );

        var segmentConfig = new ActionSegmentGroupPart()
                .setCondition(Condition.ALL)
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(mobileUsersCondition())
                )
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(subscribedCondition())
                );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, ADVERTISING, PUID_1),
                subscription(EMAIL_2, ADVERTISING)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        var user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asEmail(EMAIL_2))
                                .addEdge(0, 1)
                );

        userTestHelper.addUsers(user);
        userTestHelper.finishUsersPreparation();

        var action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);
        action.getConfig().setMultifilterSegmentConfig(segmentConfig);
        actionTestHelper.updateAction(action);

        requestExecution(action);
        waitExecuted(action.getId(), Collections.singleton(SEGMENT_STEP_ID));

        var resultPath = getSegmentStepDirectory(action.getId()).child(DEFAULT_VARIANT);
        var rows = ytClient.read(resultPath, StepOutputRow.class);
        var uids = extractUids(rows);
        Assertions.assertEquals(Set.of(Uid.asPuid(PUID_1)), uids);

        action = actionsService.getAction(action.getId());

        var stepStatus = (BuildSegmentStepStatus) action.getStatus()
                .getSteps().get(SEGMENT_STEP_ID);
        Assertions.assertNotNull(stepStatus);

        var variant = action.getConfig().getVariants().get(0).getId();
        var counts = stepStatus.getInResultByVariantCounts().get(variant);
        Assertions.assertNotNull(counts);
        Assertions.assertEquals(1, counts.get(UidType.PUID));
        Assertions.assertNull(counts.get(UidType.YUID));

        var discardedByMultifilterCounts = stepStatus.getDiscardedByMultifilterCounts();
        Assertions.assertNotNull(discardedByMultifilterCounts);
        Assertions.assertEquals(3, discardedByMultifilterCounts.get(UidType.PUID));
        Assertions.assertEquals(1, discardedByMultifilterCounts.get(UidType.YUID));
    }

    private List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step);
    }

    private List<StepOutputRow> executeSingleVariantAction(PlainAction action) throws Exception {
        return executeAction(action).values().iterator().next();
    }

    private Map<String, List<StepOutputRow>> executeAction(PlainAction action) throws Exception {
        requestExecution(action);

        ActionConfig config = action.getConfig();
        List<ActionVariant> variants = config.getVariants();

        Set<String> lastStepIds = getLastSteps(variants);

        waitExecuted(action.getId(), lastStepIds);

        return lastStepIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        stepId -> {
                            YPath output = getStepOutputPath(action.getId(), stepId);
                            return ytClient.read(output, StepOutputRow.class);
                        }
                ));
    }

    private Set<String> getLastSteps(List<ActionVariant> variants) {
        if (CollectionUtils.isEmpty(variants)) {
            return Collections.singleton(SEGMENT_STEP_ID);
        }

        return variants.stream()
                .map(variant -> {
                    List<ActionStep> steps = variant.getSteps();
                    return CollectionUtils.isEmpty(variant.getSteps())
                            ? SEGMENT_STEP_ID
                            : steps.get(steps.size() - 1).getId();
                })
                .collect(Collectors.toSet());
    }

    private Segment prepareSegment(SegmentAlgorithmPart... parts) {
        Segment segment = segment(parts);
        segmentService.addSegment(segment);
        return segment;
    }

    private void waitExecuted(String actionId, Set<String> lastStepIds) throws InterruptedException {
        actionTestHelper.waitFor(actionId, Duration.ofMinutes(25), action -> {
            ActionStatus status = action.getStatus();
            assertNotNull(status);

            return lastStepIds.stream()
                    .allMatch(stepId -> {
                        StepStatus<?> stepStatus = status.getSteps().get(stepId);

                        if (stepStatus == null) {
                            return false;
                        }

                        StageStatus stageStatus = stepStatus.getStageStatus();

                        Assertions.assertTrue(
                                stageStatus == StageStatus.IN_PROGRESS || stageStatus == StageStatus.FINISHED,
                                "Step status is " + stageStatus);

                        return stageStatus == StageStatus.FINISHED;
                    });
        });
    }

    private void requestExecution(PlainAction action) throws Exception {
        requestLaunch(action.getId(), SEGMENT_STEP_ID);
    }

    private void requestLaunch(String actionId, String stepId) throws Exception {
        mockMvc.perform(
                post("/api/actions/{actionId}/steps/{stepId}/launch", actionId, stepId)
        )
                .andExpect(status().isOk())
                .andDo(print());
    }

    private YPath getSegmentStepDirectory(String actionId) {
        return actionTestHelper.getStepDirectory(actionId, SEGMENT_STEP_ID);
    }

    private YPath getStepOutputPath(String actionId, String stepId) {
        return actionTestHelper.getStepOutputPath(actionId, stepId);
    }

    private void prepareSegmentationResult(String actionId, List<StepOutputRow> rows) {
        actionTestHelper.prepareSegmentationResult(actionId, rows);
    }

    private PlainAction prepareAction(String segmentId, ActionStep... steps) {
        return actionTestHelper.prepareAction(segmentId, LinkingMode.NONE, steps);
    }

    private void assertExpirationTimeIsNotSet(YPath path) {
        var expirationTimeIsSet = ytClient.getAttribute(path, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode)
                .isPresent();

        assertFalse(expirationTimeIsSet, "Expiration time is set");
    }
}
