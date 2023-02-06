package ru.yandex.market.gutgin.tms.base;

import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.ir.autogeneration.common.util.LocalizedStringUtils;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.csku.util.OfferSpecialParameterCreator;
import ru.yandex.market.partner.content.common.csku.wrappers.pictures.PictureWrapper;

import static ru.yandex.market.partner.content.common.BaseDBStateGenerator.CATEGORY_ID;
import static ru.yandex.market.partner.content.common.BaseDBStateGenerator.PARTNER_SHOP_ID;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.NAME;

public class ModelGeneration {
    public static final String BARCODE_STR = "12";

    public static ModelStorage.Model generateModel(long modelId, long... skus) {
        return generateModel(false, modelId, skus);
    }

    public static ModelStorage.Model generateMModel(long modelId, long... skus) {
        return generateModel(true, modelId, skus);
    }

    public static ModelStorage.Model generateModel(boolean isMsku, long modelId, long... skus) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setCategoryId(CATEGORY_ID)
                .setSourceType(isMsku ? "GURU" : "PARTNER")
                .setCurrentType("GURU")
                .setSupplierId(PARTNER_SHOP_ID)
                .setPublished(true)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(NAME.getId())
                        .setXslName(NAME.getXslName())
                        .addStrValue(LocalizedStringUtils.defaultString(String.valueOf(modelId)))
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownParameters.BARCODE.getId())
                        .addStrValue(LocalizedStringUtils.defaultString(BARCODE_STR))
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownParameters.MODEL_QUALITY.getId())
                        .setOptionId((int) OfferSpecialParameterCreator.MODEL_QUALITY_PARTNER_OPTION_ID)
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(modelId)
                        .build());

        for (long l : skus) {
            builder.addRelations(ModelStorage.Relation.newBuilder()
                    .setId(l)
                    .setCategoryId(CATEGORY_ID)
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .build());
        }
        return builder.build();
    }

    public static ModelStorage.Model generateSku(long id, long modelId) {
        return getBaseBuilder(false, id, modelId).build();
    }

    public static ModelStorage.Model generateMsku(long id, long modelId) {
        return getBaseBuilder(true, id, modelId).build();
    }

    public static  ModelStorage.Model generateSkuWithPictures(long id, long modelId, List<PictureWrapper> pictures) {
        return getBaseBuilder(false, id, modelId)
                .addAllPictures(pictures.stream().map(PictureWrapper::getPicture).collect(Collectors.toList()))
                .build();
    }

    public static ModelStorage.Model.Builder getBaseBuilder(boolean isMsku, long id, long modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .addTitles(LocalizedStringUtils.defaultString("старое имя"))
                .setCategoryId(CATEGORY_ID)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(isMsku ? ModelStorage.ModelType.SKU.name() : ModelStorage.ModelType.PARTNER_SKU.name())
                .setSupplierId(PARTNER_SHOP_ID)
                .setPublished(true)
                .setModifiedTs(100)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownParameters.BARCODE.getId())
                        .addStrValue(LocalizedStringUtils.defaultString(BARCODE_STR))
                        .build())
                .addParameterValues(
                        ModelStorage.ParameterValue.newBuilder()
                                .setParamId(NAME.getId())
                                .setXslName(NAME.getXslName())
                                .addStrValue(LocalizedStringUtils.defaultString(String.valueOf(id)))
                                .build()
                )
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(modelId)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .setCategoryId(CATEGORY_ID)
                        .build());
    }

    public static ModelStorage.Model generateSkuWithPicturesAndQuality(long id, long modelId,
                                                                  List<PictureWrapper> pictures) {
        return getBaseBuilder(false, id, modelId)
                .addAllPictures(pictures.stream().map(PictureWrapper::getPicture).collect(Collectors.toList()))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownParameters.CONTENT_QUALITY.getId())
                        .setXslName(KnownParameters.CONTENT_QUALITY.getXslName())
                        .addStrValue(LocalizedStringUtils.defaultString(String.valueOf(0))))
                .build();
    }
}
