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
import ru.yandex.market.crm.campaign.domain.actions.status.FilterMobileUsersStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.filterMobileUsers;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;

/**
 * @author apershukov
 */
public class FilterMobileUsersStepTest extends AbstractControllerLargeTest {

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

    private static final String CRYPTA_ID_1 = "crypta-id-1";
    private static final String CRYPTA_ID_2 = "crypta-id-2";
    private static final String CRYPTA_ID_3 = "crypta-id-3";

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private ActionTestHelper actionTestHelper;

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
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareChytPassportUuidsTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    /**
     * Шаг "Фильтрация по наличию мобильного приложения" оставляет в акции только
     * идентификаторы из которых можно зарезолвить uuid приложения указанного цвета и с указанной подпиской
     */
    @Test
    public void testFilterMobileUsers() throws Exception {
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

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3)
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

        ActionStep filterStep = filterMobileUsers(STORE_PUSH_GENERAL_ADVERTISING);
        PlainAction action = prepareAction(filterStep);

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

        FilterMobileUsersStepStatus status = (FilterMobileUsersStepStatus) stepsStatusDAO
                .get(action.getId(), filterStep.getId());

        Map<UidType, Long> counts = status.getCounts();
        assertNotNull(counts);
        assertEquals(1, (long) counts.get(UidType.YUID));
        assertEquals(2, (long) counts.get(UidType.PUID));
    }

    /**
     * В случае если в акции включено вычитание глобального контроля, через
     * шаг не проходят идентификаторы мобильные устройства, попавшие в глобальный
     * контроль.
     */
    @Test
    public void testFilterGlobalControl() throws Exception {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(CRYPTA_ID_1, true),
                uniformSplitEntry(CRYPTA_ID_2, true),
                uniformSplitEntry(CRYPTA_ID_3, false)
        );

        globalSplitsTestHelper.prepareCryptaMatchingEntries(
                UserTestHelper.UUID,
                cryptaMatchingEntry(UUID_1, UserTestHelper.UUID, CRYPTA_ID_1),
                cryptaMatchingEntry(UUID_2, UserTestHelper.UUID, CRYPTA_ID_2),
                cryptaMatchingEntry(UUID_3, UserTestHelper.UUID, CRYPTA_ID_3)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2),
                chytPassportUuid(PUID_3, UUID_3)
        );

        ActionStep filterStep = filterMobileUsers();
        PlainAction action = prepareAction(filterStep);
        actionTestHelper.enableGlobalControl(action);

        prepareSegmentationResult(action.getId(), Arrays.asList(
                outputRow(UidType.PUID, String.valueOf(PUID_1)),
                outputRow(UidType.PUID, String.valueOf(PUID_2)),
                outputRow(UidType.PUID, String.valueOf(PUID_3))
        ));

        Set<Long> result = execute(action, filterStep).stream()
                .map(StepOutputRow::getIdValue)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        assertEquals(Set.of(PUID_1, PUID_2), result);
    }

    private PlainAction prepareAction(ActionStep... steps) {
        return actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
    }

    private void prepareSegmentationResult(String actionId, List<StepOutputRow> rows) {
        actionTestHelper.prepareSegmentationResult(actionId, rows);
    }

    private List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step);
    }
}
