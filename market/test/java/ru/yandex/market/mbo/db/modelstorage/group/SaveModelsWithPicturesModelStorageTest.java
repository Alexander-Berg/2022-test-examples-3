package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.randomizers.PictureRandomizer;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.utils.MboAssertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 28.08.2018
 */
public class SaveModelsWithPicturesModelStorageTest extends BaseGroupStorageUpdatesTest {

    private static final long SEED = 385221342;
    private static final long CATEGORY_ID = 1597565803;

    private PictureRandomizer pictureRandomizer;

    private CommonModel existsModel;
    private List<CategoryParam> parameters;
    private Picture existsPicture;

    @Before
    @Override
    public void before() {
        parameters = ParametersBuilder.startParameters()
            .imageParameters(XslNames.XL_PICTURE)
            .endParameters();

        super.before();

        pictureRandomizer = PictureRandomizer.aNewRandomizer(SEED);

        existsPicture = pictureRandomizer.getRandomValue();
        existsPicture.setUrl("://exists-picture/");

        existsModel = CommonModelBuilder.newBuilder(idGenerator.getId(), CATEGORY_ID, 1)
            .parameters(parameters)
            .picture(existsPicture)
            .pictureParam(existsPicture)
            .getModel();

        existsModel.getFlatParameterValues().forEach(setModificationSource(ModificationSource.OPERATOR_FILLED));

        putToStorage(existsModel);
    }

    @Test
    public void replacePictureShouldOverwriteParameterValues() {
        Picture changedPicture = pictureRandomizer.getRandomValue();
        changedPicture.setXslName(null);
        changedPicture.setUrl("://changed-picture/");

        CommonModel changedModel = CommonModelBuilder.newBuilder(existsModel.getId(), CATEGORY_ID, 1)
            .parameters(parameters)
            .picture(changedPicture)
            .getModel();

        changedModel.getFlatParameterValues().forEach(setModificationSource(ModificationSource.VENDOR_OFFICE));

        context.setReplacePictures(true);
        context.setMerge(true);
        context.setOperationSource(ModificationSource.VENDOR_OFFICE);

        ModelSaveGroup group = ModelSaveGroup.fromModels(changedModel);

        GroupOperationStatus groupOperationStatus = storage.saveModels(group, context);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.OK);

        CommonModel model = storage.getModel(CATEGORY_ID, changedModel.getId(), new ReadStats())
            .orElseThrow(() -> new AssertionError("model is empty"));

        assertThat(model.getPictures()).containsExactly(changedPicture);
        assertThat(model.getParameterValues(XslNames.XL_PICTURE))
            .values(changedPicture.getUrl())
            .modificationSource(changedPicture.getModificationSource());
    }

    private static Consumer<ParameterValue> setModificationSource(ModificationSource modificationSource) {
        return parameterValue -> parameterValue.setModificationSource(modificationSource);
    }

    @Override
    protected CategoryParametersServiceClient createCategoryParametersServiceClient() {
        return CategoryParametersServiceClientStub.ofCategoryParams(CATEGORY_ID, parameters);
    }
}
