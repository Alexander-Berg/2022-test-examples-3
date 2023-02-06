package ru.yandex.direct.binlogbroker.logbrokerwriter.models;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.binlogbroker.logbrokerwriter.components.DataFormat;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class WorkModeTest {

    @Test
    public void testToStringFormat() {
        // используется в именах локов в YT
        assertThat(WorkMode.PROTOBUF_0.toString()).isEqualTo("protobuf-0");
        assertThat(WorkMode.PROTOBUF_1.toString()).isEqualTo("protobuf-1");
        assertThat(WorkMode.PROTOBUF_2.toString()).isEqualTo("protobuf-2");
        assertThat(WorkMode.PROTOBUF_3.toString()).isEqualTo("protobuf-3");
        assertThat(WorkMode.JSON_0.toString()).isEqualTo("json-0");
        assertThat(WorkMode.JSON_1.toString()).isEqualTo("json-1");
        assertThat(WorkMode.JSON_2.toString()).isEqualTo("json-2");
        assertThat(WorkMode.JSON_3.toString()).isEqualTo("json-3");
    }

    @Test
    public void checkChunksCount() {
        var jsonChunks = StreamEx.of(WorkMode.values())
                .filter(workMode -> workMode.getDataFormat() == DataFormat.JSON)
                .map(workMode -> workMode.getChunkId())
                .sorted()
                .toList();

        var protobufChunks = StreamEx.of(WorkMode.values())
                .filter(workMode -> workMode.getDataFormat() == DataFormat.PROTOBUF)
                .map(workMode -> workMode.getChunkId())
                .sorted()
                .toList();

        var possibleChunks = IntStreamEx.range(0, WorkMode.CHUNKS_COUNT).boxed().toList();

        assertThat(jsonChunks).isEqualTo(possibleChunks);
        assertThat(protobufChunks).isEqualTo(possibleChunks);
    }

    @Test
    public void checkShardsByChunk() {
        var list = asList(1, 2, 3, 4, 5, 6, 7, 8);

        assertThat(WorkMode.JSON_0.getShards(list)).containsExactly(4, 8);
        assertThat(WorkMode.JSON_1.getShards(list)).containsExactly(1, 5);
        assertThat(WorkMode.JSON_2.getShards(list)).containsExactly(2, 6);
        assertThat(WorkMode.JSON_3.getShards(list)).containsExactly(3, 7);
        assertThat(WorkMode.PROTOBUF_0.getShards(list)).containsExactly(4, 8);
        assertThat(WorkMode.PROTOBUF_1.getShards(list)).containsExactly(1, 5);
        assertThat(WorkMode.PROTOBUF_2.getShards(list)).containsExactly(2, 6);
        assertThat(WorkMode.PROTOBUF_3.getShards(list)).containsExactly(3, 7);
    }
}
