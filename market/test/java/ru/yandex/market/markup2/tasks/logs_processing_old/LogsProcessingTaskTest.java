package ru.yandex.market.markup2.tasks.logs_processing_old;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.utils.aliasmaker.AliasMakerService;
import ru.yandex.market.markup2.utils.aliasmaker.ModelOffersAliases;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.offer.YtOffersReader;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.vendor.VendorService;
import ru.yandex.market.markup2.workflow.general.IdGenerator;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
import ru.yandex.market.markup2.workflow.taskDataUnique.FullTaskDataUniqueContext;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.markup2.tasks.logs_processing_old.LogsProcessingTestCommon.CATEGORY_ID;
import static ru.yandex.market.markup2.tasks.logs_processing_old.LogsProcessingTestCommon.VENDOR_ID;
import static ru.yandex.market.markup2.tasks.logs_processing_old.LogsProcessingTestCommon.commonMocks;
import static ru.yandex.market.markup2.tasks.logs_processing_old.LogsProcessingTestCommon.prepareResponseData;
import static ru.yandex.market.markup2.tasks.logs_processing_old.LogsProcessingTestCommon.verifySavedModel;
import static ru.yandex.market.markup2.tasks.logs_processing_old.LogsProcessingTestCommon.verifyTask;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createModel;
import static ru.yandex.market.markup2.utils.ModelTestUtils.mockModelStorageGetModel;
import static ru.yandex.market.markup2.utils.ModelTestUtils.mockModelStorageProcessModels;
import static ru.yandex.market.markup2.utils.ModelTestUtils.mockModelStorageSaveModels;
import static ru.yandex.market.markup2.utils.OfferTestUtils.createOffer;
import static ru.yandex.market.markup2.utils.OfferTestUtils.mockOffersReader;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 27.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LogsProcessingTaskTest {

    private LogsProcessingRequestGenerator generator = spy(new LogsProcessingRequestGenerator());
    private LogsProcessingResultCollector resultCollector = new LogsProcessingResultCollector();

    @Mock
    private ModelStorageService modelStorageService;
    @Mock
    private YtOffersReader ytOffersReader;
    @Mock
    private AliasMakerService aliasMakerService;
    @Mock
    private VendorService vendorService;
    @Mock
    private TovarTreeProvider tovarTreeProvider;

    @Captor
    ArgumentCaptor<List<ModelStorage.Model>> modelsCaptor;

    @Captor
    ArgumentCaptor<List<ModelOffersAliases>> modelOfferAliasesCaptor;

    private IdGenerator idGenerator = Markup2TestUtils.mockIdGenerator();

    ModelStorage.Model model1 = createModel(CATEGORY_ID, VENDOR_ID, 1L, "GURU", false);
    ModelStorage.Model model2 = createModel(CATEGORY_ID, VENDOR_ID, 2L, "GURU", true);
    ModelStorage.Model ethalon10 = createModel(CATEGORY_ID, VENDOR_ID, 10L, "GURU", false);
    ModelStorage.Model ethalon11 = createModel(CATEGORY_ID, VENDOR_ID, 11L, "GURU", true);
    List<ModelStorage.Model> models = new ArrayList<>();

    {
        models.addAll(Arrays.asList(model1, model2, ethalon10, ethalon11));
    }

    OffersStorage.GenerationDataOffer offer1Matched = createOffer(CATEGORY_ID, VENDOR_ID, "1", "1", 1L, null);
    OffersStorage.GenerationDataOffer offer2 = createOffer(CATEGORY_ID, VENDOR_ID, "2", "2", null, 100L);
    OffersStorage.GenerationDataOffer offer3 = createOffer(CATEGORY_ID, VENDOR_ID, "3", "3", null, 200L);
    OffersStorage.GenerationDataOffer offer4 = createOffer(CATEGORY_ID, VENDOR_ID, "4", "4", null, 200L);
    OffersStorage.GenerationDataOffer offer5WrongCategory = createOffer(100, 600L, "5", "5", 2L, null);
    OffersStorage.GenerationDataOffer offer6WrongVendor = createOffer(CATEGORY_ID, 100L, "6", "6", 2L, null);
    List<OffersStorage.GenerationDataOffer> offers = new ArrayList<>();

    {
        offers.addAll(Arrays.asList(offer5WrongCategory, offer6WrongVendor, offer1Matched, offer2, offer3, offer4));
    }

    @Before
    public void setup() {
        mockModelStorageGetModel(modelStorageService, models);
        mockModelStorageProcessModels(modelStorageService, models);
        mockModelStorageSaveModels(modelStorageService, new HashSet<>(Arrays.asList(0L, 1L)));

        mockOffersReader(ytOffersReader, offers);

        commonMocks(generator, Arrays.asList(ethalon10, ethalon11), vendorService, tovarTreeProvider);

        YtOfferDAO offerDAO = new YtOfferDAO();
        offerDAO.setOffersReader(ytOffersReader);

        generator.setModelStorageService(modelStorageService);
        generator.setOfferDAO(offerDAO);
        generator.setVendorService(vendorService);
        generator.setTovarTreeProvider(tovarTreeProvider);

        resultCollector.setModelStorageService(modelStorageService);
    }

    @Test
    public void testGenerate() {
        TaskInfo taskInfo = Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, 2, Collections.emptyMap());
        FullTaskDataUniqueContext<LogsProcessingDataIdentity> uniqueContext =
            Markup2TestUtils.createBasicUniqueContext();

        RequestGeneratorContext<LogsProcessingDataIdentity, LogsProcessingDataItemPayload, LogsProcessingResponse>
            generationContext = Markup2TestUtils.createGenerationContext(taskInfo, uniqueContext, idGenerator);

        generator.generateRequests(generationContext);

        Collection<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> items =
            generationContext.getTaskDataItems();

        assertEquals(2, items.size());

        Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> itemsIt = items.iterator();
        verifyTask(itemsIt.next(), offer2, Arrays.asList(ethalon10, ethalon11));
        verifyTask(itemsIt.next(), offer3, Arrays.asList(ethalon10, ethalon11));
    }

    @Test
    public void testProcessAddNameResults() {
        List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> items = prepareResponseData();
        TaskInfo taskInfo = Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, 4, Collections.emptyMap());

        ResultMakerContext<LogsProcessingDataIdentity, LogsProcessingDataItemPayload, LogsProcessingResponse>
            resultMakerContext = Markup2TestUtils.createResultMakerContext(taskInfo);
        resultMakerContext.addDataItems(items);

        resultCollector.makeResults(resultMakerContext);

        verify(modelStorageService).getModel(CATEGORY_ID, 1L);
        verify(modelStorageService).saveModels(modelsCaptor.capture());
        List<ModelStorage.Model> savedModels = modelsCaptor.getValue();

        assertEquals(3, savedModels.size());
        verifySavedModel(savedModels.get(0), "Qwerty");
        verifySavedModel(savedModels.get(1), "Model1");
        verifySavedModel(savedModels.get(2), "Model2");

        /* TODO Uncomment once alias maker is fully enabled
        verify(aliasMakerService).makeAliases(eq(CATEGORY_ID), eq(VENDOR_ID), modelOfferAliasesCaptor.capture());
        List<ModelOffersAliases> offersAliases = modelOfferAliasesCaptor.getValue();
        verifyModelOffersAliases(offersAliases, 1000L,
            new OfferAliasPair("1", "Qwerty", null),
            new OfferAliasPair("2", "Qwerty", null));
        verifyModelOffersAliases(offersAliases, 1L,
            new OfferAliasPair("3", "Model1", null));*/
    }
}

