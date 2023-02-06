package ru.yandex.market.mboc.common.masterdata.model;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mdm.http.MdmCommon.RemainingShelfLife;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
public class RslCollectionTest {

    @Test
    public void testEmptyRslCollectionActualizesToItself() {
        var collection = new RslCollection();
        var actual = collection.actualize();
        assertThat(actual).isEqualTo(collection);
    }

    @Test
    public void testFutureRslCollectionActualizesToEmpty() {
        long todayTs = today();
        var inDays = List.of(rsl(10, todayTs + 100000));
        var inPercents = List.of(rsl(20, todayTs + 200000));
        var outDays = List.of(rsl(30, todayTs + 300000));
        var outPercents = List.of(rsl(40, todayTs + 400000));
        var collection = new RslCollection(inDays, inPercents, outDays, outPercents);
        var actual = collection.actualize();
        assertTrue(actual.isEmpty());
    }

    @Test
    public void testLatestRslTakenFromOlderRecords() {
        long todayTs = today();
        var inDays = List.of(
            rsl(10, todayTs - 100000),
            rsl(20, todayTs - 10000),
            rsl(30, todayTs),
            rsl(40, todayTs + 1)
        );
        var inPercents = List.of(
            rsl(35, todayTs - 10)
        );
        var outDays = List.of(
            rsl(50, todayTs - 100000),
            rsl(60, todayTs - 10000),
            rsl(70, todayTs),
            rsl(80, todayTs + 1)
        );
        var outPercents = List.of(
            rsl(35, todayTs - 10)
        );
        var collection = new RslCollection(inDays, inPercents, outDays, outPercents);
        var actual = collection.actualize();
        assertThat(actual.getInDays()).containsExactly(rsl(30, todayTs));
        assertThat(actual.getOutDays()).containsExactly(rsl(70, todayTs));
        assertThat(actual.getInPercents()).isEmpty();
        assertThat(actual.getOutPercents()).isEmpty();
    }

    private RemainingShelfLife rsl(int value, long time) {
        return RemainingShelfLife.newBuilder()
            .setValue(value)
            .setActivatedAt(time)
            .build();
    }

    private long today() {
        return LocalDate.now().atStartOfDay(TimestampUtil.ZONE_ID).toInstant().toEpochMilli();
    }
}
