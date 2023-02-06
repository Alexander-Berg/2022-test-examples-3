package ru.yandex.market.crm.campaign.services.segments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.OrderFactsTestHelper;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.util.EmailUtil;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.models.Subscription;
import ru.yandex.market.crm.util.CrmStrings;

import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportUuid;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.not;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.ordersFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.genericSubscription;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.mobileAppInfo;
import static ru.yandex.market.crm.core.test.utils.OrderFactsTestHelper.order;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.WISHLIST;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.plusData;

/**
 * @author apershukov
 */
public class SegmentService2Test extends AbstractServiceLargeTest {

    private static final String EMAIL_1 = "user.1@yandex.ru";
    private static final String EMAIL_2 = "user.2@yandex.ru";
    private static final String EMAIL_3 = "user.3@yandex.ru";

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;

    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";
    private static final String UUID_3 = "uuid-3";

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";
    private static final String DEVICE_ID_3 = "device_id_3";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";
    private static final String DEVICE_ID_HASH_3 = "device_id_hash_3";

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private OrderFactsTestHelper orderFactsTestHelper;

    @Inject
    private MobileTablesHelper mobileTablesHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

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
        ytSchemaTestHelper.prepareUserTables();
    }

    /**
     * Если при вычислении сегмента из EMAIL (без склеек) из условия, работающего по EMAIL одновременно вычитаются
     * условия, работающие по EMAIL и PUID, в результат не попадают как адреса из вычитаемого условия по EMAIL так
     * и адреса вычисленные из PUID
     *
     * см. LILUCRM-4917
     */
    @Test
    void testSimpleAndComplexNegativeParts() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, ADVERTISING),
                subscription(EMAIL_2, ADVERTISING),
                subscription(EMAIL_3, WISHLIST)
        );

        userTestHelper.addPlusData(
                plusData(PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        var segment = segment(
                subscriptionFilter(ADVERTISING),
                not(subscriptionFilter(WISHLIST)),
                not(plusFilter())
        );

        var expected = Set.of(
                pair(Uid.asEmail(EMAIL_1))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, Set.of(UidType.EMAIL), segment);
    }

    /**
     * В случае если один и тот же uuid попадает в сегмент из разных источников, в счетчике он учитывается как один.
     */
    @Test
    void testCountSameUuidAsOne() throws Exception {
        userTestHelper.addPlusData(
                plusData(PUID_1),
                plusData(PUID_2)
        );

        chytDataTablesHelper.preparePassportUuids(
                chytPassportUuid(PUID_1, UUID_1),
                chytPassportUuid(PUID_2, UUID_2)
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_2))
                        .addNode(Uid.asUuid(UUID_2))
                        .addEdge(0, 1)
        );

        var segment = segment(plusFilter());
        segmentatorTestHelper.assertCounts(segment, LinkingMode.ALL, Map.of(UidType.UUID, 2));
    }

    /**
     * Если в вычитаемом условии "Осуществлял заказ" содержится email с доменом yandex.kz,
     * а в условии подписки присутствует тот же email, но со стондартным доменом yandex.ru,
     * тогда в результат подсчёта сегмента не попадёт ни один из них
     */
    @Test
    public void testNormalizeEmailsInSegmentPartsResult() throws Exception {
        String email1 = "qwert@yandex.ru";
        String email2 = "qwert@yandex.kz";

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_3),
                subscription(email1)
        );

        orderFactsTestHelper.prepareOrders(
                order(email2, 1)
        );

        Segment segment = segment(
                subscriptionFilter(ADVERTISING),
                not(ordersFilter())
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
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
     * Если были изменены (кастомизированы) дефолтные выходные идентификаторы условия сегментации,
     * и если результирующий идентификатор сегмента не попадает в них, но попадает в дефолтные идентификаторы условия,
     * то на выходе после этого условия он должен быть разрешен после в рамках склейки
     */
    @Test
    public void testSegmentAlgorithmPartUidTypesCustomization() throws Exception {
        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2),
                mobileAppInfo(UUID_3)
        );

        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2),
                chytUuidWithToken(UUID_3, DEVICE_ID_3, DEVICE_ID_HASH_3)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1),
                genericSubscription(UUID_2),
                genericSubscription(UUID_3)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1),
                chytUuidWithSubscription(UUID_2),
                chytUuidWithSubscription(UUID_3)
        );

        orderFactsTestHelper.prepareOrders(
                order(Uids.create(ru.yandex.market.crm.platform.commons.UidType.PUID, PUID_1))
        );

        prepareUsers(
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_1))
                        .addNode(Uid.asUuid(UUID_1))
                        .addNode(Uid.asUuid(UUID_2))
                        .addEdge(0, 1)
                        .addEdge(0, 2),
                new IdsGraph()
                        .addNode(Uid.asPuid(PUID_2))
                        .addNode(Uid.asUuid(UUID_3))
                        .addEdge(0, 1)
        );

        Segment segment = segment(
                mobilesFilter(),
                ordersFilter().setUidTypes(Set.of(UidType.PUID)).setNot(true)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asUuid(UUID_3))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.DIRECT_ONLY,
                Set.of(UidType.UUID),
                segment
        );
    }

    /**
     * В случае если паспортный адрес пользователя прописан с буквами в верхнем регистре
     * он все равно вычитается из сегмента в которой тот же адрес попал в нижнем регистре
     */
    @Test
    public void testIgnoreRegisterForPassportEmails() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2),
                subscription(CrmStrings.capitalize(EMAIL_2), PUID_2)
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
                not(plusFilter())
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * При подсчёте сегмента в результат попадают email'ы в нормализованном виде
     * (lowerCase и, если почта яндекса, то единый домен yandex.ru)
     */
    @Test
    public void testNormalizeEmailsInSegmentResult() throws Exception {
        List<String> yaDomains = List.copyOf(EmailUtil.YANDEX_DOMAINS);

        List<Pair<Long, String>> puidsWithEmails = IntStream
                .range(0, yaDomains.size())
                .mapToObj(i -> Pair.of(i + 1L, String.format("SOME_EMAIL_%d@%s", i, yaDomains.get(i))))
                .collect(Collectors.toList());

        userTestHelper.addPassportProfiles(
                puidsWithEmails.stream()
                        .map(x -> passportProfile(x.getFirst(), "m"))
                        .toArray(YTreeMapNode[]::new)
        );

        subscriptionsTestHelper.saveSubscriptions(
                puidsWithEmails.stream()
                        .map(x -> subscription(x.getSecond(), x.getFirst()))
                        .toArray(Subscription[]::new)
        );

        chytDataTablesHelper.preparePassportEmails(
                puidsWithEmails.stream()
                        .map(x -> chytPassportEmail(x.getFirst(), x.getSecond()))
                        .toArray(YTreeMapNode[]::new)
        );

        Segment segment = segment(
                passportGender("m"),
                subscriptionFilter(ADVERTISING)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expectedPairs = puidsWithEmails.stream()
                .map(x -> pair(
                        Uid.asEmail(EmailUtil.normalizeEmail(x.getSecond().toLowerCase())),
                        Uid.asPuid(x.getFirst())
                ))
                .collect(Collectors.toSet());

        segmentatorTestHelper.assertSegmentPairs(expectedPairs, LinkingMode.NONE, segment);
    }

    /**
     * Не нормализуем email'ы, если домен похож на домен yandex'a, но таковым не является (напр, email@ya.rutube.ru)
     */
    @Test
    public void testNotNormalizeEmailsWithDomainsLookLikeYandex() throws Exception {
        List<String> yaDomains = new ArrayList<>(EmailUtil.YANDEX_DOMAINS);
        List<Pair<Long, String>> puidsWithEmails = IntStream
                .range(0, yaDomains.size())
                .mapToObj(i -> Pair.of(i + 1L, String.format("email%d@%stube.ru", i, yaDomains.get(i))))
                .collect(Collectors.toList());

        userTestHelper.addPassportProfiles(
                puidsWithEmails.stream()
                        .map(x -> passportProfile(x.getFirst(), "m"))
                        .toArray(YTreeMapNode[]::new)
        );

        subscriptionsTestHelper.saveSubscriptions(
                puidsWithEmails.stream()
                        .map(x -> subscription(x.getSecond(), x.getFirst()))
                        .toArray(Subscription[]::new)
        );

        chytDataTablesHelper.preparePassportEmails(
                puidsWithEmails.stream()
                        .map(x -> chytPassportEmail(x.getFirst(), x.getSecond()))
                        .toArray(YTreeMapNode[]::new)
        );

        Segment segment = segment(
                passportGender("m"),
                subscriptionFilter(ADVERTISING)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expectedPairs = puidsWithEmails.stream()
                .map(x -> pair(
                        Uid.asEmail(x.getSecond().toLowerCase()),
                        Uid.asPuid(x.getFirst())
                ))
                .collect(Collectors.toSet());

        segmentatorTestHelper.assertSegmentPairs(expectedPairs, LinkingMode.NONE, segment);
    }

    private void prepareUsers(IdsGraph... userGraph) {
        var users = Stream.of(userGraph)
                .map(graph -> new User(UUID.randomUUID().toString())
                        .setIdsGraph(graph)
                )
                .toArray(User[]::new);

        userTestHelper.addUsers(users);
        userTestHelper.finishUsersPreparation();
    }
}
