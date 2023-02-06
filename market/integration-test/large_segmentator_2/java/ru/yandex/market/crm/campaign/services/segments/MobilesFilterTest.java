package ru.yandex.market.crm.campaign.services.segments;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithSubscription;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytUuidWithToken;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.mobilesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.genericSubscription;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.mobileAppInfo;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_PERSONAL_ADVERTISING;

public class MobilesFilterTest extends AbstractServiceLargeTest {
    private static final String UUID_1 = "uuid-1";
    private static final String UUID_2 = "uuid-2";

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";

    @Inject
    private MobileTablesHelper mobileTablesHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareGenericSubscriptionFactsTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();
        ytSchemaTestHelper.prepareChytUuidsWithTokensTable();
        ytSchemaTestHelper.prepareChytUuidsWithSubscriptionsTable();
    }

    /**
     * При построении сегмента с использованием алгоритма mobiles (сегмент пользователей, имеющих мобильные
     * приложения с соответствующими свойствами) должны исключиться идентификаторы девайсов, которые в АппМетрике
     * считаются неактивными
     */
    @Test
    public void testCorrectWorkMobilesAlgorithmWithActiveMetricaDevices() throws InterruptedException {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1),
                chytUuidWithSubscription(UUID_2)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1),
                genericSubscription(UUID_2)
        );

        Segment segment = segment(
                mobilesFilter()
        );

        segmentatorTestHelper.assertCounts(
                segment,
                LinkingMode.NONE,
                Map.of(UidType.UUID, 1)
        );
    }

    /**
     * Должны исключиться идентификаторы девайсов, у которых нет подписок, указанных в условии сегмента,
     * если параметр "Зарегистрированы на получение пуш уведомлений" = Да
     */
    @Test
    public void testCorrectWorkMobilesAlgorithmWithGenericSubscriptionIfSubscribed() throws InterruptedException {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_PERSONAL_ADVERTISING, true)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, true),
                genericSubscription(UUID_2, STORE_PUSH_PERSONAL_ADVERTISING, true)
        );

        Segment segment = segment(
                mobilesFilter(1, STORE_PUSH_GENERAL_ADVERTISING)
        );

        segmentatorTestHelper.assertCounts(
                segment,
                LinkingMode.NONE,
                Map.of(UidType.UUID, 1)
        );
    }

    /**
     * Должны исключиться идентификаторы девайсов, у которых есть подписки, указанные в условии сегмента,
     * если параметр "Зарегистрированы на получение пуш уведомлений" = Нет
     */
    @Test
    public void testCorrectWorkMobilesAlgorithmWithoutGenericSubscriptionIfUnsubscribed() throws InterruptedException {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, false),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_PERSONAL_ADVERTISING, false)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, false),
                genericSubscription(UUID_2, STORE_PUSH_PERSONAL_ADVERTISING, false)
        );

        Segment segment = segment(
                mobilesFilter(-1, STORE_PUSH_GENERAL_ADVERTISING)
        );

        segmentatorTestHelper.assertCounts(
                segment,
                LinkingMode.NONE,
                Map.of(UidType.UUID, 1)
        );
    }

    /**
     * Должны остаться все идентификаторы девайсов независимо от подписок, указанных в условии сегмента,
     * если параметр "Зарегистрированы на получение пуш уведомлений" = Не важно
     */
    @Test
    public void testCorrectWorkMobilesAlgorithmWithGenericSubscriptionIgnoring() throws InterruptedException {
        chytDataTablesHelper.prepareUuidsWithTokens(
                chytUuidWithToken(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1),
                chytUuidWithToken(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
        );

        mobileTablesHelper.prepareMobileAppInfos(
                mobileAppInfo(UUID_1),
                mobileAppInfo(UUID_2)
        );

        chytDataTablesHelper.prepareUuidsWithSubscriptions(
                chytUuidWithSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, false),
                chytUuidWithSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        mobileTablesHelper.prepareGenericSubscriptions(
                genericSubscription(UUID_1, STORE_PUSH_GENERAL_ADVERTISING, false),
                genericSubscription(UUID_2, STORE_PUSH_GENERAL_ADVERTISING, true)
        );

        Segment segment = segment(
                mobilesFilter(0, STORE_PUSH_GENERAL_ADVERTISING)
        );

        segmentatorTestHelper.assertCounts(
                segment,
                LinkingMode.NONE,
                Map.of(UidType.UUID, 2)
        );
    }
}
