package ru.yandex.market.crm.campaign.http.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.conditions.ActionSegmentGroupPart;
import ru.yandex.market.crm.campaign.domain.actions.conditions.ActionSegmentPart;
import ru.yandex.market.crm.campaign.domain.actions.conditions.SegmentConditionPart;
import ru.yandex.market.crm.campaign.domain.actions.status.MultifilterStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.core.domain.segment.Condition;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.mobileUsersCondition;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.multifilter;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.subscribedCondition;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;

public class MultifilterStepTest extends AbstractControllerLargeTest {
    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;
    private static final long PUID_4 = 444;

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";
    private static final String UUID_4 = "uuid-4";

    private static final String YUID_1 = "111";
    private static final String YUID_2 = "222";

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";
    private static final String DEVICE_ID_3 = "device_id_3";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";
    private static final String DEVICE_ID_HASH_3 = "device_id_hash_3";

    private static final String EMAIL_1 = "email_1@yandex-team.ru";
    private static final String EMAIL_2 = "email_2@yandex-team.ru";
    private static final String EMAIL_3 = "email_3@yandex-team.ru";

    @Inject
    private ActionTestHelper actionTestHelper;
    @Inject
    private UserTestHelper userTestHelper;
    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private StepsStatusDAO stepsStatusDAO;
    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareUserTables();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.prepareChytPassportUuidsTable();
        ytSchemaTestHelper.prepareChytPassportEmailsTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    /**
     * После прохождения шага мультифильтрации с условием наличия мобильного приложения останутся только
     * идентфикаторы, из которых можно зарезолвить uuid приложения указанного цвета и с указанным типом подписки
     */
    @Test
    public void testMultifilterStepWithMobileUsersCondition() throws Exception {
        ActionSegmentPart segmentConfig = new ActionSegmentGroupPart()
                .setCondition(Condition.ALL)
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(mobileUsersCondition(STORE_PUSH_GENERAL_ADVERTISING))
                );

