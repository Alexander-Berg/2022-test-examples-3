package ru.yandex.direct.oneshot.oneshots.invalidpermalinks;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.oneshot.base.YtInputData;
import ru.yandex.direct.oneshot.base.YtState;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@OneshotTest
@RunWith(SpringRunner.class)
public class InvalidPermalinksOneshotTest {

    private static final int CHUNK_SIZE = 5;

    private YtProvider ytProvider;
    private YtOperator ytOperator;
    private InvalidPermalinksOneshotStub oneshot;

    @Before
    public void before() throws Exception {
        ytOperator = mock(YtOperator.class);

        ytProvider = mock(YtProvider.class);
        when(ytProvider.getOperator(any(YtCluster.class))).thenReturn(ytOperator);

        oneshot = new InvalidPermalinksOneshotStub(ytProvider, CHUNK_SIZE);
    }

    @Test
    public void firstIteration_emptyTable() {
        when(ytOperator.readTableRowCount(any(YtTable.class))).thenReturn(0L);
        YtState state = oneshot.execute(createInputData(), null, 1);
        Assert.assertNull(state);
    }

    @Test
    public void firstIteration_rowCountLessThenChunk() {
        Long rowCount = CHUNK_SIZE - 2L;
        when(ytOperator.readTableRowCount(any(YtTable.class))).thenReturn(rowCount);
        YtState state = oneshot.execute(createInputData(), null, 1);
        Assert.assertNotNull(state);
        Assert.assertEquals(rowCount, state.getNextRow());
        Assert.assertEquals(rowCount, state.getTotalRowCount());
    }

    @Test
    public void firstIteration_rowCountMoreThenChunk() {
        Long rowCount = CHUNK_SIZE + 2L;
        when(ytOperator.readTableRowCount(any(YtTable.class))).thenReturn(rowCount);
        YtState state = oneshot.execute(createInputData(), null, 1);
        Assert.assertNotNull(state);
        Assert.assertEquals(Long.valueOf(CHUNK_SIZE), state.getNextRow());
        Assert.assertEquals(rowCount, state.getTotalRowCount());
    }

    @Test
    public void lastIteration() {
        Long rowCount = 7L;
        YtState prevState = new YtState()
                .withTotalRowCount(rowCount)
                .withNextRowFromYtTable(rowCount);
        YtState state = oneshot.execute(createInputData(), prevState, 1);
        Assert.assertNull(state);
    }

    @Test
    public void secondIteration_rowCountLessThenChunk() {
        Long nextRow = Long.valueOf(CHUNK_SIZE);
        Long rowCount = nextRow + CHUNK_SIZE - 2L;
        YtState prevState = new YtState()
                .withTotalRowCount(rowCount)
                .withNextRowFromYtTable(nextRow);
        when(ytOperator.readTableRowCount(any(YtTable.class))).thenReturn(rowCount);
        YtState state = oneshot.execute(createInputData(), prevState, 1);
        Assert.assertNotNull(state);
        Assert.assertEquals(rowCount, state.getNextRow());
        Assert.assertEquals(rowCount, state.getTotalRowCount());
    }

    @Test
    public void secondIteration_rowCountMoreThenChunk() {
        Long nextRow = Long.valueOf(CHUNK_SIZE);
        Long rowCount = nextRow + CHUNK_SIZE + 2L;
        YtState prevState = new YtState()
                .withTotalRowCount(rowCount)
                .withNextRowFromYtTable(nextRow);
        when(ytOperator.readTableRowCount(any(YtTable.class))).thenReturn(rowCount);
        YtState state = oneshot.execute(createInputData(), prevState, 1);
        Assert.assertNotNull(state);
        nextRow += CHUNK_SIZE;
        Assert.assertEquals(nextRow, state.getNextRow());
        Assert.assertEquals(rowCount, state.getTotalRowCount());
    }

    @Test
    public void missShards() {
        Long rowCount = CHUNK_SIZE + 2L;
        when(ytOperator.readTableRowCount(any(YtTable.class))).thenReturn(rowCount);
        Integer shard = 3;
        List<InvalidPermalinkTableRow> rows = IntStream
                .range(shard + 1, shard + CHUNK_SIZE)
                .mapToObj(i -> {
                    InvalidPermalinkTableRow row = new InvalidPermalinkTableRow();
                    row.setValue(InvalidPermalinkTableRow.SHARD, i);
                    row.setValue(InvalidPermalinkTableRow.CLIENT_ID, 123L);
                    row.setValue(InvalidPermalinkTableRow.PERMALINK_ID, 321L);
                    return row;
                })
                .collect(Collectors.toList());
        mockReadTableByRowRange(rows);
        YtState state = oneshot.execute(createInputData(), null, shard);
        Assert.assertEquals(1, oneshot.invokes.size());
        Assert.assertEquals(shard, oneshot.invokes.get(0).getLeft());
        Assert.assertEquals(0, oneshot.invokes.get(0).getRight().size());
    }

    @Test
    public void processItems() {
        Long rowCount = CHUNK_SIZE + 2L;
        when(ytOperator.readTableRowCount(any(YtTable.class))).thenReturn(rowCount);
        Integer shard = 3;
        List<Pair<ClientId, Long>> expectedItems = List.of(
                Pair.of(ClientId.fromLong(10L), 100L),
                Pair.of(ClientId.fromLong(10L), 200L),
                Pair.of(ClientId.fromLong(20L), 100L)
        );
        List<InvalidPermalinkTableRow> missedRows = IntStream
                .range(shard + 1, shard + CHUNK_SIZE - 2)
                .mapToObj(i -> {
                    InvalidPermalinkTableRow row = new InvalidPermalinkTableRow();
                    row.setValue(InvalidPermalinkTableRow.SHARD, i);
                    row.setValue(InvalidPermalinkTableRow.CLIENT_ID, 123L);
                    row.setValue(InvalidPermalinkTableRow.PERMALINK_ID, 321L);
                    return row;
                })
                .collect(Collectors.toList());
        List<InvalidPermalinkTableRow> allRows = StreamEx.of(expectedItems)
                .map(pair -> {
                    InvalidPermalinkTableRow row = new InvalidPermalinkTableRow();
                    row.setValue(InvalidPermalinkTableRow.SHARD, shard);
                    row.setValue(InvalidPermalinkTableRow.CLIENT_ID, pair.getLeft().asLong());
                    row.setValue(InvalidPermalinkTableRow.PERMALINK_ID, pair.getRight());
                    return row;
                })
                .append(missedRows)
                .toList();
        mockReadTableByRowRange(allRows);
        oneshot.execute(createInputData(), null, shard);
        Assert.assertEquals(1, oneshot.invokes.size());
        Assert.assertEquals(shard, oneshot.invokes.get(0).getLeft());
        Assert.assertEquals(expectedItems, oneshot.invokes.get(0).getRight());
    }

    private YtInputData createInputData() {
        YtInputData inputData = new YtInputData();
        inputData.setYtCluster(YtCluster.HAHN.getName());
        inputData.setTablePath("");
        return inputData;
    }

    private void mockReadTableByRowRange(List<InvalidPermalinkTableRow> rows) {
        doAnswer(invocation -> {
            Consumer consumer = invocation.getArgument(1);
            rows.forEach(consumer::accept);
            return null;
        }
        ).when(ytOperator).readTableByRowRange(
                any(YtTable.class),
                any(Consumer.class),
                any(InvalidPermalinkTableRow.class),
                anyLong(),
                anyLong());
    }
}
