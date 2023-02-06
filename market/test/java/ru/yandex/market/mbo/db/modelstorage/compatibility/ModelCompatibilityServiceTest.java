package ru.yandex.market.mbo.db.modelstorage.compatibility;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.compatibility.audit_billing.ModelCompatibilityAuditService;
import ru.yandex.market.mbo.db.modelstorage.compatibility.dao.CompatibilityBuilder;
import ru.yandex.market.mbo.db.modelstorage.compatibility.dao.ModelCompatibilityDAO;
import ru.yandex.market.mbo.db.modelstorage.compatibility.dao.ModelCompatibilityDAOMock;
import ru.yandex.market.mbo.db.modelstorage.compatibility.validation.ModelCompatibilityValidationService;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.vendor.GlobalVendorBuilder;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorServiceMock;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModel;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModelBuilder;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Тест, проверяющий корректность работы сервиса совместимости.
 *
 * @author s-ermakov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ModelCompatibilityServiceTest {

    private CommonModel model1;
    private CommonModel model2;
    private CommonModel model3;
    private CommonModel model4;

    private static final GlobalVendor VENDOR = GlobalVendorBuilder.newBuilder(100, "my vendor").build();
    private static final TovarCategory TOVAR_CATEGORY = new TovarCategory("my category", 10, 0);

    private static final long USER_ID = 27069701L;

    private ModelCompatibilityService modelCompatibilityService;

    private ModelCompatibilityDAO modelCompatibilityDAO;

    private GlobalVendorService globalVendorService;

    private TovarTreeService tovarTreeService;

    private ModelStorageService modelStorageService;

    @Mock
    private ModelCompatibilityAuditService modelCompatibilityAuditService;

    @Before
    public void setUp() throws Exception {
        model1 = CommonModelBuilder.newBuilder(1, 10, 100).getModel();
        model2 = CommonModelBuilder.newBuilder(2, 10, 100).getModel();
        model3 = CommonModelBuilder.newBuilder(3, 10, 100).getModel();
        model4 = CommonModelBuilder.newBuilder(4, 10, 100).getModel();

        modelCompatibilityService = new ModelCompatibilityService();
        globalVendorService = new GlobalVendorServiceMock(VENDOR);

        modelStorageService = new ModelStorageServiceStub(model1, model2, model3, model4) {
            @Override
            public void processQueryModels(MboIndexesFilter query, Consumer<CommonModel> processor) {
                // query contains model ids
                modelsMap.values().forEach(processor);
            }
        };
        modelCompatibilityDAO = new ModelCompatibilityDAOMock();
        tovarTreeService = new TovarTreeServiceMock(TOVAR_CATEGORY);

        modelCompatibilityService.setModelCompatibilityDAO(modelCompatibilityDAO);
        modelCompatibilityService.setModelStorageService(modelStorageService);
        modelCompatibilityService.setVendorService(globalVendorService);
        modelCompatibilityService.setTovarTreeService(tovarTreeService);
        modelCompatibilityService.setModelCompatibilityValidationService(new ModelCompatibilityValidationService());
        modelCompatibilityService.setModelCompatibilityAuditService(modelCompatibilityAuditService);

        // init dao
        Compatibility compatibility1To2 = CompatibilityBuilder.newBuilder(1, 2,
            Compatibility.Direction.FORWARD).create();
        Compatibility compatibility1To3 = CompatibilityBuilder.newBuilder(1, 3,
            Compatibility.Direction.BOTH).create();
        Compatibility compatibility2To3 = CompatibilityBuilder.newBuilder(2, 3,
            Compatibility.Direction.FORWARD).create();
        modelCompatibilityDAO.saveCompatibilities(Arrays.asList(compatibility1To2, compatibility1To3,
            compatibility2To3));
    }

    @Test
    public void getModelCompatibilitiesFromOne() throws Exception {
        List<CompatibilityModel> compatibilityModels = modelCompatibilityService.getModelCompatibilities(model1);

        assertEquals(2, compatibilityModels.size());
        assertCompatibility(model1, model2, CompatibilityModel.Direction.FORWARD, model1,
            compatibilityModels.get(0));
        assertCompatibility(model1, model3, CompatibilityModel.Direction.BOTH, model1, compatibilityModels.get(1));
    }

    @Test
    public void getModelCompatibilitiesFromTwo() throws Exception {
        List<CompatibilityModel> compatibilityModels = modelCompatibilityService.getModelCompatibilities(model2);

        assertEquals(2, compatibilityModels.size());
        assertCompatibility(model2, model1, CompatibilityModel.Direction.BACKWARD, model2,
            compatibilityModels.get(0));
        assertCompatibility(model2, model3, CompatibilityModel.Direction.FORWARD, model2,
            compatibilityModels.get(1));
    }

    @Test
    public void getModelCompatibilitiesFromThree() throws Exception {
        List<CompatibilityModel> compatibilityModels = modelCompatibilityService.getModelCompatibilities(model3);

        assertEquals(2, compatibilityModels.size());
        assertCompatibility(model3, model1, CompatibilityModel.Direction.BOTH, model3, compatibilityModels.get(0));
        assertCompatibility(model3, model2, CompatibilityModel.Direction.BACKWARD, model3,
            compatibilityModels.get(1));
    }

    @Test
    public void saveCompatibilities() throws Exception {
        CompatibilityModel compatibilityModel = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model4.getId(), model4.getTitle())
            .create();

        modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatibilityModel));

        List<CompatibilityModel> compatibilityModelsFrom1 = modelCompatibilityService.getModelCompatibilities(model1);
        List<CompatibilityModel> compatibilityModelsFrom4 = modelCompatibilityService.getModelCompatibilities(model4);

        assertEquals(1, compatibilityModelsFrom1.size());
        assertEquals(1, compatibilityModelsFrom4.size());

        assertCompatibility(model1, model4, CompatibilityModel.Direction.BACKWARD, model1,
            compatibilityModelsFrom1.get(0));
        assertCompatibility(model4, model1, CompatibilityModel.Direction.FORWARD, model4,
            compatibilityModelsFrom4.get(0));
    }

    @Test
    public void dontReturnInvalidIfModelMissed() {
        CompatibilityModel compatibilityModel = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model4.getId(), model4.getTitle())
            .create();

        modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatibilityModel));

        modelStorageService.deleteModel(model4, USER_ID);
        modelCompatibilityDAO.markCompatibilitiesValid(model4.getId(), false);

        List<CompatibilityModel> models = modelCompatibilityService.getModelCompatibilities(model1);

        assertThat(models).isEmpty();
    }

    @Test
    public void dontReturnInvalidIfTargetDeleted() {
        CompatibilityModel compatibilityModel = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model4.getId(), model4.getTitle())
            .create();

        model4.setDeleted(true);
        modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatibilityModel));

        modelCompatibilityDAO.markCompatibilitiesValid(model4.getId(), false);

        List<CompatibilityModel> models = modelCompatibilityService.getModelCompatibilities(model1);

        assertThat(models).isEmpty();
    }

    @Test
    public void returnInvalidIfModelExists() {
        CompatibilityModel compatibilityModel = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model4.getId(), model4.getTitle())
            .create();

        modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatibilityModel));

        // don't delete model, but mark invalid
        modelCompatibilityDAO.markCompatibilitiesValid(model4.getId(), false);

        List<CompatibilityModel> models = modelCompatibilityService.getModelCompatibilities(model1);

        assertThat(models).hasSize(1);
        assertCompatibility(model1, model4, CompatibilityModel.Direction.BACKWARD, model1, models.get(0));
    }

    @Test
    public void recover() {
        CompatibilityModel compatibilityModel = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model4.getId(), model4.getTitle())
            .create();

        modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatibilityModel));

        // don't delete model, but mark invalid
        modelCompatibilityDAO.markCompatibilitiesValid(model4.getId(), false);
        assertThat(modelCompatibilityDAO.getInvalidCompatibilities()).hasSize(1);


        modelCompatibilityService.cleanupInvalidOrRecover();

        List<Compatibility> compatibilities = modelCompatibilityDAO.getCompatibilitiesByModelId(model4.getId());
        assertThat(compatibilities).hasSize(1);
        assertThat(compatibilities.get(0).isValid()).isTrue();
    }

    @Test
    public void recoverConcurrent() {
        CompatibilityModel compatToModel4 = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model4.getId(), model4.getTitle())
            .create();

        CompatibilityModel compatToModel3 = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model3.getId(), model3.getTitle())
            .create();

        ModelCompatibilityDAO dao = spy(this.modelCompatibilityDAO);
        modelCompatibilityService.setModelCompatibilityDAO(dao);
        modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatToModel4));

        // don't delete model, but mark invalid
        this.modelCompatibilityDAO.markCompatibilitiesValid(model1.getId(), false);
        assertThat(this.modelCompatibilityDAO.getInvalidCompatibilities()).hasSize(1);


        // updated compatibilities concurrently
        when(dao.deleteCompatibilities(anyCollection())).then((Answer<Integer>) invocation -> {
            modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatToModel3));
            return (Integer) invocation.callRealMethod();
        });
        modelCompatibilityService.cleanupInvalidOrRecover();


        List<Compatibility> compatibilities = modelCompatibilityDAO.getCompatibilitiesByModelId(model1.getId());
        assertThat(compatibilities).hasSize(1);

        List<CompatibilityModel> models = modelCompatibilityService.getModelCompatibilities(model1);

        assertThat(models).hasSize(1);
        assertCompatibility(model1, model3, CompatibilityModel.Direction.BACKWARD, model1, models.get(0));
    }

    @Test
    public void cleanup() {
        CompatibilityModel compatibilityModel = CompatibilityModelBuilder.newBuilder()
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .setModel(model4.getId(), model4.getTitle())
            .create();

        ModelCompatibilityDAO dao = spy(this.modelCompatibilityDAO);
        modelCompatibilityService.setModelCompatibilityDAO(dao);
        modelCompatibilityService.saveModelCompatibilities(0, model1, Collections.singletonList(compatibilityModel));

        modelStorageService.deleteModel(model4, USER_ID);
        this.modelCompatibilityDAO.markCompatibilitiesValid(model4.getId(), false);
        assertThat(this.modelCompatibilityDAO.getInvalidCompatibilities()).hasSize(1);


        modelCompatibilityService.cleanupInvalidOrRecover();


        List<Compatibility> compatibilities = modelCompatibilityDAO.getCompatibilitiesByModelId(model4.getId());
        assertThat(compatibilities).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getCompatibilitiesWithPublishedModels() throws Exception {
        CommonModel unpublishedModel = CommonModelBuilder.newBuilder(model3.getId(), model3.getCategoryId(),
            model3.getVendorId())
            .published(false)
            .getModel();

        modelStorageService.saveModel(unpublishedModel, 0);

        List<Compatibility> compatibilities = modelCompatibilityService.getCompatibilitiesBetweenPublishedModels();

        assertEquals(1, compatibilities.size());
        assertCompatibility(model1, model2, CompatibilityModel.Direction.FORWARD, compatibilities.get(0));
    }

    private void assertCompatibility(CommonModel from, CommonModel to, CompatibilityModel.Direction direction,
                                     CommonModel base, CompatibilityModel compatibilityModel) {
        assertEquals(from.getId(), base.getId());
        assertEquals(to.getId(), compatibilityModel.getModelId());
        assertEquals(direction, compatibilityModel.getDirection());
    }

    private void assertCompatibility(CommonModel from, CommonModel to, CompatibilityModel.Direction direction,
                                     Compatibility compatibility) {
        assertEquals(from.getId(), compatibility.getModelId1());
        assertEquals(to.getId(), compatibility.getModelId2());
        assertEquals(direction.toString(), compatibility.getDirection().toString());
    }
}