        ActionStep filterStep = multifilter(segmentConfig);
        PlainAction action = prepareAction(filterStep);

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3),
                chytUuidWithToken(UUID_4, "device_id_4", "device_id_hash_4")
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1),
                chytUuidWithSubscription(UUID_2),
                chytUuidWithSubscription(UUID_3, STORE_PUSH_GENERAL_ADVERTISING, false),
                chytUuidWithSubscription(UUID_4)
        );

        User user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asUuid(UUID_4))
                                .addEdge(0, 1)
                );

        userTestHelper.addUsers(user);
        userTestHelper.finishUsersPreparation();

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3)
        );

        prepareSegmentationResult(action.getId(), Arrays.asList(
                outputRow(UidType.PUID, String.valueOf(PUID_1)),   // Пройдет
                outputRow(UidType.PUID, String.valueOf(PUID_2)),   // Пройдет
                outputRow(UidType.PUID, String.valueOf(PUID_3)),   // Не пройдет. Есть uuid но он отписан
                outputRow(UidType.PUID, String.valueOf(PUID_4)),   // Не пройдет. Нет связанного uuid'а
                outputRow(UidType.YUID, YUID_1),                   // Пройдет
                outputRow(UidType.YUID, YUID_2),                   // Не пройдет. Нет связанного uuid'а
                outputRow(UidType.EMAIL, "user@yandex.ru") // Не пройдет. Фильтр не умеет работать с email
        ));

        Set<Uid> result = execute(action, filterStep).stream()
                .map(row -> Uid.of(row.getIdType(), row.getIdValue()))
                .collect(Collectors.toSet());

        assertEquals(Set.of(Uid.asPuid(PUID_1), Uid.asPuid(PUID_2), Uid.asYuid(YUID_1)), result);

        MultifilterStepStatus status = (MultifilterStepStatus) stepsStatusDAO
                .get(action.getId(), filterStep.getId());

        Map<UidType, Long> counts = status.getCounts();
        assertNotNull(counts);
        assertEquals(1, (long) counts.get(UidType.YUID));
        assertEquals(2, (long) counts.get(UidType.PUID));
    }

    /**
     * После прохождения шага мультифильтрации с условием подписки в акции остаются только
     * идентфикаторы, вычисляемые адреса для которых имеют указанную подписку
     */
    @Test
    public void testMultifilterStepWithSubscribedCondition() throws Exception {
        ActionSegmentPart segmentConfig = new ActionSegmentGroupPart()
                .setCondition(Condition.ALL)
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(subscribedCondition())
                );

        ActionStep filterStep = multifilter(segmentConfig);
        PlainAction action = prepareAction(filterStep);

        prepareSegmentationResult(action.getId(), Arrays.asList(
                outputRow(UidType.PUID, String.valueOf(PUID_1)),
                outputRow(UidType.PUID, String.valueOf(PUID_2)),
                outputRow(UidType.PUID, String.valueOf(PUID_3)),
                outputRow(UidType.YUID, YUID_1),
                outputRow(UidType.YUID, YUID_2)
        ));

        User user1 = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asEmail(EMAIL_1))
                                .addEdge(0, 1)
                );

        User user2 = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_2))
                                .addNode(Uid.asEmail(EMAIL_2))
                                .addEdge(0, 1)
                );

        userTestHelper.addUsers(user1, user2);
        userTestHelper.finishUsersPreparation();

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, ADVERTISING),
                subscription(EMAIL_3, ADVERTISING, PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_2, EMAIL_3)
        );

        Set<Uid> results = execute(action, filterStep).stream()
                .map(x -> Uid.of(x.getIdType(), x.getIdValue()))
                .collect(Collectors.toSet());

        assertEquals(Set.of(Uid.asPuid(PUID_2), Uid.asYuid(YUID_1)), results);

        MultifilterStepStatus status = (MultifilterStepStatus) stepsStatusDAO
                .get(action.getId(), filterStep.getId());

        Map<UidType, Long> counts = status.getCounts();
        assertNotNull(counts);
        assertEquals(1, (long) counts.get(UidType.YUID));
        assertEquals(1, (long) counts.get(UidType.PUID));
    }

    /**
     * После прохождения шага мультифильтрации останутся только идентфикаторы, которые удовлетворяют
     * условию конфигурации сегмента: есть приложение Беру и есть подписка
     */
    @Test
    public void testMultifilterStep_HasBlueAppAndSubscription() throws Exception {
        ActionSegmentPart segmentConfig = new ActionSegmentGroupPart()
                .setCondition(Condition.ALL)
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(subscribedCondition())
                )
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(mobileUsersCondition())
                );

        testMultifilterWithOtherSegmentConfigs(
                segmentConfig,
                Set.of(Uid.asPuid(PUID_1))
        );
    }

    /**
     * После прохождения шага мультифильтрации останутся только идентфикаторы, которые удовлетворяют
     * условию конфигурации сегмента: нет приложения Беру и есть подписка
     */
    @Test
    public void testMultifilterStep_NoMarketAppAndHasSubscription() throws Exception {
        ActionSegmentPart segmentConfig = new ActionSegmentGroupPart()
                .setCondition(Condition.ALL)
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(subscribedCondition())
                )
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(mobileUsersCondition())
                                .setNot(true)
                );

        testMultifilterWithOtherSegmentConfigs(
                segmentConfig,
                Set.of(Uid.asYuid(YUID_1))
        );
    }

    /**
     * После прохождения шага мультифильтрации останутся только идентификаторы, которые удовлетворяют
     * условию конфигурации сегмента: есть приложение маркета либо есть подписка
     */
    @Test
    public void testMultifilterStep_HasMarketAppOrHasSubscription() throws Exception {
        ActionSegmentPart segmentConfig = new ActionSegmentGroupPart()
                .setCondition(Condition.ANY)
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(mobileUsersCondition())
                )
                .addPart(
                        new SegmentConditionPart()
                                .setSegmentCondition(subscribedCondition())
                );

        testMultifilterWithOtherSegmentConfigs(
                segmentConfig,
                Set.of(Uid.asPuid(PUID_1), Uid.asPuid(PUID_2), Uid.asPuid(PUID_3), Uid.asYuid(YUID_1))
        );
    }

    private void prepareSegmentationResult(String actionId, List<StepOutputRow> rows) {
        actionTestHelper.prepareSegmentationResult(actionId, rows);
    }

    private void testMultifilterWithOtherSegmentConfigs(ActionSegmentPart segmentConfig,
                                                        Set<Uid> expectedResult) throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, ADVERTISING, PUID_1),
                subscription(EMAIL_3, ADVERTISING)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        User user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asEmail(EMAIL_3))
                                .addEdge(0, 1)
                );

        userTestHelper.addUsers(user);
        userTestHelper.finishUsersPreparation();

        ActionStep filterStep = multifilter(segmentConfig);
        PlainAction action = prepareAction(filterStep);

        prepareSegmentationResult(action.getId(), Arrays.asList(
                outputRow(UidType.PUID, String.valueOf(PUID_1)),
                outputRow(UidType.PUID, String.valueOf(PUID_2)),
                outputRow(UidType.PUID, String.valueOf(PUID_3)),
                outputRow(UidType.PUID, String.valueOf(PUID_4)),
                outputRow(UidType.YUID, YUID_1)
        ));

        Set<Uid> result = execute(action, filterStep).stream()
                .map(row -> Uid.of(row.getIdType(), row.getIdValue()))
                .collect(Collectors.toSet());

        assertEquals(expectedResult, result);
    }

    private List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step);
    }

    private PlainAction prepareAction(ActionStep... steps) {
        return actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
    }
}
