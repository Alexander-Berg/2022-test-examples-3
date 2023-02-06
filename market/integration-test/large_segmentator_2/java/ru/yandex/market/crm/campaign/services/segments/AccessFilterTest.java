package ru.yandex.market.crm.campaign.services.segments;

import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.accessMarketFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.DeviceType.APP;
import static ru.yandex.market.crm.core.test.utils.DeviceType.DESKTOP;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.accessEntry;

public class AccessFilterTest extends AbstractServiceLargeTest {
    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;

    private static final String YUID_1 = "111";
    private static final String YUID_2 = "222";
    private static final String YUID_3 = "333";

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareAccessDataTables();
    }

    /**
     * В сегмент попадают только те пользователи, которые посещали сервис с указанного типа девайса
     */
    @Test
    public void testSegmentContainsOnlyUsersVisitedServiceWithDevice() throws Exception {
        userTestHelper.addFapiAccessLogEntries(
                accessEntry(null, PUID_1, DESKTOP),
                accessEntry(null, PUID_2, DESKTOP),
                accessEntry(null, PUID_3, APP),
                accessEntry(YUID_1, null, DESKTOP),
                accessEntry(YUID_2, null, DESKTOP),
                accessEntry(YUID_3, null, APP)
        );

        Segment segment = segment(
                accessMarketFilter(DESKTOP)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1)),
                pair(Uid.asPuid(PUID_2)),
                pair(Uid.asYuid(YUID_1)),
                pair(Uid.asYuid(YUID_2))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID),
                segment
        );
    }

    /**
     * Если в сегменте указан тип девайса APP, то пользователи, посетившие сервис, будут определены как из данных,
     * полученных от frontend api, так и от content api
     */
    @Test
    public void testSegmentContainsIdsFromCapiAndFapiForUsersVisitedServiceWithApp() throws Exception {
        userTestHelper.addFapiAccessLogEntries(
                accessEntry(null, PUID_1, APP),
                accessEntry(YUID_1, null, APP)
        );

        userTestHelper.addCapiAccessLogEntries(
                accessEntry(null, PUID_2, APP),
                accessEntry(YUID_2, null, APP)
        );

        Segment segment = segment(
                accessMarketFilter(APP)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1)),
                pair(Uid.asPuid(PUID_2)),
                pair(Uid.asYuid(YUID_1)),
                pair(Uid.asYuid(YUID_2))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID),
                segment
        );
    }

    /**
     * Если в сегменте указано несколько типов девайсов, то пользователи, посетившие сервис, будут определены
     * по каждому из них
     */
    @Test
    public void testSegmentContainsIdsVisitedServiceWithSeveralApps() throws Exception {
        userTestHelper.addFapiAccessLogEntries(
                accessEntry(null, PUID_1, APP),
                accessEntry(YUID_1, null, APP)
        );

        userTestHelper.addCapiAccessLogEntries(
                accessEntry(null, PUID_2, DESKTOP),
                accessEntry(YUID_2, null, DESKTOP)
        );

        Segment segment = segment(
                accessMarketFilter(APP, DESKTOP)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asPuid(PUID_1)),
                pair(Uid.asPuid(PUID_2)),
                pair(Uid.asYuid(YUID_1)),
                pair(Uid.asYuid(YUID_2))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID),
                segment
        );
    }
}
