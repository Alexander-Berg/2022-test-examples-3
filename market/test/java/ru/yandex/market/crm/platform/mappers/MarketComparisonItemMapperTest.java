package ru.yandex.market.crm.platform.mappers;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.MarketComparisonItem;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MarketComparisonItemMapperTest {
    private final MarketComparisonItemMapper mapper = new MarketComparisonItemMapper();

    @Test
    public void testMapForAddedByPuid() {
        String line = "tskv\tid=1\tuid_type=UID\tuid_value=12345\t" +
            "action_time=1566998926004\taction=ADD\tcategory_id=1\tproduct_id=2\tregion_id=213";

        List<MarketComparisonItem> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        MarketComparisonItem actual = parsed.get(0);
        MarketComparisonItem expected = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926004L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .setRegionId(213)
            .build();

        assertEquals(expected, actual);
    }

    @Test
    public void testMapForAddedByYuid() {
        String line = "tskv\tid=1\tuid_type=YANDEX_UID\tuid_value=12345\t" +
            "action_time=1566998926004\taction=UPDATE\tcategory_id=1\tproduct_id=2\tregion_id=213";

        List<MarketComparisonItem> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        MarketComparisonItem actual = parsed.get(0);
        MarketComparisonItem expected = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926004L)
            .setLastAction(MarketComparisonItem.ActionType.UPDATE)
            .setUid(Uids.create(UidType.YANDEXUID, "12345"))
            .setCategoryId("1")
            .setProductId(2)
            .setRegionId(213)
            .build();

        assertEquals(expected, actual);
    }

    @Test
    public void testMapForAddedByUuid() {
        String line = "tskv\tid=1\tuid_type=UUID\tuid_value=12345\t" +
            "action_time=1566998926004\taction=REMOVE\tcategory_id=1\tproduct_id=2\tregion_id=54";

        List<MarketComparisonItem> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        MarketComparisonItem actual = parsed.get(0);
        MarketComparisonItem expected = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926004L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.UUID, "12345"))
            .setCategoryId("1")
            .setProductId(2)
            .setRegionId(54)
            .build();

        assertEquals(expected, actual);
    }

    @Test
    public void testMapForRemoved() {
        String line = "tskv\tid=1\tuid_type=UID\tuid_value=12345\t" +
            "action_time=1566998926004\taction=UPDATE\tcategory_id=1\tproduct_id=2\tregion_id=4534";

        List<MarketComparisonItem> parsed = mapper.apply(line.getBytes());

        assertEquals(1, parsed.size());

        MarketComparisonItem actual = parsed.get(0);
        MarketComparisonItem expected = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926004L)
            .setLastAction(MarketComparisonItem.ActionType.UPDATE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .setRegionId(4534)
            .build();

        assertEquals(expected, actual);
    }

    @Test
    public void testMapEmptyOrNullRegionId() {
        String line = "tskv\tid=1\tuid_type=UID\tuid_value=12345\t" +
            "action_time=1566998926004\taction=UPDATE\tcategory_id=1\tproduct_id=2";

        MarketComparisonItem expected = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926004L)
            .setLastAction(MarketComparisonItem.ActionType.UPDATE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();

        List<MarketComparisonItem> parsed = mapper.apply(line.getBytes());
        assertEquals(1, parsed.size());
        assertEquals(expected, parsed.get(0));

        parsed = mapper.apply((line + "\tregion_id=").getBytes());
        assertEquals(1, parsed.size());
        assertEquals(expected, parsed.get(0));
    }

    @Test
    public void testMapForUnknownUidType() {
        String line = "tskv\tid=1\tuid_type=EMAIL\tuid_value=12345\t" +
            "action_time=1566998926004\taction=ADD\tcategory_id=1\tproduct_id=2";

        List<MarketComparisonItem> parsed = mapper.apply(line.getBytes());

        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    public void testParseEventWithSkuId() {
        String line = "tskv\tuid_type=UUID\tuid_value=771255d924ee404c83bff78becda8503\tid=322068538\tcategory_id" +
                "=91491\tsku=100911097730\taction_time=1598091801000\taction=REMOVE";

        List<MarketComparisonItem> parsed = mapper.apply(line.getBytes());
        assertThat(parsed, hasSize(1));

        MarketComparisonItem item = parsed.get(0);
        assertEquals(100911097730L, item.getProductId());
        assertEquals(100911097730L, item.getSku());
    }
}
