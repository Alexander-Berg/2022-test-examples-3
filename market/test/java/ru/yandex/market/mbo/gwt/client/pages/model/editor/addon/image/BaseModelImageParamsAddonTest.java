package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.image;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;

/**
 * Базовый класс для всех картиночно-прааметрых адднов. Например, {@link ModelImageParamsAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public abstract class BaseModelImageParamsAddonTest extends AbstractModelTest {

    protected static final String XL_PICTURE_2 = "XL-Picture_2";

    @Override
    public void model() {
        data.startModel()
            .title("Test model").id(10).category(11).vendorId(12)
            .currentType(CommonModel.Source.GURU)
            .modificationDate(new Date(1000000))
            .pictureParam(XslNames.XL_PICTURE, "xl_picture", 2, 2, "source", "orig")
            .picture(XslNames.XL_PICTURE, "xl_picture", 2, 2, "source", "orig")
            .endModel();
    }

    @Override
    public void parameters() {
        super.parameters();
        data.startParameters()
            .imageParameters(XslNames.XL_PICTURE)
            .imageParameters(XL_PICTURE_2)
            .endParameters();
    }
}
