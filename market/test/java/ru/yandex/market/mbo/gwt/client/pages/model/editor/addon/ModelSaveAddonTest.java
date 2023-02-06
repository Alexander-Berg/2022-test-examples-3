package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.image.BaseModelImageParamsAddonTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ParentImageRemoveRequestEvent;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;

/**
 * @author danfertev
 * @since 27.06.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelSaveAddonTest extends BaseModelImageParamsAddonTest {
    @Override
    public void model() {
        data.startModel()
            .title("Test model").id(10).category(11).vendorId(12)
            .currentType(CommonModel.Source.GURU)
            .modificationDate(new Date(1000000))
            .startParentModel()
                .title("Parent test model").id(1).category(11).vendorId(12)
                .currentType(CommonModel.Source.GURU)
                .modificationDate(new Date(1000000))
                .pictureParam(XslNames.XL_PICTURE, "xl_picture", 2, 2, "source", "orig")
                .picture(XslNames.XL_PICTURE, "xl_picture", 2, 2, "source", "orig")
            .endModel()
            .endModel();
    }

    @Test
    public void testLoadAndUpdateRelatedModels() {
        CommonModel modification1 = getGuruBuilder().id(11).parentModel(model).endModel();
        CommonModel modification2 = getGuruBuilder().id(12).parentModel(model).endModel();
        CommonModel modification3 = getGuruBuilder().id(13).parentModel(model).endModel();
        rpc.setGetModifications(Arrays.asList(model, modification1, modification2, modification3), null);

        rpc.setSaveModel(model.getId(), null);
        rpc.setLoadModel(new CommonModel(model.getParentModel()), null);

        bus.fireEvent(new ParentImageRemoveRequestEvent(XslNames.XL_PICTURE));
        bus.fireEvent(new SaveModelRequest());

        CommonModel savedModel = rpc.getSavedModel();
        List<CommonModel> savedRelatedModels = rpc.getRelatedModelsToSave();

        Assertions.assertThat(savedRelatedModels)
            .containsExactlyInAnyOrder(savedModel.getParentModel(), modification1, modification2, modification3);

        MboAssertions.assertThat(savedModel.getParentModel(), XslNames.XL_PICTURE).notExists();
        MboAssertions.assertThat(modification1, XslNames.XL_PICTURE).values("xl_picture");
        MboAssertions.assertThat(modification2, XslNames.XL_PICTURE).values("xl_picture");
        MboAssertions.assertThat(modification3, XslNames.XL_PICTURE).values("xl_picture");
    }
}
