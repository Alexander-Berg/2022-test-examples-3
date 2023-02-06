package ru.yandex.market.pers.tms.logbroker.executor.save;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.actionId;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.generateModelTransition;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.id;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.oldEntityDeleted;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.primaryTransition;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.timestamp;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.transitionReason;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.transitionType;

public abstract class SaveTransitionExecutorTest extends MockedPersTmsTest {

    @Autowired
    @Qualifier("qaJdbcTemplate")
    JdbcTemplate qaJdbcTemplate;

    private MessageBatch generateMessageBatch(ModelStorage.ModelTransition modelTransition) {
        return generateMessageBatch(Collections.singletonList(modelTransition));
    }

    private MessageBatch generateMessageBatch(List<ModelStorage.ModelTransition> modelTransitions) {
        MessageMeta messageMeta = new MessageMeta(new byte[0],
            0,
            0,
            0,
            "",
            CompressionCodec.RAW,
            Collections.emptyMap());
        List<MessageData> messageDataList = modelTransitions.stream().map(modelTransition -> new MessageData(
            modelTransition.toByteArray(),
            0,
            messageMeta)).collect(Collectors.toList());
        return new MessageBatch("", 1, messageDataList);
    }

    private ConsumerReadResponse mockConsumerReadResponse(ModelStorage.ModelTransition.ModelType modelType) {
        int id = 0;
        List<ModelStorage.ModelTransition> list = new ArrayList<>();
        for (var transitionReason : ModelStorage.ModelTransition.TransitionReason.values()) {
            for (var transitionType : ModelStorage.ModelTransition.TransitionType.values()) {
                list.add(generateModelTransition(++id, modelType, transitionReason, transitionType));
            }
        }
        return new ConsumerReadResponse(Collections.singletonList(generateMessageBatch(list)), 1);
    }

    private SyncConsumer mockSyncConsumer(ConsumerReadResponse consumerReadResponse) throws InterruptedException {
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        when(syncConsumer.read()).thenReturn(consumerReadResponse, (ConsumerReadResponse) null);
        return syncConsumer;
    }

    public void testCheckSaveAfterConsumeSkeleton(SaveTransitionExecutor saveTransitionExecutor,
                                                  ModelStorage.ModelTransition.ModelType modelType) throws TimeoutException, InterruptedException {
        Assert.assertEquals(0, pgJdbcTemplate.queryForObject(
            "select count(*) from model_transition", Long.class).longValue());

        ModelStorage.ModelTransition modelTransition = generateModelTransition(id,
            modelType,
            transitionReason,
            transitionType);

        ConsumerReadResponse consumerReadResponse = new ConsumerReadResponse(
            List.of(generateMessageBatch(modelTransition)), 2);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        saveTransitionExecutor.consume(syncConsumer);
        assertTransitionsCount(1);
        Assert.assertEquals(1,
            pgJdbcTemplate.queryForObject(
                "select count(*) from model_transition " +
                    "where id = ? and action_id = ? and cr_time = ? and type = ? and reason = ? and entity_type = ? " +
                    "and old_entity_id = ? and old_entity_id_deleted = ? " +
                    "and new_entity_id = ? and primary_transition = ?",
                Long.class,
                id, actionId, timestamp, transitionType.getNumber(), transitionReason.getNumber(),
                modelType.getNumber(), modelTransition.getOldEntityId(), oldEntityDeleted,
                modelTransition.getNewEntityId(), primaryTransition).longValue());
    }

    public void testConsumingSkeleton(SaveTransitionExecutor saveTransitionExecutor,
                                      ModelStorage.ModelTransition.ModelType modelType) throws TimeoutException,
        InterruptedException {
        assertTransitionsCount(0);
        ConsumerReadResponse consumerReadResponse = mockConsumerReadResponse(modelType);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        saveTransitionExecutor.consume(syncConsumer);
        verify(syncConsumer).init();
        verify(syncConsumer, times(2)).read();
        verify(syncConsumer).commit(eq(consumerReadResponse.getCookie()));
        assertTransitionsCount(consumerReadResponse.getBatches().get(0).getMessageData().size());
    }

    public void testConsumingSameDataSkeleton(SaveTransitionExecutor saveTransitionExecutor,
                                              ModelStorage.ModelTransition.ModelType modelType) throws TimeoutException, InterruptedException {
        assertTransitionsCount(0);
        ConsumerReadResponse consumerReadResponse = mockConsumerReadResponse(modelType);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        saveTransitionExecutor.consume(syncConsumer);
        assertTransitionsCount(consumerReadResponse.getBatches().get(0).getMessageData().size());
        syncConsumer = mockSyncConsumer(consumerReadResponse);
        saveTransitionExecutor.consume(syncConsumer);
        assertTransitionsCount(consumerReadResponse.getBatches().get(0).getMessageData().size());
    }

    public void testErrorWhileConsumingSkeleton(SaveTransitionExecutor saveTransitionExecutor,
                                                ModelStorage.ModelTransition.ModelType modelType) throws TimeoutException, InterruptedException {
        ConsumerReadResponse consumerReadResponse = mockConsumerReadResponse(modelType);
        SyncConsumer syncConsumer = mockSyncConsumer(consumerReadResponse);
        doThrow(new RuntimeException("something bad happened in qa db")).when(qaJdbcTemplate).batchUpdate(anyString(),
            anyList(),
            anyInt(),
            any());
        try {
            saveTransitionExecutor.consume(syncConsumer);
            Assert.fail("Exception expected");
        } catch (RuntimeException ignored) {
        }
        verify(syncConsumer).init();
        verify(syncConsumer, times(1)).read();
        verify(syncConsumer, times(0)).commit(anyLong());
    }

    private void assertTransitionsCount(int expected) {
        Assert.assertEquals(expected,
            (long) pgJdbcTemplate.queryForObject("select count(*) from model_transition", Long.class));
    }


}