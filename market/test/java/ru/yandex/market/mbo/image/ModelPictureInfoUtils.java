package ru.yandex.market.mbo.image;

import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelPictureInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import java.util.ArrayList;
import java.util.List;

/**
 * @author danfertev
 * @since 04.07.2018
 */
public class ModelPictureInfoUtils {
    private ModelPictureInfoUtils() {
    }

    public static Picture picture(String xslName, String url, Integer height, Integer width,
                                  String urlSource, String urlOrig) {
        Picture picture = new Picture();
        picture.setXslName(xslName);
        picture.setUrl(url);
        picture.setHeight(height);
        picture.setWidth(width);
        picture.setUrlSource(urlSource);
        picture.setUrlOrig(urlOrig);
        picture.setModificationSource(ModificationSource.OPERATOR_FILLED);
        return picture;
    }

    public static List<ModelInfo> modelInfo(CommonModel... model) {
        List<ModelInfo> result = new ArrayList<>();
        for (CommonModel m : model) {
            result.add(ModelInfo.from(m));
        }
        return result;
    }

    public static ModelPictureInfo modelPictureInfo(Picture picture, CommonModel... model) {
        return new ModelPictureInfo(picture, modelInfo(model));
    }
}
