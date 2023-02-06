package ru.yandex.market.crm.campaign.services.segments;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.core.domain.CommonDateRange;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.test.utils.YtTestTables;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.models.ExecutedAction;
import ru.yandex.market.crm.yt.client.YtClient;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.periodicAction;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.platform.commons.UidType.PUID;

/**
 * @author apershukov
 */
public class PeriodicActionFilterTest extends AbstractServiceLargeTest {

    private static YTreeMapNode fact(long puid, String actionKey, ZonedDateTime time) {
        ru.yandex.market.crm.platform.commons.Uid uid = Uids.create(PUID, puid);
        long timestamp = time.toEpochSecond() * 1000;

        ExecutedAction fact = ExecutedAction.newBuilder()
                .setUid(uid)
                .setTimestamp(timestamp)
                .setAction(actionKey)
                .build();

        return YTree.mapBuilder()
                .key("id").value(uid.getStringValue())
                .key("id_type").value(uid.getType().name().toLowerCase())
                .key("timestamp").value(timestamp)
                .key("fact").value(new YTreeStringNodeImpl(fact.toByteArray(), null))
                .buildMap();
    }

    private static final String ACTION_1 = "action-1";
    private static final String ACTION_2 = "action-2";

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private YtTestTables ytTestTables;

    @Inject
    private YtClient ytClient;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareExecutedActionFactsTable();
    }

    @Test
    public void testSimpleCase() throws Exception {
        ytClient.write(ytTestTables.getExecutedActions(), YTableEntryTypes.YSON, List.of(
                fact(111, ACTION_1, ZonedDateTime.now().minusDays(7)), // Не пройдет. Слишком давно
                fact(222, ACTION_2, ZonedDateTime.now().minusDays(2)), // Не пройдет. Не та акция
                fact(333, ACTION_1, ZonedDateTime.now().minusDays(1)), // Пройдет
                fact(444, ACTION_1, ZonedDateTime.now())               // Пройдет
        ));

        Segment segment = segment(
                periodicAction(ACTION_1, new CommonDateRange(3))
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asPuid(333L)),
                pair(Uid.asPuid(444L))
        );

        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, Set.of(UidType.PUID), segment);
    }
}
