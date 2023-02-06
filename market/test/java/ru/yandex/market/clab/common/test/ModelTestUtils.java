package ru.yandex.market.clab.common.test;

import ru.yandex.market.clab.common.mbo.ProtoUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import static ru.yandex.market.clab.common.mbo.ProtoUtils.defaultWord;

/**
 * @author anmalysh
 * @since 11/29/2018
 */
public class ModelTestUtils {

    public static final long CATEGORY_ID = 1L;

    public static ModelStorage.Model.Builder model(long id) {
        return ModelStorage.Model.newBuilder()
            .setCurrentType(ProtoUtils.GURU)
            .setCategoryId(CATEGORY_ID)
            .setId(id);
    }

    public static ModelStorage.Model.Builder sku(long id) {
        return ModelStorage.Model.newBuilder()
            .setId(id)
            .setCategoryId(CATEGORY_ID)
            .setCurrentType(ProtoUtils.SKU);
    }

    public static ModelStorage.ParameterValue.Builder parameterValue(long id, String xslName,
                                                                     MboParameters.ValueType type,
                                                                     ModelStorage.ModificationSource source) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(id)
            .setXslName(xslName)
            .setValueType(type)
            .setValueSource(source);
    }

    public static ModelStorage.ParameterValue.Builder boolValue(long id, String xslName, boolean value) {
        return parameterValue(id, xslName, MboParameters.ValueType.BOOLEAN,
            ModelStorage.ModificationSource.OPERATOR_FILLED)
            .setBoolValue(value);
    }

    public static ModelStorage.ParameterValue.Builder clabBoolValue(long id, String xslName, boolean value) {
        return parameterValue(id, xslName, MboParameters.ValueType.BOOLEAN,
            ModelStorage.ModificationSource.CONTENT_LAB)
            .setBoolValue(value);
    }

    public static ModelStorage.ParameterValue.Builder enumValue(long id, String xslName, int optionId) {
        return parameterValue(id, xslName, MboParameters.ValueType.ENUM,
            ModelStorage.ModificationSource.OPERATOR_FILLED)
            .setOptionId(optionId);
    }

    public static ModelStorage.ParameterValue.Builder clabEnumValue(long id, String xslName, int optionId) {
        return parameterValue(id, xslName, MboParameters.ValueType.ENUM,
            ModelStorage.ModificationSource.CONTENT_LAB)
            .setOptionId(optionId);
    }

    public static ModelStorage.ParameterValue.Builder pickerValue(long id, String xslName, int optionId) {
        return enumValue(id, xslName, optionId)
            .setImagePickerSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
    }

    public static ModelStorage.ParameterValue.Builder clabPickerValue(long id, String xslName, int optionId) {
        return enumValue(id, xslName, optionId)
            .setImagePickerSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
    }

    public static MboParameters.PickerImage.Builder picker(String url) {
        return MboParameters.PickerImage.newBuilder()
                .setUrl(url);
    }

    public static ModelStorage.Picture.Builder picture(String url, String xslName) {
        return skuPicture(url)
            .setXslName(xslName);
    }

    public static ModelStorage.Picture.Builder skuPicture(String url) {
        return ModelStorage.Picture.newBuilder()
            .setUrl(url)
            .setUrlOrig(url + "orig")
            .setUrlSource(url + "source")
            .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
    }

    public static ModelStorage.Picture.Builder clabPicture(String url, String xslName) {
        return picture(url, xslName)
            .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB);
    }

    public static ModelStorage.Picture.Builder clabSkuPicture(String url) {
        return skuPicture(url)
            .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB);
    }

    public static ModelStorage.EnumValueAlias.Builder alias(long id, long optionId, long aliasId) {
        return ModelStorage.EnumValueAlias.newBuilder()
            .setParamId(id)
            .setXslName("Param" + id)
            .setOptionId(optionId)
            .setAliasOptionId(aliasId)
            .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
    }

    public static ModelStorage.EnumValueAlias.Builder clabAlias(long id, long optionId, long aliasId) {
        return alias(id, optionId, aliasId)
            .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB);
    }

    public static MboParameters.Parameter.Builder enumParam(long id, String xslName, String name) {
        return MboParameters.Parameter.newBuilder()
            .setId(id)
            .setXslName(xslName)
            .addName(defaultWord(name));
    }

    public static MboParameters.Option enumOption(long id, String name) {
        return MboParameters.Option.newBuilder()
            .setId(id)
            .addName(defaultWord(name))
            .build();
    }
}
