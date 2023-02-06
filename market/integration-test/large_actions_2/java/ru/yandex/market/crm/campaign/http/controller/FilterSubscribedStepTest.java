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
import ru.yandex.market.crm.campaign.domain.actions.status.FilterSubscribedStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.filterSubscribed;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.GRADE_AFTER_CPA;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.EMAIL_MD5;
import static ru.yandex.market.crm.platform.models.Subscription.Status.UNSUBSCRIBED;

/**
 * @author apershukov
 */
public class FilterSubscribedStepTest extends AbstractControllerLargeTest {

    private static final String EMAIL_1 = "email_1@yandex-team.ru";
    private static final String EMAIL_2 = "email_2@yandex-team.ru";
    private static final String EMAIL_3 = "email_3@yandex-team.ru";

    private static final String YUID_1 = "111";
    private static final String YUID_2 = "222";

    private static final long PUID_1 = 1;
    private static final long PUID_2 = 2;
    private static final long PUID_3 = 3;

    private static final String CRYPTA_ID_1 = "crypta-id-1";
    private static final String CRYPTA_ID_2 = "crypta-id-2";
    private static final String CRYPTA_ID_3 = "crypta-id-3";

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private StepsStatusDAO stepsStatusDAO;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareUserTables();
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.prepareChytPassportEmailsTable();
    }

    /**
     * После прохождения шага фильтрации по подписке в акции остаются только
     * идентификаторы, вычисляемые адреса для которых имеют указанную подписку
     */
    @Test
    public void testFilterSubscribed() throws Exception {
        ActionStep step = filterSubscribed();
        PlainAction action = prepareAction(step);

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

        Set<Uid> results = execute(action, step).stream()
                .map(x -> Uid.of(x.getIdType(), x.getIdValue()))
                .collect(Collectors.toSet());

        assertEquals(Set.of(Uid.asPuid(PUID_2), Uid.asYuid(YUID_1)), results);

        FilterSubscribedStepStatus status = (FilterSubscribedStepStatus) stepsStatusDAO
                .get(action.getId(), step.getId());

        Map<UidType, Long> counts = status.getCounts();
        assertNotNull(counts);
        assertEquals(1, (long) counts.get(UidType.YUID));
        assertEquals(1, (long) counts.get(UidType.PUID));
    }

    /**
     * В случае если идентификатор из акции связан с двумя пользователями имеющими
     * подписанные email-адреса он не дублируется после прохождения шага фильтрации
     * по наличию подписки
     * <p>
     * https://st.yandex-team.ru/LILUCRM-2026
     */
    @Test
    public void testNoDoublesAfterEmailSubscriptionFilter() throws Exception {
        ActionStep step = filterSubscribed();
        PlainAction action = prepareAction(step);

        prepareSegmentationResult(action.getId(), Arrays.asList(
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
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asEmail(EMAIL_2))
                                .addEdge(0, 1)
                );

        User user3 = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_2))
                                .addNode(Uid.asEmail("yuid2@yandex.ru"))
                                .addEdge(0, 1)
                );

        // TODO Не трогать! Где-то внутри addUsers() скрывается бага которая не дает добавлять несколько юзеров
        // с общими id
        userTestHelper.addUsers(user1);
        userTestHelper.addUsers(user2);
        userTestHelper.addUsers(user3);
        userTestHelper.finishUsersPreparation();

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, ADVERTISING),
                subscription(EMAIL_2, ADVERTISING)
        );

        List<Uid> results = execute(action, step).stream()
                .map(x -> Uid.of(x.getIdType(), x.getIdValue()))
                .collect(Collectors.toList());

        assertEquals(List.of(Uid.asYuid(YUID_1)), results);
    }

    /**
     * После прохождения шага фильтрации по подписке в котором указана
     * посписка с логикой "Не отписан" в акции остаются только идентфикаторы с
     * адресами которые не были явно отписаны
     */
    @Test
    public void testFilterNotUnsubscribed() throws Exception {
        ActionStep step = filterSubscribed(GRADE_AFTER_CPA);
        PlainAction action = prepareAction(step);

        prepareSegmentationResult(action.getId(), Arrays.asList(
                outputRow(UidType.PUID, String.valueOf(PUID_1)),
                outputRow(UidType.PUID, String.valueOf(PUID_2))
        ));

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, ADVERTISING, PUID_1),
                subscription(EMAIL_2, UNSUBSCRIBED, GRADE_AFTER_CPA).toBuilder()
                        .setLinkedPuid(PUID_2)
                        .build()
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        List<Uid> results = execute(action, step).stream()
                .map(x -> Uid.of(x.getIdType(), x.getIdValue()))
                .collect(Collectors.toList());

        assertEquals(1, results.size());
        assertTrue(results.contains(Uid.asPuid(PUID_1)));
    }

    /**
     * В случае если в акции включено вычитание глобального контроля, шаг
     * при резолвинге email'ов отбрасывает email-адреса из ГК
     */
    @Test
    public void testFilterEmailsFromGlobalControl() throws Exception {
        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(CRYPTA_ID_1, true),
                uniformSplitEntry(CRYPTA_ID_2, true),
                uniformSplitEntry(CRYPTA_ID_3, false)
        );

        globalSplitsTestHelper.prepareCryptaMatchingEntries(
                EMAIL_MD5,
                cryptaMatchingEntry(EMAIL_1, EMAIL_MD5, CRYPTA_ID_1),
                cryptaMatchingEntry(EMAIL_2, EMAIL_MD5, CRYPTA_ID_2),
                cryptaMatchingEntry(EMAIL_3, EMAIL_MD5, CRYPTA_ID_3)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, ADVERTISING),
                subscription(EMAIL_2, ADVERTISING),
                subscription(EMAIL_3, ADVERTISING)
        );

        User user1 = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asEmail(EMAIL_2))
                                .addEdge(0, 1)
                );
        User user2 = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_2))
                                .addNode(Uid.asEmail(EMAIL_3))
                                .addEdge(0, 1)
                );

        userTestHelper.addUsers(user1, user2);
        userTestHelper.finishUsersPreparation();

        ActionStep step = filterSubscribed();
        PlainAction action = prepareAction(step);
        actionTestHelper.enableGlobalControl(action);

        prepareSegmentationResult(action.getId(), Arrays.asList(
                outputRow(UidType.EMAIL, EMAIL_1),
                outputRow(UidType.YUID, YUID_1),
                outputRow(UidType.YUID, YUID_2)
        ));

        Set<String> results = execute(action, step).stream()
                .map(StepOutputRow::getIdValue)
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1, YUID_1), results);
    }

    private PlainAction prepareAction(ActionStep... steps) {
        return actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
    }

    private List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step);
    }

    public void prepareSegmentationResult(String actionId, List<StepOutputRow> rows) {
        actionTestHelper.prepareSegmentationResult(actionId, rows);
    }
}
