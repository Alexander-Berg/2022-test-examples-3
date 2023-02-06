package ru.yandex.market.pers.tms.logbroker.executor.antifraud;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AntifraudBanGradeLbImportExecutorTest extends MockedPersTmsTest {
    private static final List<String> fraudInfoJsons = Arrays.asList(
            "{ \"id\" : 1, \"level\" : 1 }",
            "{ \"id\" : 2, \"level\" : 1 }"
    );

    private static final String MULTILINE_MESSAGE = "{\"id\":104300507,\"level\":1}\n" +
        "{\"id\":104300537,\"level\":2}\n";
    @Autowired
    AntifraudBanGradeLbImportExecutor antifraudBanGradeLbImportExecutor;

    @Test
    public void testCheckSaveAfterConsume() throws TimeoutException, InterruptedException {
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
                Collections.singletonList(generateMessageBatch(fraudInfoJsons)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        antifraudBanGradeLbImportExecutor.consume(syncConsumer);
        assertEquals(2, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
    }

    private void checkFraudLevel(long gradeId, int expectedLevel) {
        assertEquals(expectedLevel, (int) pgJdbcTemplate.queryForObject("select ban_level from antifraud_ban_grade where grade_id = ?", Integer.class, gradeId));
    }

    @Test
    public void testCheckSaveSeveralJsonInOneObject() throws TimeoutException, InterruptedException {
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
            Collections.singletonList(generateMessageBatch(MULTILINE_MESSAGE)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        antifraudBanGradeLbImportExecutor.consume(syncConsumer);
        assertEquals(2, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade " +
            "where grade_id in (104300507, 104300537)", Long.class));
        checkFraudLevel(104300507, 1);
        checkFraudLevel(104300537, 2);
    }

    @Test
    public void testConsuming() throws TimeoutException, InterruptedException {
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
                Collections.singletonList(generateMessageBatch(fraudInfoJsons)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        antifraudBanGradeLbImportExecutor.consume(syncConsumer);
        verify(syncConsumer).init();
        verify(syncConsumer, times(2)).read();
        verify(syncConsumer).commit(eq(consumerReadResponse.getCookie()));
        assertEquals(consumerReadResponse.getBatches().get(0).getMessageData().size(),
                (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
    }

    @Test
    public void testConsumingSameData() throws TimeoutException, InterruptedException {
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from model_transition", Long.class));
        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
                Collections.singletonList(generateMessageBatch(fraudInfoJsons)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        antifraudBanGradeLbImportExecutor.consume(syncConsumer);
        assertEquals(consumerReadResponse.getBatches().get(0).getMessageData().size(),
                (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        assertEquals(consumerReadResponse.getBatches().get(0).getMessageData().size(),
            (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade_last", Long.class));

        // consume again - log would bet bigger, last value should be still same
        syncConsumer = mockSyncConsumer(consumerReadResponse);
        antifraudBanGradeLbImportExecutor.consume(syncConsumer);
        assertEquals(consumerReadResponse.getBatches().get(0).getMessageData().size() * 2,
                (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        assertEquals(consumerReadResponse.getBatches().get(0).getMessageData().size(),
            (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade_last", Long.class));
    }

    @Test
    public void testCheckSaveWithInvalidJsonValue() throws TimeoutException, InterruptedException {
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        String invalidJson = "{ \"id\" : \"hello\", \"level\" : 1 }";
        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
                Collections.singletonList(generateMessageBatch(invalidJson)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        try {
            antifraudBanGradeLbImportExecutor.consume(syncConsumer);
            Assert.fail("Exception expected");
        } catch (RuntimeException ignored) {
        }
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
    }

    @Test
    public void testCheckSaveWithAbsentJsonProperty() throws TimeoutException, InterruptedException {
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        String invalidJson = "{\"level_hello\" : 1 }";
        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
                Collections.singletonList(generateMessageBatch(invalidJson)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        try {
            antifraudBanGradeLbImportExecutor.consume(syncConsumer);
            Assert.fail("Exception expected");
        } catch (RuntimeException ignored) {
        }
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
    }


    @Test
    public void testCheckSaveWithExtendedField() throws TimeoutException, InterruptedException {
        assertEquals(0, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
        String invalidJson = "{ \"id\" : 13, \"level\" : 1, \"extended_field\" : \"hello\"}";
        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
                Collections.singletonList(generateMessageBatch(invalidJson)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        antifraudBanGradeLbImportExecutor.consume(syncConsumer);
        assertEquals(1, (long) pgJdbcTemplate.queryForObject("select count(*) from antifraud_ban_grade", Long.class));
    }


    private MessageBatch generateMessageBatch(String fraudInfo) {
        return generateMessageBatch(Collections.singletonList(fraudInfo));
    }

    private MessageBatch generateMessageBatch(List<String> fraudInfos) {
        MessageMeta messageMeta = new MessageMeta(new byte[0], 0, 0, 0, "", CompressionCodec.RAW, Collections.emptyMap());
        List<MessageData> messageDataList = fraudInfos.stream().map(fraudInfo -> new MessageData(fraudInfo.getBytes(), 0, messageMeta)).collect(Collectors.toList());
        return new MessageBatch("", 1, messageDataList);
    }

    private SyncConsumer mockSyncConsumer(ConsumerReadResponse consumerReadResponse) throws InterruptedException {
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        when(syncConsumer.read()).thenReturn(consumerReadResponse, (ConsumerReadResponse) null);
        return syncConsumer;
    }
}
