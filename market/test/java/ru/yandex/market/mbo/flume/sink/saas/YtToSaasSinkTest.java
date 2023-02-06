package ru.yandex.market.mbo.flume.sink.saas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.flume.channel.SimpleEventBuilder;
import ru.yandex.market.mbo.flume.sink.ModelNotFoundException;
import ru.yandex.market.mbo.flume.sink.YtReader;
import ru.yandex.market.mbo.http.MboFlumeNg;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.index.UpdateModelIndexException;

/**
 * @author moskovkin@yandex-team.ru
 * @since 14.06.18
 */
public class YtToSaasSinkTest {
    private YtToSaasSink sink;

    private SaasUpdateModelIndexService index;
    private YtReader ytReader;
    private static final long DEFAULT_CATEGORY_ID = 1L;

    @Before
    public void init() {
        ytReader = Mockito.mock(YtReader.class);
        index = Mockito.mock(SaasUpdateModelIndexService.class);

        sink = new YtToSaasSink();
        sink.setYtReader(ytReader);
        sink.setModelIndexService(index);
    }

    private void mockIndexProcess() throws InvalidProtocolBufferException, ModelNotFoundException {
        // Mock index process
        Mockito.doAnswer(invocation -> {
            Set<YtReader.ModelKey> keys = invocation.getArgument(0);
            List<ModelStorage.Model> result = keys.stream()
                    .map(k -> ModelStorage.Model.newBuilder()
                            .setCategoryId(k.getCategoryId())
                            .setId(k.getModelId())
                            .setModifiedTs(2L)
                            .setCurrentType("GURU")
                            .build())
                    .collect(Collectors.toList());
            return result;
        })
                .when(ytReader)
                .readModels(Mockito.any());
    }

    @Test
    public void testCorrectBatch()
            throws InvalidProtocolBufferException, UpdateModelIndexException, EventDeliveryException {
        // Test data
        List<Event> events = Arrays.asList(createEvent(1L, DEFAULT_CATEGORY_ID, false, "1"),
                createEvent(2L, DEFAULT_CATEGORY_ID, false, "1"));

        mockIndexProcess();

        List<ModelStorage.Model> modelsToIndex = new ArrayList<>();
        Mockito.doAnswer(invocation -> {
            Collection<ModelStorage.Model> models = invocation.getArgument(0);
            modelsToIndex.addAll(models);
            return null;
        })
                .when(index)
                .index(Mockito.any());

        sink.doProcessEvents(events);

        List<ModelStorage.Model> expectedModels = Arrays.asList(
                ModelStorage.Model.newBuilder()
                        .setCategoryId(DEFAULT_CATEGORY_ID)
                        .setId(1L)
                        .setCurrentType("GURU")
                        .setModifiedTs(2L)
                        .build(),
                ModelStorage.Model.newBuilder()
                        .setCategoryId(DEFAULT_CATEGORY_ID)
                        .setId(2L)
                        .setModifiedTs(2L)
                        .setCurrentType("GURU")
                        .build()
        );

        Assert.assertArrayEquals(expectedModels.toArray(), modelsToIndex.toArray());
    }

    @Test(expected = EventDeliveryException.class)
    public void testOldModel() throws InvalidProtocolBufferException, EventDeliveryException {
        // Test data
        List<Event> events = Collections.singletonList(
                createEvent(1, DEFAULT_CATEGORY_ID, false, "2")
        );

        // Mock index process
        Mockito.doAnswer(invocation -> {
            Set<YtReader.ModelKey> keys = invocation.getArgument(0);
            List<ModelStorage.Model> result = keys.stream()
                    .map(k -> ModelStorage.Model.newBuilder()
                            .setCategoryId(k.getCategoryId())
                            .setId(k.getModelId())
                            .setModifiedTs(1L)
                            .setCurrentType("GURU")
                            .build())
                    .collect(Collectors.toList());
            return result;
        })
                .when(ytReader)
                .readModels(Mockito.any());

        sink.doProcessEvents(events);
    }

