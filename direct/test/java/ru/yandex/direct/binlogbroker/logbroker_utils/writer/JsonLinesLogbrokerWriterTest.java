package ru.yandex.direct.binlogbroker.logbroker_utils.writer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlogbroker.logbroker_utils.writer.LogbrokerProducerTestUtils.createSuccessfulProducerSupplier;

public class JsonLinesLogbrokerWriterTest {
    JsonLinesLogbrokerWriter<TestObject> writer;
    List<byte[]> written;

    @Before
    public void before() {
        written = new ArrayList<>();
        writer = new JsonLinesLogbrokerWriter<>(createSuccessfulProducerSupplier(written),
                Duration.ofSeconds(100), 1, 5);
    }

    @Test
    public void testBigBulk() throws ExecutionException, InterruptedException {
        List<TestObject> testObjects = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            testObjects.add(new TestObject(i, "HelloWorld " + i));
        }

        writer.write(testObjects).get();
        assertThat(written).hasSize(20);

        Charset charset = StandardCharsets.UTF_8;
        CharsetDecoder decoder = charset.newDecoder();

        List<TestObject> actual =
                written.stream()
                        .map(e -> {
                            CharBuffer c;
                            try {
                                c = decoder.decode(ByteBuffer.wrap(e));
                            } catch (CharacterCodingException ex) {
                                throw new RuntimeException(ex);
                            }
                            decoder.reset();
                            return c.toString();
                        })
                        .flatMap(s -> Stream.of(s.split("\n")))
                        .map(e -> JsonUtils.fromJson(e, TestObject.class))
                        .collect(Collectors.toList());

        assertThat(actual).containsExactlyInAnyOrder(testObjects.toArray(new TestObject[0]));
    }


    private static class TestObject {
        @JsonProperty("id")
        private final int id;

        @JsonProperty("value")
        private final String value;

        @JsonCreator
        public TestObject(@JsonProperty("id") int id, @JsonProperty("value") String value) {
            this.id = id;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestObject that = (TestObject) o;
            return id == that.id &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }
    }
}
