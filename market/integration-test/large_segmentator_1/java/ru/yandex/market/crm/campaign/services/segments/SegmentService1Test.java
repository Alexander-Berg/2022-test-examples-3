package ru.yandex.market.crm.campaign.services.segments;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.CryptaProfilesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.core.domain.segment.Condition;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.OrderFactsTestHelper;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;

import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.CryptaProfilesTestHelper.profile;
import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper.pluggedTableRow;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.accessMarketFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.any;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.crypta;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.cryptaSegment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.emailList;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.emailsFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.not;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.ordersFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.pluggableTableFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.genericSubscription;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.mobileAppInfo;
import static ru.yandex.market.crm.core.test.utils.OrderFactsTestHelper.order;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.WISHLIST;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.accessEntry;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.plusData;

/**
 * @author apershukov
 */
public class SegmentService1Test extends AbstractServiceLargeTest {

    private static final String EMAIL_1 = "user.1@yandex.ru";
    private static final String EMAIL_2 = "user.2@yandex.ru";
    private static final String EMAIL_3 = "user.3@yandex.ru";

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";

    private static final String DEVICE_ID_1 = "device_id_1";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";

    private static final String YUID_1 = "111";
    private static final String YUID_2 = "222";

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private MobileTablesHelper mobileTablesHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private CryptaProfilesTestHelper cryptaProfilesTestHelper;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private OrderFactsTestHelper orderFactsTestHelper;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.preparePlusDataTable();
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareGenericSubscriptionFactsTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareChytPassportUuidsTable();
        ytSchemaTestHelper.prepareChytPassportEmailsTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareUserTables();
    }

    /**
     * Сегмент, составленный из комбинации двух условий одно из которой работает
     * по email а другое по puid, может быть вычислен в режиме "Без связей"
     */
    @Test
    public void testSubscribedFilteredPassportProfiles() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        Segment segment = segment(
                passportGender("m"),
                subscriptionFilter(ADVERTISING)
        );

        Set<UidPair> expectedPairs = Set.of(
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expectedPairs, LinkingMode.NONE, segment);
    }

    /**
     * В случае если puid не попал в сегмент его email так же не попадает туда
     */
    @Test
    public void testEmailIsNotIncludedIfItsResolvedFromUnfitPuids() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "f"),
                passportProfile(PUID_3, "m")
        );

        userTestHelper.addPlusData(
                plusData(PUID_2),
                plusData(PUID_3)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_3, EMAIL_2)
        );

        Segment segment = segment(
                passportGender("m"),
                plusFilter()
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_3)),
                pair(Uid.asEmail(EMAIL_2), Uid.asPuid(PUID_3))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, segment);
    }

    /**
     * Проверка пересечения результатов трех разных условий два из которых работают по puid
     * и одно по email
     */
    @Test
    public void testThreeWayIntersection() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m")
        );

        userTestHelper.addPlusData(
                plusData(PUID_1)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        Segment segment = segment(
                passportGender("m"),
                plusFilter(),
                subscriptionFilter(ADVERTISING)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, segment);
    }

    /**
     * В случае если puid вычитается из сегмента, его email-адреса так же вычитаются
     */
    @Test
    public void testExcludeAttachedIds() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m")
        );

        userTestHelper.addPlusData(
                plusData(PUID_2),
                plusData(PUID_3)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2),
                subscription(EMAIL_3, PUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2),
                chytPassportEmail(PUID_3, EMAIL_3)
        );

        Segment segment = segment(
                passportGender("m"),
                not(plusFilter())
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1)),
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, segment);
    }

    /**
     * В случае если email учетной записи вычитается условием, работающим по email,
     * он не попадает в сегмент. При этом адреса который не было в вычитаемом условии
     * остаются в сегменте
     */
    @Test
    public void testExcludeIds() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, WISHLIST, PUID_1),
                subscription(EMAIL_2, PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        Segment segment = segment(
                passportGender("m"),
                not(subscriptionFilter(ADVERTISING))
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1)),
                pair(Uid.asPuid(PUID_2)),
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, segment);
    }

    /**
     * При объединении условий работающих по puid и email в сегмент попадают как
     * puid'ы так и email'ы. При этом в тот же сегмент добавляются паспортные адреса puid'ов
     */
    @Test
    public void testUnionIds() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1).toBuilder()
                        .setType(SubscriptionTypes.WISHLIST.getId())
                        .build(),
                subscription(EMAIL_2, PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        Segment segment = segment(any(
                passportGender("m"),
                subscriptionFilter(ADVERTISING)
        ));

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1)),
                pair(Uid.asPuid(PUID_2)),
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1)),
                pair(Uid.asEmail(EMAIL_2), Uid.asPuid(PUID_2))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, segment);
    }

    /**
     * Email пользователя не резолвится если при запуске вычисления сегмента
     * было указано что он не нужен
     */
    @Test
    public void testDoNotResolveEmailIfItsNotNeeded() throws Exception {
        userTestHelper.addPlusData(
                plusData(PUID_1)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1)
        );

        Segment segment = segment(
                plusFilter()
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, Set.of(UidType.PUID), segment);
    }

    /**
     * При вычислении сегмента из результата выфильтровываются идентификаторы типы которых
     * при запуске вычисления были указаны как ненужные
     */
    @Test
    public void testFilterOutAllUnnecessaryIds() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1)
        );

        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_1))
                        .addNode(Uid.asUuid(UUID_1))
                        .addNode(Uid.asYuid("12345678"))
                        .addEdge(0, 1)
                        .addEdge(0, 2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1)
        );

        Segment segment = segment(
                mobilesFilter()
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.ALL, Set.of(UidType.EMAIL), segment);
    }

    /**
     * Для результатов фильтра, работающего по email'ам, адреса не резолвятся.
     * Если адрес способен пройти такой фильтр, он уже должен быть в результатах. Если
     * его там нет значит его туда добавлять нельзя т. к. он не удовлетворяет фильтрации.
     */
    @Test
    public void testDoNotResolveEmailsInResultsOfEmailFilter() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1).toBuilder()
                        .setType(SubscriptionTypes.WISHLIST.getId())
                        .build(),
                subscription(EMAIL_2)
        );

        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_1))
                        .addNode(Uid.asEmail(EMAIL_2))
                        .addEdge(0, 1)
        );

        Segment segment = segment(
                subscriptionFilter(ADVERTISING)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_2)),
                pair(Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.ALL, segment);
    }

    /**
     * Для паспортных идентификаторов пользователей резолвятся
     * актуальные uuid'ы
     */
    @Test
    public void testResolveUuidsByPuids() throws Exception {
        userTestHelper.addPlusData(
                plusData(PUID_1),
                plusData(PUID_2)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1)
        );

        Segment segment = segment(
                plusFilter()
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1)),
                pair(Uid.asPuid(PUID_2)),
                pair(Uid.asUuid(UUID_1), Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, segment);
    }

    /**
     * Smoke-тест на подсчет сегмента в режиме склейки "Все связи"
     */
    @Test
    public void testCountIdsInSegment() throws InterruptedException {
        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_1))
                        .addNode(Uid.asEmail(EMAIL_1))
                        .addNode(Uid.asUuid(UUID_1))
                        .addEdge(0, 1)
                        .addEdge(0, 2)
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1)
        );

        Segment segment = segment(
                emailsFilter(EMAIL_1)
        );

        segmentatorTestHelper.assertCounts(segment, LinkingMode.ALL, Map.of(
                UidType.EMAIL, 1,
                UidType.UUID, 1,
                UidType.PUID, 1
        ));
    }

    /**
     * При вычислении сегмента из email'ов в режиме "Прямые связи" с условием работающим по yuid
     * в результат попадают только те email-адреса которые напрямую связаны с yuid'ами,
     * удовлетворяющими условию
     */
    @Test
    public void testResolveEmailsByDirectLinks() throws Exception {
        cryptaProfilesTestHelper.prepareProfiles(
                profile(YUID_1).heuristicCommon(List.of(1058)),
                profile(YUID_2).heuristicCommon(List.of(8501))
        );

        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asYuid(YUID_1))
                        .addNode(Uid.asEmail(EMAIL_1))
                        .addNode(Uid.asEmail(EMAIL_2))
                        .addEdge(0, 1)
                        .addEdge(1, 2),
                new IdsGraph()
                        .addNode(Uid.asYuid(YUID_2))
                        .addNode(Uid.asEmail(EMAIL_3))
                        .addEdge(0, 1)
        );

        Segment segment = segment(
                crypta(cryptaSegment(547, 1058))
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.DIRECT_ONLY,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * При пересчете сегмента email'ов с группой условий "Любому" в режиме "Прямые связи"
     * к результатам группы доклеиваются адреса, связанные с ними по прямым связям.
     * <p>
     * В сегмент попадают только email'ы
     */
    @Test
    public void testLinkEmailsWithOrGroup() throws Exception {
        cryptaProfilesTestHelper.prepareProfiles(
                profile(YUID_1).heuristicCommon(List.of(1058)),
                profile(YUID_2).heuristicCommon(List.of(1058))
        );

        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asYuid(YUID_1))
                        .addNode(Uid.asEmail(EMAIL_1))
                        .addEdge(0, 1)
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_2)
        );

        Segment segment = segment(
                "Test segment",
                Condition.ANY,
                crypta(cryptaSegment(547, 1058)),
                subscriptionFilter(ADVERTISING)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1)),
                pair(Uid.asEmail(EMAIL_2))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.DIRECT_ONLY,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * В случае если из результатов условия, работающего по EMAIL,
     * вычитаются два условия, работающих по PUID'ам в результат не
     * попадают адреса учеток, удовлетворяющих вычитаемым условиям
     */
    @Test
    public void testSegmentWithTwoNegativePuidCriterions() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2),
                subscription(EMAIL_3)
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m")
        );

        userTestHelper.addPlusData(
                plusData(PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        Segment segment = segment(
                subscriptionFilter(ADVERTISING),
                not(passportGender("m")),
                not(plusFilter())
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_3))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * Проверка вычисления сегмента у которого из трех условий есть два работающие по одному типу идентификатора
     * одно из которых настроено на исключение
     */
    @Test
    public void testSingleCriterionGroup() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2)
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m")
        );

        userTestHelper.addPlusData(
                plusData(PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        Segment segment = segment(
                subscriptionFilter(ADVERTISING),
                passportGender("m"),
                not(plusFilter())
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * Из подключенной пользовательской таблицы, содержащей puid'ы, можно получить
     * сегмент, состоящий из email'ов
     */
    @Test
    public void testResolveEmailsFromPluggableTable() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID,
                pluggedTableRow(String.valueOf(PUID_1)),
                pluggedTableRow(String.valueOf(PUID_2))
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        Segment segment = segment(
                pluggableTableFilter(
                        pluggableTable.getId(),
                        pluggableTable.getPath(),
                        pluggableTable.getUidColumn(),
                        pluggableTable.getUidType()
                )
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1)),
                pair(Uid.asEmail(EMAIL_2), Uid.asPuid(PUID_2))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * В случае если вычитается группа условий с режимом "Любому" в результате
     * отсутствуют идентификаторы удовлетворяющием любому условию из группы
     */
    @Test
    public void testExcludeGroup() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m"),
                passportProfile(PUID_3, "m")
        );

        userTestHelper.addFapiAccessLogEntries(
                accessEntry(YUID_1, PUID_2)
        );

        userTestHelper.addPlusData(
                plusData(PUID_3)
        );

        Segment segment = segment(
                passportGender("m"),
                not(any(
                        accessMarketFilter(),
                        plusFilter()
                ))
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID),
                segment
        );
    }

    /**
     * Проверка вычисления puid из email по прямым связям.
     * Тест написан после инцидента с похожим сегментом в проде
     */
    @Test
    public void testResolvePuidsFromEmailsByDirectLinks() throws Exception {
        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_1))
                        .addNode(Uid.asEmail(EMAIL_1))
                        .addNode(Uid.asPuid(PUID_2))
                        .addEdge(0, 1)
                        .addEdge(0, 2)
        );

        Segment segment = segment(
                emailList(EMAIL_1)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.DIRECT_ONLY,
                Set.of(UidType.PUID),
                segment
        );
    }

    @Test
    public void testAnyGroupPuidsResolving() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1),
                subscription(EMAIL_2, PUID_2),
                subscription(EMAIL_3, PUID_1)
        );

        userTestHelper.addPlusData(
                plusData(PUID_2)
        );

        orderFactsTestHelper.prepareOrders(
                order(EMAIL_1, PUID_1)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_3),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        Segment segment = segment(
                subscriptionFilter(ADVERTISING),
                any(
                        ordersFilter(),
                        plusFilter()
                )
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1)),
                pair(Uid.asEmail(EMAIL_2), Uid.asPuid(PUID_2))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * Активные девайсы попадают в сегмент при его расчете, а неактивные - нет
     */
    @Test
    public void testHitActiveMetricaDevicesIntoSegment() throws InterruptedException {
        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_1))
                        .addNode(Uid.asEmail(EMAIL_1))
                        .addNode(Uid.asUuid(UUID_1))
                        .addEdge(0, 1)
                        .addEdge(0, 2),
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_2))
                        .addNode(Uid.asEmail(EMAIL_2))
                        .addNode(Uid.asUuid(UUID_2))
                        .addEdge(0, 1)
                        .addEdge(0, 2)
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1)
        );

        Segment segment = segment(
                emailsFilter(EMAIL_1, EMAIL_2)
        );

        segmentatorTestHelper.assertCounts(
                segment,
                LinkingMode.ALL,
                Map.of(UidType.UUID, 1)
        );
    }

    private static User user(IdsGraph graph) {
        return new User(UUID.randomUUID().toString())
                .setIdsGraph(graph);
    }

    private void prepareUsers(IdsGraph... userGraph) {
        var users = Stream.of(userGraph)
                .map(SegmentService1Test::user)
                .toArray(User[]::new);

        userTestHelper.addUsers(users);
        userTestHelper.finishUsersPreparation();
    }
}
