package ru.yandex.market.mbo.cardrender.app.service.yql;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cardrender.app.model.base.DeleteModelRequest;
import ru.yandex.market.mbo.cardrender.app.model.saas.DeleteModelSaasRow;
import ru.yandex.market.mbo.cardrender.app.repository.YtModelRenderRepository;
import ru.yandex.market.mbo.cardrender.app.service.DatacampService;
import ru.yandex.market.mbo.cardrender.app.service.SaasPushService;
import ru.yandex.market.mbo.cardrender.app.task.model.ModelCategoryWithHideType;
import ru.yandex.market.mbo.cardrender.app.task.model.ModelCategoryWithTimestamp;
import ru.yandex.market.mbo.gwt.models.modelstorage.CategoryModelId;

import static org.mockito.Mockito.times;
import static ru.yandex.market.mbo.cardrender.app.task.model.ModelCategoryWithHideType.HideType.DELETE;
import static ru.yandex.market.mbo.cardrender.app.task.model.ModelCategoryWithHideType.HideType.HIDE_FROM_STUFF_MODEL;
import static ru.yandex.market.mbo.cardrender.app.task.model.ModelCategoryWithHideType.HideType.HIDE_FROM_STUFF_SKU;

/**
 * @author apluhin
 * @created 1/18/22
 */
public class SyncDeletedYqlReaderTest {

    SaasPushService saasPushService;
    DatacampService datacampService;
    SyncDeletedYqlReader syncDeletedYqlReader;
    YtModelRenderRepository ytModelRenderRepository;

    @Before
    public void setUp() throws Exception {
        saasPushService = Mockito.mock(SaasPushService.class);
        ytModelRenderRepository = Mockito.mock(YtModelRenderRepository.class);
        datacampService = Mockito.mock(DatacampService.class);
        syncDeletedYqlReader = new SyncDeletedYqlReader(
                null,
                null,
                saasPushService,
                datacampService,
                ytModelRenderRepository
        );
    }

    @Test
    public void testSelectSql() {
        String test = syncDeletedYqlReader.selectSql(100L, "test");
        Assertions.assertThat(test).isEqualTo("select " +
                "model_id, " +
                "category_id, " +
                "change_ts, " +
                "type " +
                "from test " +
                "order by model_id limit 200000000 offset 100");
    }

    @Test
    public void testHandleBatch() {
        var hideModel = new ModelCategoryWithHideType(new CategoryModelId(1L,
                1L), 1L, HIDE_FROM_STUFF_MODEL);
        var hideSku = new ModelCategoryWithHideType(new CategoryModelId(1L,
                2L), 1L, HIDE_FROM_STUFF_SKU);
        var deleteModel = new ModelCategoryWithHideType(new CategoryModelId(1L,
                3L), 1L, DELETE);
        syncDeletedYqlReader.handleBatch(List.of(
                hideModel,
                hideSku,
                deleteModel
        ));

        ArgumentCaptor<List> delayRowsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(saasPushService, times(2)).sendDeleteWithDelayRows(delayRowsCaptor.capture());

        ArgumentCaptor<List> delayIds = ArgumentCaptor.forClass(List.class);
        Mockito.verify(saasPushService, times(1)).sendDeleteWithDelay(delayIds.capture());

        ArgumentCaptor<List> datacampDelete = ArgumentCaptor.forClass(List.class);
        Mockito.verify(datacampService, times(1)).sendDeleteModels(datacampDelete.capture());

        ArgumentCaptor<List> deleteSku = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytModelRenderRepository, times(1)).deleteSku(deleteSku.capture());

        ArgumentCaptor<List> deleteModels = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytModelRenderRepository, times(1)).deleteModels(deleteModels.capture());


        LocalDateTime ignore = LocalDateTime.now();
        //assert hide models
        Assertions.assertThat(delayRowsCaptor.getAllValues().get(0).get(0))
                .isEqualToIgnoringGivenFields(DeleteModelSaasRow.modelDelete(1L, ignore), "deleted");
        Assertions.assertThat(deleteModels.getValue().get(0))
                .isEqualToIgnoringGivenFields(new DeleteModelRequest(1L, 1L, 1L));

        //assert hide sku
        Assertions.assertThat(delayRowsCaptor.getAllValues().get(1).get(0))
                .isEqualToIgnoringGivenFields(DeleteModelSaasRow.skuDelete(2L, ignore), "deleted");
        Assertions.assertThat(deleteSku.getValue().get(0))
                .isEqualToIgnoringGivenFields(new DeleteModelRequest(2L, 1L,1L));

        //assert delete model
        Assertions.assertThat(delayIds.getAllValues().get(0).get(0))
                .isEqualToIgnoringGivenFields(3L);
        Assertions.assertThat(datacampDelete.getValue().get(0))
                .isEqualToIgnoringGivenFields(new ModelCategoryWithTimestamp(3L, 1L, 1L));
    }
}
