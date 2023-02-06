package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.StorageUpdates;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.SignModificationsPreprocessorTest;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

/**
 * Тесты проверяют корректность взаимодействия {@link StorageUpdates} и {@link AutoOperatorSignService}.
 * Здесь находятся более общие проверки.
 * Более детальные проверки стоит искать в {@link SignModificationsPreprocessorTest}.
 *
 * @author s-ermakov
 */
public class AutoOperatorSignGroupStorageTest extends BaseGroupStorageUpdatesTest {
    private static final long CATEGORY_HID = 666;

    private static final Long SIGN_TRUE_ID = 1001L;
    private static final Long SIGN_FALSE_ID = 1002L;

    @Override
    protected CategoryParametersServiceClient createCategoryParametersServiceClient() {
        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setHid(CATEGORY_HID);

        categoryEntities.addParameter(CategoryParamBuilder.newBuilder(1, XslNames.OPERATOR_SIGN)
            .setType(Param.Type.BOOLEAN)
            .addOption(OptionBuilder.newBuilder(SIGN_TRUE_ID).addName("TRUE"))
            .addOption(OptionBuilder.newBuilder(SIGN_FALSE_ID).addName("falSe"))
            .setUseForGuru(true)
            .build());
        categoryEntities.addParameter(
            CategoryParamBuilder.newBuilder(2, XslNames.PREVIEW)
                .setType(Param.Type.STRING)
                .setUseForGuru(true)
                .build()
        );
        return CategoryParametersServiceClientStub.ofCategoryEntities(categoryEntities);
    }

    @Test
    public void savingModelWithTrueAutoSignWillAutomaticallySignModification() {
        CommonModel modelInStorage = createGuruModel(1, CATEGORY_HID, 1, builder -> {
            builder.putParameterValues(signValue(false));
        });
        CommonModel modificationInStorage = createGuruModel(2, CATEGORY_HID, 1, builder -> {
            builder.parentModelId(1);
        });
        putToStorage(modelInStorage, modificationInStorage);

        modelInStorage.putParameterValues(signValue(true));
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(modelInStorage);
        GroupOperationStatus groupOperationStatus = storage.saveModels(modelSaveGroup, context);

        CommonModel updatedModel = groupOperationStatus.getRequestedModel(1).get();
        MboAssertions.assertThat(updatedModel, XslNames.OPERATOR_SIGN).values(true);

        Assert.assertEquals(1, groupOperationStatus.getAdditionalModelStatues().size());
        CommonModel modification = groupOperationStatus.getAdditionalModel(2).get();
        MboAssertions.assertThat(modification, XslNames.OPERATOR_SIGN).values(true);
    }

    @Test
    public void savingModelWithTrueAutoSignWillAutomaticallySignModifications() {
        context.setSource(AuditAction.Source.MBO);

        CommonModel modelInStorage = createGuruModel(1, CATEGORY_HID, 1, builder -> {
            builder.putParameterValues(signValue(false));
        });
        CommonModel modificationInStorage = createGuruModel(2, CATEGORY_HID, 1, builder -> {
            builder.parentModelId(modelInStorage.getId());
        });
        CommonModel newModification = createGuruModel(0, CATEGORY_HID, 1, builder -> {
            builder.parentModelId(modelInStorage.getId());
            builder.putParameterValues(signValue(false));
        });
        putToStorage(modelInStorage, modificationInStorage);

        modelInStorage.putParameterValues(signValue(true));

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(modelInStorage, newModification);
        GroupOperationStatus groupOperationStatus = storage.saveModels(modelSaveGroup, context);

        Assert.assertEquals(2, groupOperationStatus.getRequestedModelStatuses().size());
        Assert.assertEquals(1, groupOperationStatus.getAdditionalModelStatues().size());

        CommonModel updatedModel = groupOperationStatus.getRequestedModel(modelInStorage.getId()).get();
        MboAssertions.assertThat(updatedModel, XslNames.OPERATOR_SIGN).values(true);

        CommonModel createdModification = groupOperationStatus.getRequestedModelByIndex(1).get();
        MboAssertions.assertThat(createdModification, XslNames.OPERATOR_SIGN).values(true);

        CommonModel updatedModification = groupOperationStatus.getAdditionalModel(modificationInStorage.getId()).get();
        MboAssertions.assertThat(updatedModification, XslNames.OPERATOR_SIGN).values(true);
    }

    @Test
    public void savingModelWithTrueAutoSignWontCangeDeletedModifications() {
        CommonModel modelInStorage = createGuruModel(1, CATEGORY_HID, 1, builder -> {
            builder.putParameterValues(signValue(true));
        });
        CommonModel modificationInStorage = createGuruModel(2, CATEGORY_HID, 1, builder -> {
            builder.parentModelId(1);
        });
        putToStorage(modelInStorage, modificationInStorage);

        modificationInStorage.setDeleted(true);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(modelInStorage, modificationInStorage);
        GroupOperationStatus groupOperationStatus = storage.saveModels(modelSaveGroup, context);

        Assert.assertEquals(2, groupOperationStatus.getRequestedModelStatuses().size());

        CommonModel updatedModel = groupOperationStatus.getRequestedModel(1).get();
        MboAssertions.assertThat(updatedModel, XslNames.OPERATOR_SIGN).values(true);

        CommonModel updatedModification = groupOperationStatus.getRequestedModel(2).get();
        MboAssertions.assertThat(updatedModification, XslNames.OPERATOR_SIGN).notExists();
    }

    private ParameterValues signValue(boolean value) {
        return new ParameterValues(1, XslNames.OPERATOR_SIGN, Param.Type.BOOLEAN, value,
            value ? SIGN_TRUE_ID : SIGN_FALSE_ID);
    }
}
