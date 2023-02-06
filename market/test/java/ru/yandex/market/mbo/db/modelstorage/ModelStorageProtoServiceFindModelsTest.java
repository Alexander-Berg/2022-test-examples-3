package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import ru.yandex.market.http.ServiceException;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.index.util.CursorAwareResponse;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Model;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 09.08.2018
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class ModelStorageProtoServiceFindModelsTest {

    private static final long SEED = 718283223832L;
    private static final long MAX_SEARCH_PAGE = ModelStorageProtoService.MAX_SEARCH_PAGE;

    private ModelStorageProtoService service;

    @Mock
    private StatsModelStorageService storageService;

    @Mock
    private ModelStorageHealthService healthService;

    @Captor
    private ArgumentCaptor<Consumer<Model>> callbackCaptor;

    @Captor
    private ArgumentCaptor<MboIndexesFilter> filterCaptor;

    private Random random;

    @Before
    public void before() {
        random = new Random(SEED);
        service = new ModelStorageProtoService();
        service.setStorageService(storageService);
        service.setModelStorageHealthService(healthService);
    }

    @Test
    public void returnModelsFoundCountByIds() {
        List<Model> models = models(829);

        doAnswer(nothingButCallSimpleFiltered(callbackCaptor, filterCaptor, models))
            .when(storageService)
            .processQueryFullModels(filterCaptor.capture(), callbackCaptor.capture(), any(ReadStats.class));

        ModelStorage.GetModelsResponse response = service.findModels(ModelStorage.FindModelsRequest.newBuilder()
            .addAllModelIds(random.longs(models.size()).boxed().collect(toList()))
            .build());

        assertThat(response.getModelsFound()).isEqualTo(models.size());
        assertThat(response.getModelsList()).isEqualTo(models);
    }

    @Test
    public void returnModelsFoundByPage() {
        int modelsCount = (int) (MAX_SEARCH_PAGE + 965);
        List<Model> models = models(modelsCount);

        when(storageService.count(filterCaptor.capture(), any(ReadStats.class))).thenReturn((long) modelsCount);
        when(storageService.getFullModelsPage(any(), any(ReadStats.class)))
            .thenAnswer(modelsSimpleFilteredBy(filterCaptor, models));

        ModelStorage.GetModelsResponse response = service.findModels(ModelStorage.FindModelsRequest.newBuilder()
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.VENDOR_ID) // force page query
            .build());

        assertThat(response.getModelsFound()).isEqualTo(modelsCount);
        assertThat(response.getModelsCount()).isEqualTo((int) MAX_SEARCH_PAGE);
    }

    @Test
    public void returnModelsFoundByCursor() {
        int modelsCount = (int) (MAX_SEARCH_PAGE + 412);
        List<Model> models = models(modelsCount);

        when(storageService.count(filterCaptor.capture(), any(ReadStats.class))).thenReturn((long) modelsCount);
        when(storageService.getFullModelsCursor(any(), nullable(String.class), any(ReadStats.class)))
            .thenReturn(
                new CursorAwareResponse<>(
                    models.subList(0, (int) MAX_SEARCH_PAGE),
                    "next-cursor-mark"
                )
            );

        ModelStorage.GetModelsResponse response = service.findModels(ModelStorage.FindModelsRequest.newBuilder()
            .build());

        assertThat(response.getModelsFound()).isEqualTo(modelsCount);
        assertThat(response.getModelsCount()).isEqualTo((int) MAX_SEARCH_PAGE);
        assertThat(response.getNextCursorMark()).isEqualTo("next-cursor-mark");
    }

    @Test
    public void throwErrorAndStatsWritten() {
        ArgumentCaptor<OperationStats> statsCaptor = ArgumentCaptor.forClass(OperationStats.class);

        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .addAllModelIds(random.longs(MAX_SEARCH_PAGE + 1).boxed().collect(toList()))
            .build();

        assertThatThrownBy(() -> service.findModels(request))
            .hasCauseExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Failed to findModels with error: " +
                "IllegalArgumentException: Maximum allowed rows is " + MAX_SEARCH_PAGE)
            .isInstanceOf(ServiceException.class);

        verify(healthService).appendStats(statsCaptor.capture());

        OperationStats stats = statsCaptor.getValue();
        assertThat(stats.getMethod()).isEqualTo("FindModels");
        assertThat(stats.getService()).isEqualTo("ModelStorageService");
    }

    @Nonnull
    protected static Answer<List<Model>> modelsSimpleFilteredBy(ArgumentCaptor<MboIndexesFilter> queryCaptor,
                                                                List<Model> models) {
        return invocation -> getModelsSimpleFilteredBy(queryCaptor, models);
    }

    @Nonnull
    private static List<Model> getModelsSimpleFilteredBy(ArgumentCaptor<MboIndexesFilter> filterCaptor,
                                                         List<Model> models) {
        MboIndexesFilter filter = filterCaptor.getValue();
        int fromIndex = filter.getOffset(0);
        int toIndex = fromIndex + filter.getLimit(models.size());
        if (fromIndex == 0 && toIndex == models.size()) {
            return models;
        }
        return models.subList(
            fromIndex,
            Math.min(toIndex, models.size())
        );
    }

    private static List<Model> models(long count) {
        return Stream.generate(() -> Model.newBuilder().build())
            .limit(count)
            .collect(toList());
    }

    private static Answer<?> nothingButCallSimpleFiltered(ArgumentCaptor<Consumer<Model>> callbackCaptor,
                                                          ArgumentCaptor<MboIndexesFilter> filterCaptor,
                                                          List<Model> models) {
        return nothingButDo(() -> getModelsSimpleFilteredBy(filterCaptor, models).forEach(callbackCaptor.getValue()));
    }

    private static Answer<?> nothingButDo(Runnable runnable) {
        return (Answer) invocation -> {
            runnable.run();
            return null;
        };
    }
}
