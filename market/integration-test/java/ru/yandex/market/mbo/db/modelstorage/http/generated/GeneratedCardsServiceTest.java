package ru.yandex.market.mbo.db.modelstorage.http.generated;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.OperationStatusUtils;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.http.ModelCardApiServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.http.ModelCardApiServiceTest;
import ru.yandex.market.mbo.db.modelstorage.http.utils.ProtobufHelper;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.user.AutoUser;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author amaslak
 * @timestamp 9/27/15 7:10 PM
 */
@Ignore("MBO-15344")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:mbo-card-api/test-config.xml"})
@SuppressWarnings("checkstyle:MagicNumber")
public class GeneratedCardsServiceTest {

    private static final Logger log = Logger.getLogger(ModelCardApiServiceTest.class);

    private static final List<Long> TEST_CATEGORIES = Arrays.asList(
        91013L, // ноутбуки
        6427100L, // планшеты
        90555L, // наушники
        91491L, // телефоны
        8367360L, // gps трекеры
        91033L, // Жесткие диски, SSD и сетевые накопители
        459013L); // Телефоны / Запасные части
    private static final long VENDOR_ID = 153043L; // Apple

    private static final int MAX_ROWS = 10000;

    private static final String DEFAULT_NEW_MODEL_DESCR = "New";
    private static final String TEST_MODEL_PREFIX = "INTEGRATION_TEST_";

    @Autowired
    @Qualifier("autoUser")
    private AutoUser autoUser;

    @Resource(name = "modelStorageService")
    private ModelStorageService modelStorageService;

    @Autowired
    @Qualifier("modelCardApiService")
    private ModelCardApiServiceImpl modelCardApiService;

    @Before
    @After
    public void setUp() throws Exception {
        List<CommonModel> testModels = loadTestGeneratedModels(TEST_CATEGORIES);

        log.debug("Removing models: " + testModels.stream().map(m -> String.valueOf(m.getId()))
            .collect(Collectors.joining(", ")));

        List<GroupOperationStatus> operationStatuses = modelStorageService.deleteModels(testModels,
            new ModelSaveContext(autoUser.getId()));
        OperationStatusUtils.throwExceptionIfFailed(operationStatuses.stream()
            .map(GroupOperationStatus::getSingleModelStatus)
            .collect(Collectors.toList()));
    }

    @Test
    public void apiTest() throws Exception {
        Map<Long, SaveGeneratedModelsTask> saveTasks = TEST_CATEGORIES.stream()
            .collect(Collectors.toMap(Function.identity(), categoryId -> {
                    SaveGeneratedModelsTask task = new SaveGeneratedModelsTask(categoryId, VENDOR_ID);
                    task.setModelCardApiService(modelCardApiService);
                    task.setGenerateModelFunction(this::generateModel);
                    task.setLoadGenerationModelsFunction(this::loadTestGeneratedModels);
                    return task;
                })
            );

        invokeInParallel(saveTasks.values());

        // Сравниваем данные с тем что записали в статистику в процессе импорта
        for (Long testCategory : TEST_CATEGORIES) {
            SaveGeneratedModelsTask saveGeneratedModelsTask = saveTasks.get(testCategory);
            int expectedAliveModelsCount = saveGeneratedModelsTask.getCurrentlyExistingModels();
            int actualAliveModelsCount = loadTestGeneratedModels(testCategory).size();
            assertEquals("Real number of existing test models in storage doesn't equal to requested ones.",
                expectedAliveModelsCount, actualAliveModelsCount);
        }
    }

    private static void invokeInParallel(Collection<SaveGeneratedModelsTask> saveTasks)
        throws InterruptedException, ExecutionException {
        ExecutorService service = Executors.newFixedThreadPool(saveTasks.size());
        List<Future<Void>> futures = service.invokeAll(saveTasks);

        // вызываем get(), чтобы отловить исключение, если оно произошло внутри
        for (Future future : futures) {
            future.get();
        }
    }

    private List<CommonModel> loadTestGeneratedModels(Collection<Long> categoryIds) {
        return categoryIds.stream()
            .flatMap(categoryId -> loadTestGeneratedModels(categoryId).stream())
            .collect(Collectors.toList());
    }

    private List<CommonModel> loadTestGeneratedModels(long categoryId) {
        List<CommonModel> result = new ArrayList<>();
        modelStorageService.processAllCategoryModels(
            categoryId,
            CommonModel.Source.GENERATED,
            false,
            MAX_ROWS,
            model -> {
                String title = model.getTitle();
                if (title.startsWith(TEST_MODEL_PREFIX)) {
                    result.add(model);
                }
            });
        return result;
    }

    private CommonModel generateModel(String nameSuffix, long categoryId, long vendorId) {
        CommonModel model = new CommonModel();
        model.setCategoryId(categoryId);
        model.setSource(CommonModel.Source.GENERATED);
        model.setCurrentType(CommonModel.Source.GENERATED);
        model.setModifiedUserId(autoUser.getId());

        // title
        ParameterValues nameValue = new ParameterValues(
            ProtobufHelper.NAME_PARAM_ID,
            XslNames.NAME,
            Param.Type.STRING,
            WordUtil.defaultWord(TEST_MODEL_PREFIX + nameSuffix));
        nameValue.setModificationSource(ModificationSource.AUTO);
        nameValue.setLastModificationDate(new Date());
        nameValue.setLastModificationUid(autoUser.getId());

        // vendor
        ParameterValues vendorValue = new ParameterValues(
            ProtobufHelper.VENDOR_PARAM_ID,
            XslNames.VENDOR,
            Param.Type.ENUM,
            vendorId);
        vendorValue.setModificationSource(ModificationSource.AUTO);
        vendorValue.setLastModificationDate(new Date());
        vendorValue.setLastModificationUid(autoUser.getId());

        // operator comment
        ParameterValues operatorComment = new ParameterValues(
            ProtobufHelper.OPERATOR_COMMENT_PARAM_ID,
            XslNames.OPERATOR_COMMENT,
            Param.Type.STRING,
            WordUtil.defaultWord(DEFAULT_NEW_MODEL_DESCR));
        operatorComment.setModificationSource(ModificationSource.AUTO);
        operatorComment.setLastModificationDate(new Date());
        operatorComment.setLastModificationUid(autoUser.getId());

        model.putParameterValues(nameValue);
        model.putParameterValues(vendorValue);
        model.putParameterValues(operatorComment);
        return model;
    }
}
