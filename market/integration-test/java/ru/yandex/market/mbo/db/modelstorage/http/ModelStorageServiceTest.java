package ru.yandex.market.mbo.db.modelstorage.http;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageProtoService;
import ru.yandex.market.mbo.db.modelstorage.http.utils.ProtobufHelper;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import javax.annotation.Resource;

/**
 * @author astafurovme
 * @timestamp 1/12/16 7:10 PM
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:mbo-card-api/test-config.xml"})
public class ModelStorageServiceTest {

    @Resource(name = "modelStorageProtoService")
    private ModelStorageProtoService modelStorageService;

    // guru-category Test71
    private final long testHid = 910761;

    @Test
    @Ignore("MBO-14445")
    public void apiTest() throws Exception {
        // 1. Создание модели
        ModelStorage.ParameterValue.Builder vendorValue = ProtobufHelper.createValueBuilder(XslNames.VENDOR,
                7893318, Param.Type.ENUM);
        // Vendor - 24Seven
        vendorValue.setOptionId(3274877);
        vendorValue.setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
        ModelStorage.ParameterValue vendorParam = vendorValue.build();

        ModelStorage.ParameterValue.Builder nameValue = ProtobufHelper.createValueBuilder(XslNames.NAME,
            KnownIds.NAME_PARAM_ID, Param.Type.STRING);
        nameValue.addStrValue(ProtobufHelper.fromString("Model_storage_test"));
        nameValue.setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
        ModelStorage.ParameterValue nameParam = nameValue.build();

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
                .setCategoryId(testHid)
                .addParameterValues(vendorParam)
                .addParameterValues(nameParam)
                .setSourceType(CommonModel.Source.GURU.name())
                .setCurrentType(CommonModel.Source.GURU.name()).build();

        ModelStorage.SaveModelsRequest saveModelRequest =
                ModelStorage.SaveModelsRequest.newBuilder().addModels(model).build();

        ModelStorage.OperationResponse saveResponse = modelStorageService.saveModels(saveModelRequest);
        ModelStorage.OperationStatus saveOperationStatus = saveResponse.getStatuses(0);
        Assert.assertEquals(ModelStorage.OperationStatusType.OK, saveOperationStatus.getStatus());

        // 2. Поиск созданной модели
        long modelId = saveOperationStatus.getModelId();

        ModelStorage.GetModelsRequest findRequest = ProtobufHelper.getModelsRequestBuilder(testHid, modelId).build();
        ModelStorage.GetModelsResponse models = modelStorageService.getModels(findRequest);

        Assert.assertEquals(1, models.getModelsList().size());

        // 3. Удаляем созданную модель
        ModelStorage.RemoveModelsRequest removeModelsRequest = ProtobufHelper.removeModelsRequestBuilder(models
                .getModelsList().get(0)).build();
        ModelStorage.OperationResponse removeResponse = modelStorageService.removeModels(removeModelsRequest);

        Assert.assertNotEquals(0, removeResponse.getStatusesCount());
        Assert.assertEquals(ModelStorage.OperationStatusType.OK, removeResponse.getStatuses(0).getStatus());
    }
}
