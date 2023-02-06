package ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelStore;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.ModelStorageHolderPayload;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.ParamValueSearch;
import ru.yandex.market.mbo.gwt.models.modelstorage.CategoryModelId;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.http.ModelStorage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author apluhin
 * @created 11/20/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class FilteredModelStorageReaderTest {

    private YtModelStore modelStore;
    private FilteredModelStorageReader reader;
    private ReadStats stats = new ReadStats();

    @Before
    public void setUp() throws Exception {
        modelStore = Mockito.mock(YtModelStore.class);
        reader = new FilteredModelStorageReader(modelStore);
    }

    @Test
    public void testFitModels() {
        Mockito.when(modelStore.getModels(Mockito.any(), Mockito.any())).thenReturn(Arrays.asList(model()));

        ParameterValues numericMatch = new ParameterValues(
            0L,
            "someNumberXslName",
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(BigDecimal.valueOf(Double.parseDouble("100.5")))
        );
        List<ModelStorageHolderPayload> modelStorageHolderPayloads = reader.selectFiltered(
            new ArrayList<>(),
            new MboIndexesFilter().setModelId(100L).setCategoryId(150L)
                .addAttribute(new ParamValueSearch(numericMatch)),
            stats);
        Assertions.assertThat(modelStorageHolderPayloads.size()).isEqualTo(1);
    }

    @Test
    public void testFilterModel() {
        Mockito.when(modelStore.getModels(Mockito.any(), Mockito.any())).thenReturn(Arrays.asList(model()));

        List<ModelStorageHolderPayload> modelStorageHolderPayloads = reader.selectFiltered(
            new ArrayList<>(),
            new MboIndexesFilter().setCategoryId(101L),
            stats);
        Assertions.assertThat(modelStorageHolderPayloads.size()).isEqualTo(0);
    }

    @Test
    public void testInsertedArguments() {
        ArgumentCaptor<ReadStats> captorStats = ArgumentCaptor.forClass(ReadStats.class);
        ArgumentCaptor<List> captorModelsIds = ArgumentCaptor.forClass(List.class);
        Mockito.when(modelStore.getModels(captorModelsIds.capture(), captorStats.capture()))
            .thenReturn(Collections.emptyList());

        List<CategoryModelId> categoryModelIds = Arrays.asList(new CategoryModelId(1L, 2L));
        reader.selectFiltered(categoryModelIds,
            new MboIndexesFilter(), stats);

        Assertions.assertThat(captorStats.getValue()).isEqualTo(stats);
        Assertions.assertThat(captorModelsIds.getValue()).isEqualTo(categoryModelIds);
    }

    private ModelStorage.Model model() {
        return ModelStorage.Model.newBuilder()
            .setId(100L)
            .setCategoryId(150L)
            .addAllParameterValues(Arrays.asList(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName("someNumberXslName")
                    .setParamId(-2)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setNumericValue("100.5")
                    .build()
                )
            ).build();
    }
}
