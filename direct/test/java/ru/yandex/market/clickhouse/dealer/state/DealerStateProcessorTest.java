package ru.yandex.market.clickhouse.dealer.state;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.clickhouse.dealer.YtAttributes;
import ru.yandex.market.clickhouse.dealer.clickhouse.ToYyyyMmClickHousePartitionExtractor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 01/02/2018
 */
public class DealerStateProcessorTest {
    private String dateString = "2017-12-19T01:37:19.028075Z";
    private Instant date = Instant.ofEpochSecond(1513647439, 28075000);

    @Test
    public void processNewNode() {
        DealerState state = new DealerState(null);
        String ytPartition = "2018-01-30";
        Instant checkDate = Instant.now();

        processNode(state, ytPartition, 42, 100500, checkDate);

        PartitionState actual = state.getPartitionState(ytPartition);
        PartitionState expected = new PartitionState(
            ytPartition, "201801",
            null,
            new PartitionYtState(date, 100500L, checkDate, 42),
            PartitionState.Status.NEW, 0
        );

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processTransferredNeedUpdate() {
        DealerState state = new DealerState(null);
        PartitionYtState oldYtState = new PartitionYtState(date, 100L, null, 21);
        String ytPartition = "2018-01-31";
        PartitionState partitionState = getPartitionState(
            ytPartition, oldYtState, 100, PartitionState.Status.TRANSFERRED, 21
        );
        state.putPartitionState(partitionState);

        processNode(state, ytPartition, 42, 500, null);

        PartitionState actual = state.getPartitionState(ytPartition);
        PartitionState expected = getPartitionState(
            ytPartition, oldYtState, 500, PartitionState.Status.TRANSFERRED_NEED_UPDATE, 42
        );
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processTransferred() {
        DealerState state = new DealerState(null);
        PartitionYtState ytState = new PartitionYtState(date, 100L, null, 21);
        String ytPartition = "2018-01-31";
        PartitionState partitionState = getPartitionState(
            ytPartition, ytState, 100, PartitionState.Status.TRANSFERRED, 21
        );
        state.putPartitionState(partitionState);

        processNode(state, ytPartition, 21, 100, null);

        PartitionState actual = state.getPartitionState(ytPartition);
        PartitionState expected = getPartitionState(
            ytPartition, ytState, 100, PartitionState.Status.TRANSFERRED, 21
        );

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processSkipped() {
        DealerState state = new DealerState(null);
        PartitionYtState ytState = new PartitionYtState(date, 0, null, 21);
        String ytPartition = "2018-08-03";

        PartitionState partitionState = getPartitionState(
            ytPartition, ytState, 0, PartitionState.Status.TRANSFERRED, 21
        );
        state.putPartitionState(partitionState);

        processNode(state, ytPartition, 21, 0, null);

        PartitionState actual = state.getPartitionState(ytPartition);
        PartitionState expected = getPartitionState(
            ytPartition, ytState, 0, PartitionState.Status.SKIPPED, 21
        );

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processYtDeleted() {
        DealerState state = new DealerState(null);
        PartitionYtState ytState = new PartitionYtState(date, 123, null, 21);
        String ytPartition = "2018-08-03";
        PartitionState partitionState = getPartitionState(
            ytPartition, ytState, 123, PartitionState.Status.TRANSFERRED, 21
        );

        String newYtPartition = "2018-09-20";

        /* Put in DealerState one, but process a new one - because the first one was deleted */
        state.putPartitionState(partitionState);
        processNode(state, newYtPartition, 1, 0, null);

        PartitionState actual = state.getPartitionState(ytPartition);
        PartitionState expected = getPartitionState(
            ytPartition, ytState, 123, PartitionState.Status.YT_DELETED, 21
        );

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processSkippedToNew() {
        DealerState state = new DealerState(null);
        String ytPartition = "2019-03-03";

        PartitionState partitionState = getPartitionState(
            ytPartition, null, 10, PartitionState.Status.SKIPPED, 21
        );
        state.putPartitionState(partitionState);

        processNode(state, ytPartition, 21, 10, null);

        PartitionState actual = state.getPartitionState(ytPartition);
        PartitionState expected = getPartitionState(
            ytPartition, null, 10, PartitionState.Status.NEW, 21
        );

        Assert.assertEquals(expected, actual);
    }

    private static YTreeStringNode createYtNode(String dir, long revision, long rowCount, String modificationDate) {
        return new YTreeStringNodeImpl(
            dir,
            Cf.map(
                YtAttributes.MODIFICATION_TIME, new YTreeStringNodeImpl(modificationDate, Cf.map()),
                YtAttributes.REVISION, new YTreeIntegerNodeImpl(false, revision, Cf.map()),
                YtAttributes.ROW_COUNT, new YTreeIntegerNodeImpl(false, rowCount, Cf.map()),
                YtAttributes.TYPE, new YTreeStringNodeImpl("table", Cf.map())
            )
        );
    }

    private PartitionState getPartitionState(String ytPartition, PartitionYtState ytState, long rowCount,
                                             PartitionState.Status status, long revision) {
        return getPartitionState("201801", ytPartition, ytState, rowCount, status, revision);
    }

    private PartitionState getPartitionState(String clickhousePartition, String ytPartition, PartitionYtState ytState,
                                             long rowCount, PartitionState.Status status, long revision) {
        return new PartitionState(
            ytPartition, clickhousePartition,
            ytState == null ? null : new PartitionClickHouseState(ytState, null, null),
            new PartitionYtState(date, rowCount, null, revision),
            status, 1
        );
    }

    private void processNode(DealerState state, String ytPartition, long revision, long rowCount, Instant checkDate) {
        YTreeStringNode newNode = createYtNode(ytPartition, revision, rowCount, dateString);
        DealerStateProcessor.processYtNodes(
            state, ToYyyyMmClickHousePartitionExtractor.INSTANCE, Collections.singletonList(newNode), checkDate
        );
    }

    @Test
    public void checkForRotatedPartitionsRequiredBeTransferred() {
        DealerState state = Mockito.mock(DealerState.class);

        PartitionState shouldntBeChanged = getPartitionState(
            "201904", "2019-04-25", null, 1, PartitionState.Status.TRANSFERRED, 42
        );

        PartitionState shouldntBeChanged2 = getPartitionState(
            "201903", "2019-03-20", null, 1, PartitionState.Status.NEW, 42
        );

        PartitionState shouldBeChanged = getPartitionState(
            "201902", "2019-02-23", null, 1, PartitionState.Status.ROTATED, 42
        );

        PartitionState shouldBeChanged2 = getPartitionState(
            "201902", "2019-02-20", null, 2, PartitionState.Status.ROTATED, 43
        );

        PartitionState shouldntBeChanged3 = getPartitionState(
            "201901", "2019-01-21", null, 2, PartitionState.Status.ROTATED, 43
        );

        List<PartitionState> states = Arrays.asList(
            shouldBeChanged, shouldntBeChanged, shouldBeChanged2, shouldntBeChanged2, shouldntBeChanged3
        );
        Mockito.when(state.getPartitionStates()).thenReturn(states);

        String chPartition = "201902";

        DealerStateProcessor.checkForRotatedPartitionsRequiredBeTransferred(state, chPartition);
        Assert.assertEquals(PartitionState.Status.TRANSFERRED, shouldntBeChanged.getStatus());
        Assert.assertEquals(PartitionState.Status.NEW, shouldntBeChanged2.getStatus());
        Assert.assertEquals(PartitionState.Status.TRANSFERRED_NEED_UPDATE, shouldBeChanged.getStatus());
        Assert.assertEquals(PartitionState.Status.TRANSFERRED_NEED_UPDATE, shouldBeChanged2.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATED, shouldntBeChanged3.getStatus());
    }

    @Test
    public void checkForRotatedPartitionsRequiredBeTransferredForYYYYMMDD() {
        DealerState state = Mockito.mock(DealerState.class);

        PartitionState shouldntBeChanged1 = getPartitionState("2019-04-25", PartitionState.Status.TRANSFERRED);
        PartitionState shouldntBeChanged2 = getPartitionState("'2019-04-25'", PartitionState.Status.TRANSFERRED);
        PartitionState shouldBeChanged1 = getPartitionState("2019-04-24", PartitionState.Status.ROTATED);
        PartitionState shouldBeChanged2 = getPartitionState("'2019-04-24'", PartitionState.Status.ROTATED);
        PartitionState shouldntBeChanged3 = getPartitionState("2019-04-23", PartitionState.Status.ROTATED);
        PartitionState shouldntBeChanged4 = getPartitionState("'2019-04-23'", PartitionState.Status.ROTATED);

        List<PartitionState> states = Arrays.asList(shouldBeChanged1, shouldBeChanged2,
            shouldntBeChanged1, shouldntBeChanged2, shouldntBeChanged3, shouldntBeChanged4);
        Mockito.when(state.getPartitionStates()).thenReturn(states);

        String chPartition = "2019-04-24";

        DealerStateProcessor.checkForRotatedPartitionsRequiredBeTransferred(state, chPartition);
        Assert.assertEquals(PartitionState.Status.TRANSFERRED, shouldntBeChanged1.getStatus());
        Assert.assertEquals(PartitionState.Status.TRANSFERRED, shouldntBeChanged2.getStatus());
        Assert.assertEquals(PartitionState.Status.TRANSFERRED_NEED_UPDATE, shouldBeChanged1.getStatus());
        Assert.assertEquals(PartitionState.Status.TRANSFERRED_NEED_UPDATE, shouldBeChanged2.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATED, shouldntBeChanged3.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATED, shouldntBeChanged4.getStatus());
    }

    @Test
    public void checkForNewPartitionsRequiredRotationYYYYMMDD() {
        DealerState state = Mockito.mock(DealerState.class);

        PartitionState shouldntBeChanged = getPartitionState("2019-04-25", PartitionState.Status.NEW);
        PartitionState shouldntBeChanged2 = getPartitionState("2019-04-24", PartitionState.Status.NEW);
        PartitionState shouldBeChanged = getPartitionState("'2019-04-23'", PartitionState.Status.NEW);
        PartitionState shouldBeChanged2 = getPartitionState("2019-04-22", PartitionState.Status.NEW);

        List<PartitionState> states = Arrays.asList(shouldBeChanged, shouldntBeChanged, shouldntBeChanged2, shouldBeChanged2);
        Mockito.when(state.getPartitionStates()).thenReturn(states);

        String chPartition = "2019-04-24";

        DealerStateProcessor.checkForNewPartitionsRequiredRotation(state, chPartition);
        Assert.assertEquals(PartitionState.Status.NEW, shouldntBeChanged.getStatus());
        Assert.assertEquals(PartitionState.Status.NEW, shouldntBeChanged2.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATED, shouldBeChanged.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATED, shouldBeChanged2.getStatus());
    }

    @Test
    public void checkForPartitionsRequiredBeRotated() {
        DealerState state = Mockito.mock(DealerState.class);

        PartitionState shouldBeChanged1 = getPartitionState("2018-01-02", PartitionState.Status.TRANSFERRED);
        PartitionState shouldBeChanged2 = getPartitionState("2019-01-03", PartitionState.Status.TRANSFERRED_DATA_MISMATCH);
        PartitionState shouldBeChanged3 = getPartitionState("2019-02-04", PartitionState.Status.TRANSFERRED_NEED_UPDATE);

        PartitionState shouldntBeChanged1 = getPartitionState("2019-02-05", PartitionState.Status.YT_DELETED);
        PartitionState shouldntBeChanged2 = getPartitionState("2019-02-06", PartitionState.Status.NEW);
        PartitionState shouldntBeChanged3 = getPartitionState("2019-02-07", PartitionState.Status.ROTATED);

        PartitionState shouldntBeChanged4 = getPartitionState("2019-07-09", PartitionState.Status.TRANSFERRED);
        PartitionState shouldntBeChanged5 = getPartitionState("2019-07-11", PartitionState.Status.NEW);
        PartitionState shouldntBeChanged6 = getPartitionState("'2019-07-11'", PartitionState.Status.TRANSFERRED);


        List<PartitionState> states = Arrays.asList(
            shouldBeChanged1, shouldBeChanged2, shouldBeChanged3,
            shouldntBeChanged1, shouldntBeChanged2, shouldntBeChanged3, shouldntBeChanged4, shouldntBeChanged5, shouldntBeChanged6
        );
        Mockito.when(state.getPartitionStates()).thenReturn(states);

        String chPartition = "2019-04-24";

        DealerStateProcessor.checkForPartitionsRequiredBeRotated(state, chPartition);

        Assert.assertEquals(PartitionState.Status.ROTATING, shouldBeChanged1.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATING, shouldBeChanged2.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATING, shouldBeChanged3.getStatus());

        Assert.assertEquals(PartitionState.Status.YT_DELETED, shouldntBeChanged1.getStatus());
        Assert.assertEquals(PartitionState.Status.NEW, shouldntBeChanged2.getStatus());
        Assert.assertEquals(PartitionState.Status.ROTATED, shouldntBeChanged3.getStatus());

        Assert.assertEquals(PartitionState.Status.TRANSFERRED, shouldntBeChanged4.getStatus());
        Assert.assertEquals(PartitionState.Status.NEW, shouldntBeChanged5.getStatus());
        Assert.assertEquals(PartitionState.Status.TRANSFERRED, shouldntBeChanged6.getStatus());
    }

    @Test
    public void shouldBeRotatedTest() {
        PartitionState state = getPartitionState("2018-01-02", PartitionState.Status.TRANSFERRED);
        Assert.assertFalse(DealerStateProcessor.shouldBeRotated("2018-01-02", state));
        Assert.assertTrue(DealerStateProcessor.shouldBeRotated("2019-05-01", state));
        Assert.assertFalse(DealerStateProcessor.shouldBeRotated("2017-01-02", state));

        PartitionState state2 = getPartitionState("'2018-01-02'", PartitionState.Status.TRANSFERRED);
        Assert.assertFalse(DealerStateProcessor.shouldBeRotated("2018-01-02", state2));
        Assert.assertTrue(DealerStateProcessor.shouldBeRotated("2019-05-01", state2));
        Assert.assertFalse(DealerStateProcessor.shouldBeRotated("2017-01-02", state2));

    }

    private PartitionState getPartitionState(String datePartition, PartitionState.Status status) {
        return getPartitionState(
            datePartition, datePartition, null, 1, status, 42
        );
    }
}