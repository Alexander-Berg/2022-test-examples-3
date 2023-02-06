package ru.yandex.market.mbo.db.modelstorage.index.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.http.MboFlumeNg;
import ru.yandex.market.mbo.http.MboFlumeNgService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.solr.update.UpdateModelIndexException;

/**
 * @author moskovkin@yandex-team.ru
 * @since 06.02.18
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class FlumeHttpUpdateModelIndexServiceTest {
    private static final ModelStorage.Model MODEL1 = ModelStorage.Model.newBuilder()
        .setModifiedTs(1)
        .setId(1)
        .setCategoryId(2)
        .build();

    private static final ModelStorage.Model MODEL2 = ModelStorage.Model.newBuilder()
        .setModifiedTs(2)
        .setId(2)
        .setCategoryId(3)
        .build();

    private static final List<ModelStorage.Model> TEST_BATCH = Arrays.asList(MODEL1, MODEL2);
    public static final int BATCH_SIZE = 10;

    @Mock
    private MboFlumeNgService flumeService;
    private FlumeHttpUpdateModelIndexService modelUpdateIndexService;


    @Before
    public void init() {
        modelUpdateIndexService =
            new FlumeHttpUpdateModelIndexService(flumeService, false, false, BATCH_SIZE);
    }

    private MboFlumeNg.ModelUpdateEvent findEventForModel(
        List<MboFlumeNg.ModelUpdateEvent> events,
        ModelStorage.Model model
    ) {
        MboFlumeNg.ModelUpdateEvent result = events.stream().filter(modelUpdateEvent ->
            model.getId() == modelUpdateEvent.getId()
                && model.getCategoryId() == modelUpdateEvent.getCategoryId()
                && model.getModifiedTs() == modelUpdateEvent.getModifiedTimestamp())
            .findAny()
            .orElse(null);
        return result;
    }

    @Test
    public void testIndex() throws UpdateModelIndexException {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            MboFlumeNg.ReindexRequest request = invocation.getArgument(0);

            Assert.assertEquals(2, request.getEventCount());

            MboFlumeNg.ModelUpdateEvent modelEvent1 = findEventForModel(request.getEventList(), MODEL1);
            Assert.assertNotNull(modelEvent1);

            MboFlumeNg.ModelUpdateEvent modelEvent2 = findEventForModel(request.getEventList(), MODEL2);
            Assert.assertNotNull(modelEvent2);

            return null;
        })
        .when(flumeService)
        .reindex(Mockito.any());

        modelUpdateIndexService.index(TEST_BATCH);
        Mockito.verify(flumeService, Mockito.times(1)).reindex(Mockito.any());
    }

    @Test
    public void testBatchSize() throws UpdateModelIndexException {
        List<ModelStorage.Model> models = Stream.generate(() -> MODEL1)
            .limit(BATCH_SIZE * 3 + BATCH_SIZE / 2)
            .collect(Collectors.toList());

        List<Integer> sentBatchSizes = new ArrayList<>();
        Mockito.doAnswer((Answer<Void>) invocation -> {
            MboFlumeNg.ReindexRequest request = invocation.getArgument(0);
            sentBatchSizes.add(request.getEventCount());
            return null;
        })
            .when(flumeService)
            .reindex(Mockito.any());

        modelUpdateIndexService.index(models);
        Mockito.verify(flumeService, Mockito.times(4)).reindex(Mockito.any());
        Assert.assertThat(sentBatchSizes, Matchers.contains(BATCH_SIZE, BATCH_SIZE, BATCH_SIZE, BATCH_SIZE / 2));
    }

    @Test
    public void testRetryIndexRequest() throws UpdateModelIndexException {
        Mockito.when(flumeService.reindex(Mockito.any()))
            .thenThrow(RuntimeException.class)
            .thenThrow(RuntimeException.class)
            .thenReturn(Mockito.any());

        List<ModelStorage.Model> models = Collections.singletonList(MODEL1);
        modelUpdateIndexService.index(models);
        Mockito.verify(flumeService, Mockito.times(3)).reindex(Mockito.any());
    }

    @Test(expected = UpdateModelIndexException.class)
    public void testExhaustedRetryIndexRequest() throws UpdateModelIndexException {
        Mockito.when(flumeService.reindex(Mockito.any())).thenThrow(RuntimeException.class);

        List<ModelStorage.Model> models = Collections.singletonList(MODEL1);
        modelUpdateIndexService.index(models);
    }
}
