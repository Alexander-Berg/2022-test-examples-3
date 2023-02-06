package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DataFormatTest {

    @Test
    public void fromString() {
        assertThat(DataFormat.fromString("protobuf")).isEqualTo(DataFormat.PROTOBUF);
        assertThat(DataFormat.fromString("json")).isEqualTo(DataFormat.JSON);
    }

    @Test
    public void testToString() {
        // toString is used in configs.
        assertThat(DataFormat.JSON.toString()).isEqualTo("json");
        assertThat(DataFormat.PROTOBUF.toString()).isEqualTo("protobuf");
    }
}
