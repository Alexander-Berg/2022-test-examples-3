package ru.yandex.market.mbo.tms.modeltransfer.processor;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryPair;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelsConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ModelTransferList;
import ru.yandex.market.mbo.gwt.models.transfer.step.TextResult;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.solr.update.UpdateModelIndexException;
import ru.yandex.market.mbo.solr.update.UpdateModelIndexInterface;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gwt.models.transfer.ResultInfo.Status.COMPLETED;
import static ru.yandex.market.mbo.gwt.models.transfer.ResultInfo.Status.FAILED;

public class ReindexModelStepProcessorTest {

    @InjectMocks
    private ReindexModelStepProcessor processor;
    @Mock
    private UpdateModelIndexInterface updateModelIndexService;
    @Mock
    private ModelStoreInterface modelStore;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(modelStore.getModels(anyLong(), anyList())).thenReturn(Arrays.asList(
            ModelStorage.Model.newBuilder().build(),
            ModelStorage.Model.newBuilder().build()
        ));
    }

    @Test
    public void executeStep() {
        final TextResult textResult = processor.executeStep(new ResultInfo(),
            new ModelTransferJobContext<>(new ModelTransfer(),
                new ModelTransferStepInfo(),
                new ArrayList<>(),
                buildConfig(),
                new ArrayList<>()));
        assertEquals(COMPLETED, textResult.getResultInfo().getStatus());
    }

    @Test
    public void executeStepFail() throws UpdateModelIndexException {
        doThrow(UpdateModelIndexException.class).when(updateModelIndexService).index(anyList());
        final TextResult textResult = processor.executeStep(new ResultInfo(),
            new ModelTransferJobContext<>(new ModelTransfer(),
                new ModelTransferStepInfo(),
                new ArrayList<>(),
                buildConfig(),
                new ArrayList<>()));
        assertEquals(FAILED, textResult.getResultInfo().getStatus());
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private ListOfModelParameterLandingConfig buildConfig() {
        final ListOfModelParameterLandingConfig config =
            new ListOfModelParameterLandingConfig();
        final ListOfModelsConfig listOfModelsConfig = new ListOfModelsConfig();
        listOfModelsConfig.setEntitiesList(Arrays.asList(
            new ModelTransferList(new CategoryPair(1, 2), Arrays.asList(1L, 2L), ""),
            new ModelTransferList(new CategoryPair(2, 3), Arrays.asList(1L, 2L), "")
        ));
        config.setListOfModelsConfig(listOfModelsConfig);
        return config;
    }
}
