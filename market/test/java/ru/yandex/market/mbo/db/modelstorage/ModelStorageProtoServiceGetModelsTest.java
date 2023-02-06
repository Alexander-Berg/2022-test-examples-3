package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.http.ServiceException;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.08.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelStorageProtoServiceGetModelsTest extends ModelStorageProtoServiceTestBase {

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void modelsReturned() throws Exception {
        int modelsCount = 15;
        List<Model> models = Stream.generate(this::model)
            .limit(modelsCount)
            .map(Model.Builder::build)
            .collect(Collectors.toList());

        List<Long> modelIds = models.stream().map(Model::getId).collect(Collectors.toList());

        when(storageService.getModelStore()).thenReturn(modelStore);
        when(modelStore.getModels(anyLong(), anyCollection(), any()))
            .thenReturn(models);

        ModelStorage.GetModelsRequest request = ModelStorage.GetModelsRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .addAllModelIds(modelIds)
            .build();

        ModelStorage.GetModelsResponse response = protoService.getModels(request);

        verify(modelStore).getModels(eq(CATEGORY_ID), eq(modelIds), any());
        assertThat(response.getModelsFound()).isEqualTo(modelsCount);
        assertThat(response.getModelsList()).isEqualTo(models);
    }

    @Test
    public void statsWrittenIfFailed() throws Exception {
        ArgumentCaptor<OperationStats> healthStatsCaptor = ArgumentCaptor.forClass(OperationStats.class);
        ArgumentCaptor<ReadStats> readStatsCaptor = ArgumentCaptor.forClass(ReadStats.class);
        Exception exception = new RuntimeException("exception-message");
        when(storageService.getModelStore()).thenReturn(modelStore);
        when(modelStore.getModels(anyLong(), anyCollection(), readStatsCaptor.capture())).thenThrow(exception);

        assertThatThrownBy(() ->
            protoService.getModels(ModelStorage.GetModelsRequest.newBuilder().setCategoryId(CATEGORY_ID).build())
        ).isInstanceOf(ServiceException.class)
            .hasMessage("Failed to getModels with error: RuntimeException: exception-message")
            .hasCause(exception);


        verify(modelStorageHealthService).appendStats(healthStatsCaptor.capture());
        OperationStats healthStats = healthStatsCaptor.getValue();
        assertThat(healthStats.getService()).isEqualTo("ModelStorageService");
        assertThat(healthStats.getMethod()).isEqualTo("GetModels");
        assertThat(healthStats.getReadStats()).isSameAs(readStatsCaptor.getValue());
    }

}