    @Test(expected = EventDeliveryException.class)
    public void testReadError()
            throws InvalidProtocolBufferException, EventDeliveryException {
        // Test data
        List<Event> events = Collections.singletonList(
                createEvent(1L, DEFAULT_CATEGORY_ID, false, "2")
        );

        Mockito.doThrow(new InvalidProtocolBufferException("Test exception"))
                .when(ytReader)
                .readModels(Mockito.any());

        sink.doProcessEvents(events);
    }

    @Test(expected = EventDeliveryException.class)
    public void testIndexError()
            throws InvalidProtocolBufferException, UpdateModelIndexException, EventDeliveryException {
        // Test data
        List<Event> events = Collections.singletonList(
                createEvent(1L, DEFAULT_CATEGORY_ID, false, "1")
        );

        mockIndexProcess();

        Mockito.doThrow(new UpdateModelIndexException("Test exception"))
                .when(index)
                .index(Mockito.any());

        sink.doProcessEvents(events);
    }

    private Event createEvent(long id, boolean toDelete) {
        return createEvent(id, DEFAULT_CATEGORY_ID, toDelete, "1");
    }

    private Event createEvent(long id, long categoryId, boolean toDelete, String expectedModifDate) {
        return SimpleEventBuilder.aSimpleEvent()
                .setBody(MboFlumeNg.ModelUpdateEvent.newBuilder()
                        .setCategoryId(categoryId)
                        .setId(id)
                        .setToDelete(toDelete)
                        .build()
                        .toByteArray()
                )
                .addHeader("expected_modified_date", expectedModifDate)
                .build();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testBatchWithRemoveEvents()
            throws InvalidProtocolBufferException, UpdateModelIndexException, EventDeliveryException {
        // Test data
        List<Event> events = Arrays.asList(
                createEvent(1, true),
                createEvent(2, false),
                createEvent(3, true),
                createEvent(4, false),
                createEvent(5, true));

        mockIndexProcess();

        List<ModelStorage.Model> modelsToIndex = new ArrayList<>();
        List<ModelStorage.Model> modelsToRemove = new ArrayList<>();
        Mockito.doAnswer(invocation -> {
            Collection<ModelStorage.Model> models = invocation.getArgument(0);
            modelsToIndex.addAll(models);
            return null;
        })
                .when(index)
                .index(Mockito.any());

        Mockito.doAnswer(invocation -> {
            Collection<ModelStorage.Model> models = invocation.getArgument(0);
            modelsToRemove.addAll(models);
            return null;
        })
                .when(index)
                .removeIndex(Mockito.any());


        List<ModelStorage.Model> expectedIndexModels = Arrays.asList(
                ModelStorage.Model.newBuilder()
                        .setCategoryId(DEFAULT_CATEGORY_ID)
                        .setId(2L)
                        .setCurrentType("GURU")
                        .setModifiedTs(2L)
                        .build(),
                ModelStorage.Model.newBuilder()
                        .setCategoryId(DEFAULT_CATEGORY_ID)
                        .setId(4L)
                        .setModifiedTs(2L)
                        .setCurrentType("GURU")
                        .build()
        );

        sink.doProcessEvents(events);

        List<ModelStorage.ModelOrBuilder> expectedRemoveModels = Arrays.asList(
                ModelStorage.Model.newBuilder()
                        .setCategoryId(DEFAULT_CATEGORY_ID)
                        .setId(1L).build(),
                ModelStorage.Model.newBuilder()
                        .setCategoryId(DEFAULT_CATEGORY_ID)
                        .setId(3L).build(),
                ModelStorage.Model.newBuilder()
                        .setCategoryId(DEFAULT_CATEGORY_ID)
                        .setId(5L).build()
        );

        Assert.assertArrayEquals(expectedIndexModels.toArray(), modelsToIndex.toArray());
        Assert.assertArrayEquals(expectedRemoveModels.toArray(), modelsToRemove.toArray());
    }
}
