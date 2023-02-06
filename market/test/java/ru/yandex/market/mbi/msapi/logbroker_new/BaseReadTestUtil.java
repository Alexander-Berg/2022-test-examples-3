package ru.yandex.market.mbi.msapi.logbroker_new;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.mbi.msapi.handler.fields.TransId;
import ru.yandex.market.mbi.msapi.handler.lines.LineHandler;
import ru.yandex.market.mbi.msapi.handler.lines.LineHandlerFactory;
import ru.yandex.market.mbi.msapi.receiver.JobInfo;

/**
 * @author kateleb
 */
public class BaseReadTestUtil {

    protected static final String TOPIC = "test-ident--test-topic";
    protected static final int PARTITION = 0;
    protected static final String TOPIC_AND_PARTITION = "test-ident--test-topic:0";
    protected static final String TEST_READER = "test-reader";
    protected static final String TEST_LOG_TYPE = "test_log_type";

    protected static final long START_OFFSET = 100;
    protected static final Instant FIXED_TIME = Instant.parse("2020-05-01T10:00:00Z");
    protected static final String INCORRECT_CHUNK_DATA = "{\"rowid\":\"line1\"}\n{\"rowid\":\"line2\"}\n{\"rowid" +
            "\":\"line3\"}\n{\"rowid\":\"line4\"}\n{\"rowid\":\"line5\"}\nincorrect blabla";
    protected static final String OK_CHUNK_6_LINES =
            "{\"rowid\":\"line1\"}\n{\"rowid\":\"line2\"}\n{\"rowid\":\"line3\"}\n{\"rowid\":\"line4\"}\n{\"rowid" +
                    "\":\"line5\"}\n{\"rowid\":\"line6\"}";
    protected static final String OK_CHUNK_1_LINE = "{\"rowid\":\"line7\"}";
    protected static final String OK_CHUNK_2_LINES = "{\"rowid\":\"line8\"}\n{\"rowid\":\"line9\"}";
    protected static final String OK_CHUNK_3_LINES = "{\"rowid\":\"line11\"}\n{\"rowid\":\"line12\"}\n{\"rowid" +
            "\":\"line13\"}";
    protected static final String OK_CHUNK_LINE_NL = "{\"rowid\":\"line1\"}\n{\"rowid\":\"line2\"}\n{\"rowid" +
            "\":\"line3\"}\n{\"rowid\":\"line4\"}\n{\"rowid\":\"line5\"}\n{\"rowid\":\"line6\"}\n";
    protected static final String ANOTHER_OK_CHUNK_1_LINE = "{\"rowid\":\"line100\"}";
    protected static final String OK_CHUNK_7_LINES =
            "{\"rowid\":\"line101\"}\n{\"rowid\":\"line102\"}\n{\"rowid\":\"line103\"}\n{\"rowid\":\"line104\"}\n" +
                    "{\"rowid\":\"line105\"}\n{\"rowid\":\"line106\"}\n{\"rowid\":\"line107\"}";
    protected static final String OK_CHUNK_8_LINES =
            "{\"rowid\":\"line111\"}\n{\"rowid\":\"line112\"}\n{\"rowid\":\"line113\"}\n{\"rowid\":\"line114\"}\n" +
                    "{\"rowid\":\"line115\"}\n{\"rowid\":\"line116\"\n{\"rowid\":\"line117\"\n{\"rowid\":\"line117\"}";
    protected static final String EMPTY_DATA = "";
    protected long offset = START_OFFSET;

    protected ConsumerReadResponse readData(String data) {
        List<MessageData> chunks = new ArrayList<>();
        chunks.add(new MessageData(data.getBytes(), ++offset, meta(offset)));

        List<MessageBatch> batches = new ArrayList<>();
        batches.add(new MessageBatch(TOPIC, PARTITION, chunks));
        return new ConsumerReadResponse(batches, 1);
    }

    protected ConsumerReadResponse readData(List<String> data) {
        List<MessageData> chunks = new ArrayList<>();
        for (String chunk : data) {
            chunks.add(new MessageData(chunk.getBytes(), ++offset, meta(offset)));
        }

        List<MessageBatch> batches = new ArrayList<>();
        batches.add(new MessageBatch(TOPIC, PARTITION, chunks));
        return new ConsumerReadResponse(batches, 1);
    }


    protected MessageMeta meta(long offset) {
        return new MessageMeta("sid".getBytes(), offset, 123L, 124L,
                "127.0.0.1", CompressionCodec.RAW, new HashMap<>());
    }

    protected LineHandler lineHandler(List<String> collector) {
        return new LineHandler() {
            @Override
            public String[] requiredColumns() {
                return new String[]{"rowid"};
            }

            @Override
            public void preHandle(String[] columnNames) {
            }

            @Override
            public void handle(String[] columnNames, String line, TransId transId) {
                collector.add(line);
            }

            @Override
            public void postHandle(JobInfo holder) {
            }
        };
    }

    protected List<LineHandlerFactory> getLineHandlerFactories(List<String> collector) {
        return Collections.singletonList(() -> lineHandler(collector));
    }
}
