package ru.yandex.market.gutgin.tms.utils;

import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.cleanweb.client.CWImageResult;
import ru.yandex.market.ir.autogeneration.common.util.LocalizedStringUtils;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.wrappers.pictures.PictureWrapper;

import static ru.yandex.market.partner.content.common.BaseDBStateGenerator.PARTNER_SHOP_ID;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.NAME;

public class TestUtils {

    private static final Long CATEGORY_ID = 1234L;

    public static ModelStorage.Model.Builder getBaseBuilder(long id, long modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .addTitles(LocalizedStringUtils.defaultString("старое имя"))
                .setCategoryId(CATEGORY_ID)
                .setCurrentType(ModelStorage.ModelType.PARTNER_SKU.name())
                .setSourceType(ModelStorage.ModelType.SKU.name())
                .setSupplierId(PARTNER_SHOP_ID)
                .setPublished(true)
                .setModifiedTs(100)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(modelId)
                        .setType(ModelStorage.RelationType.SKU_MODEL)
                        .build());
    }

    public static ModelStorage.Model generateSku(long id, long modelId) {
        return getBaseBuilder(id, modelId).build();
    }

    public static ModelStorage.Model generateSkuWithPictures(long id, long modelId, List<PictureWrapper> pictures) {
        return generateSkuWithPicturesAndSupplier(id, modelId, null, pictures);
    }

    public static ModelStorage.Model generateSkuWithPicturesAndSupplier(long id, long modelId, Long supplierId,
                                                                        List<PictureWrapper> pictures) {
        ModelStorage.Model.Builder builder = getBaseBuilder(id, modelId)
            .addAllPictures(pictures.stream().map(PictureWrapper::getPicture).collect(Collectors.toList()));
        if (supplierId != null) {
            builder.setSupplierId(supplierId);
        }
        return builder.build();
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, ModelStorage.ModificationSource type) {
        return buildPictureWrapper(url, md5, null, type);
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, ModelStorage.ModificationSource type,
                                                     int offerIndex) {
        return buildPictureWrapper(url, md5, null, type, offerIndex);
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, Long ownerId,
                                                     ModelStorage.ModificationSource type) {
        return buildPictureWrapper(url, md5, ownerId, type, 0);
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, Long ownerId,
                                                     ModelStorage.ModificationSource type,
                                                     int offerIndex) {
        ModelStorage.Picture.Builder pictureBuilder = ModelStorage.Picture.newBuilder()
                .setUrlOrig(url)
                .setValueSource(type)
                .setOrigMd5(md5);
        if (ownerId != null) {
            pictureBuilder.setOwnerId(ownerId);
        }
        ModelStorage.Picture picture = pictureBuilder.build();
        return PictureWrapper.forOffer(picture, offerIndex);
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, Long ownerId,
                                                     ModelStorage.ModificationSource type,
                                                     int width, int height, boolean isWhiteBackground,
                                                     CWImageResult cwResult) {
        return buildPictureWrapper(url, md5, ownerId, type, null, width, height, isWhiteBackground, cwResult, 0);
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, Long ownerId,
                                                     ModelStorage.ModificationSource type,
                                                     int width, int height, boolean isWhiteBackground,
                                                     CWImageResult cwResult, int offerIndex) {
        return buildPictureWrapper(url, md5, ownerId, type, null, width, height, isWhiteBackground, cwResult, offerIndex);
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, Long ownerId,
                                                     ModelStorage.ModificationSource type,
                                                     ModelStorage.PictureStatus status,
                                                     int width, int height, boolean isWhiteBackground,
                                                     CWImageResult cwResult) {
        return buildPictureWrapper(url, md5, ownerId, type, status, width, height, isWhiteBackground, cwResult, 0);
    }

    public static PictureWrapper buildPictureWrapper(String url, String md5, Long ownerId,
                                                     ModelStorage.ModificationSource type,
                                                     ModelStorage.PictureStatus status,
                                                     int width, int height, boolean isWhiteBackground,
                                                     CWImageResult cwResult, int offerIndex) {
        ModelStorage.Picture.Builder pictureBuilder = ModelStorage.Picture.newBuilder()
                .setUrlOrig(url)
                .setValueSource(type)
                .setOrigMd5(md5)
                .setWidth(width)
                .setHeight(height)
                .setIsWhiteBackground(isWhiteBackground);
        if (ownerId != null) {
            pictureBuilder.setOwnerId(ownerId);
        }
        if (status != null) {
            pictureBuilder.setPictureStatus(status);
        }
        ModelStorage.Picture picture = pictureBuilder.build();
        return PictureWrapper.forOffer(picture, cwResult, offerIndex);
    }

    public static PictureWrapper buildPictureWrapper(String url,
                                                     String md5,
                                                     ModelStorage.ModificationSource type,
                                                     ModelStorage.PictureStatus status) {
        ModelStorage.Picture picture = ModelStorage.Picture.newBuilder()
                .setUrlOrig(url)
                .setValueSource(type)
                .setOrigMd5(md5)
                .setPictureStatus(status)
                .build();
        return PictureWrapper.forOffer(picture, -1);
    }

    public static ModelStorage.Model generateModel(long modelId, long... skus) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setCategoryId(CATEGORY_ID)
                .setSourceType("PARTNER")
                .setCurrentType("GURU")
                .setSupplierId(PARTNER_SHOP_ID)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(NAME.getId())
                        .setXslName(NAME.getXslName())
                        .addStrValue(LocalizedStringUtils.defaultString(String.valueOf(modelId)))
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(modelId)
                        .build());

        for (long l : skus) {
            builder.addRelations(ModelStorage.Relation.newBuilder()
                    .setId(l)
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .build());
        }
        return builder.build();
    }
}
