package ru.yandex.market.ir.autogeneration.common.util;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

/**
 * @author s-ermakov
 */
public class ModelBuilder {
    public static final long NAME_PARAM_ID = 100000L;
    public static final long VENDOR_PARAM_ID = 123456L;
    private static final long GC_ORIGINAL_SHOP_SKU_PARAM_ID = 16796681L;
    private static final int PIC_SIZE = 300;

    private ModelStorage.Model.Builder model = ModelStorage.Model.newBuilder();

    ModelBuilder() {
        currentType("GURU");
        source("GURU");
        published(true);
    }

    public static ModelBuilder newBuilder() {
        return new ModelBuilder();
    }

    public static ModelBuilder newBuilder(long id, long categoryId) {
        return newBuilder()
            .id(id)
            .category(categoryId);
    }

    public static ModelBuilder newBuilder(long id, long categoryId, long vendorId) {
        return newBuilder()
            .id(id)
            .category(categoryId)
            .vendorId(vendorId);
    }

    public ModelBuilder title(String title) {
        ModelStorage.LocalizedString titleValue = LocalizedStringUtils.defaultString(title);
        model.addTitles(titleValue);
        ModelStorage.ParameterValue param = ModelStorage.ParameterValue.newBuilder()
            .setParamId(NAME_PARAM_ID)
            .setXslName("name")
            .setValueType(MboParameters.ValueType.STRING)
            .addStrValue(titleValue)
            .build();
        parameterValue(param);
        return this;
    }

    public ModelBuilder id(long id) {
        model.setId(id);
        return this;
    }

    public ModelBuilder category(long category) {
        model.setCategoryId(category);
        return this;
    }

    public ModelBuilder supplierId(long supplierId) {
        model.setSupplierId(supplierId);
        return this;
    }

    public ModelBuilder shopSku(String shopSku) {
        ModelStorage.ParameterValue shopSkuParam = ModelStorage.ParameterValue.newBuilder()
                .setParamId(GC_ORIGINAL_SHOP_SKU_PARAM_ID)
                .setXslName(CategoryData.GC_ORIGINAL_SHOP_SKU)
                .setValueType(MboParameters.ValueType.STRING)
                .addStrValue(LocalizedStringUtils.defaultString(shopSku))
                .build();
        return parameterValue(shopSkuParam);
    }

    public ModelBuilder vendorId(long vendorId) {
        ModelStorage.ParameterValue vendorParam = ModelStorage.ParameterValue.newBuilder()
            .setParamId(VENDOR_PARAM_ID)
            .setXslName("vendor")
            .setValueType(MboParameters.ValueType.ENUM)
            .setOptionId((int) vendorId)
            .build();
        return parameterValue(vendorParam);
    }

    public ModelBuilder parentId(long parentModelId) {
        this.model.setParentId(parentModelId);
        return this;
    }

    public ModelBuilder parentModel(ModelStorage.Model parentModel) {
        return parentId(parentModel.getId());
    }

    public ModelBuilder parameterValues(ModelStorage.ParameterValue... values) {
        return parameterValues(Arrays.asList(values));
    }

    public ModelBuilder parameterValues(Collection<ModelStorage.ParameterValue> values) {
        for (ModelStorage.ParameterValue value : values) {
            parameterValue(value);
        }
        return this;
    }

    public ModelBuilder parameterValues(long paramId, String xslName, String... values) {
        ModelStorage.ParameterValue param = ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId)
            .setXslName(xslName)
            .setValueType(MboParameters.ValueType.STRING)
            .addAllStrValue(LocalizedStringUtils.defaultStrings(values))
            .build();
        return parameterValue(param);
    }

    public ModelBuilder parameterValues(long paramId, String xslName, Long... optionIds) {
        for (long optionId : optionIds) {
            ModelStorage.ParameterValue param = ModelStorage.ParameterValue.newBuilder()
                .setParamId(paramId)
                .setXslName(xslName)
                .setValueType(MboParameters.ValueType.ENUM)
                .setOptionId((int) optionId)
                .build();
            parameterValue(param);
        }
        return this;
    }

    public ModelBuilder parameterValues(long paramId, String xslName, Double... numbers) {
        for (Double number : numbers) {
            ModelStorage.ParameterValue param = ModelStorage.ParameterValue.newBuilder()
                .setParamId(paramId)
                .setXslName(xslName)
                .setValueType(MboParameters.ValueType.NUMERIC)
                .setNumericValue(String.valueOf(number))
                .build();
            parameterValue(param);
        }
        return this;
    }

    public ModelBuilder parameterValues(long paramId, String xslName, Integer... optionIds) {
        Long[] longIds = new Long[optionIds.length];
        for (int i = 0; i < optionIds.length; i++) {
            longIds[i] = optionIds[i].longValue();
        }
        return this.parameterValues(paramId, xslName, longIds);
    }

    public ModelBuilder parameterValue(ModelStorage.ParameterValue parameterValue) {
        model.addParameterValues(parameterValue);
        return this;
    }

    public ModelBuilder published(boolean published) {
        model.setPublished(published);
        return this;
    }

    public ModelBuilder publishedOnBlue(boolean publishedOnBlue) {
        model.setBluePublished(publishedOnBlue);
        return this;
    }

    public ModelBuilder source(String currentType) {
        model.setSourceType(currentType);
        return this;
    }

    public ModelBuilder currentType(String type) {
        model.setCurrentType(type);
        return this;
    }

    public ModelBuilder currentType(ModelStorage.ModelType modelType) {
        return currentType(modelType.name());
    }

    public ModelBuilder experimentFlag(String flag) {
        model.setExperimentFlag(flag);
        return this;
    }

    public ModelBuilder deleted(boolean deleted) {
        model.setDeleted(deleted);
        return this;
    }

    public ModelBuilder modificationDate(Date date) {
        model.setModifiedTs(date.getTime());
        return this;
    }

    public ModelBuilder modifiedUserId(long uid) {
        model.setModifiedUserId(uid);
        return this;
    }

    public ModelBuilder picture(ModelStorage.Picture picture) {
        model.addPictures(picture);
        return this;
    }

    public ModelBuilder picture(ModelStorage.Picture.Builder picture) {
        model.addPictures(picture);
        return this;
    }

    public ModelBuilder picture(String url) {
        return picture(null, url);
    }

    public ModelBuilder picture(String xslName, String url) {
        return picture(xslName, url, PIC_SIZE, PIC_SIZE, null, null);
    }

    public ModelBuilder picture(String url, Integer height, Integer width, String urlSource, String urlOrig) {
        return picture(null, url, height, width, urlSource, urlOrig);
    }

    public ModelBuilder picture(String xslName, String url, Integer height, Integer width,
                                String urlSource, String urlOrig) {
        return picture(xslName, url, height, width, urlSource, urlOrig, null, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public ModelBuilder picture(@Nullable String xslName,
                                @Nonnull String url, @Nonnull Integer height, @Nonnull Integer width,
                                @Nullable String urlSource, @Nullable String urlOrig,
                                @Nullable Double colorness, @Nullable Double colornessAvg) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(height);
        Objects.requireNonNull(width);
        ModelStorage.Picture.Builder picture = ModelStorage.Picture.newBuilder();
        if (xslName != null) {
            picture.setXslName(xslName);
        }
        picture.setUrl(url);
        picture.setHeight(height);
        picture.setWidth(width);
        if (urlSource != null) {
            picture.setUrlSource(urlSource);
        }
        if (urlOrig != null) {
            picture.setUrlOrig(urlOrig);
        }
        if (colorness != null) {
            picture.setColorness(colorness);
        }
        if (colornessAvg != null) {
            picture.setColornessAvg(colornessAvg);
        }
        return picture(picture);
    }

    public ModelBuilder withSkuParentRelation(long parentCategoryId, long parentId) {
        ModelStorage.Relation skuRelation = ModelStorage.Relation.newBuilder()
            .setId(parentId)
            .setCategoryId(parentCategoryId)
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .build();
        this.model.addRelations(skuRelation);
        return this;
    }

    public ModelBuilder withSkuParentRelation(ModelStorage.ModelOrBuilder parentModel) {
        return withSkuParentRelation(parentModel.getCategoryId(), parentModel.getId());
    }

    public ModelBuilder withSkuRelations(long skuCategoryId, long... skuIds) {
        for (long skuId : skuIds) {
            ModelStorage.Relation skuRelation = ModelStorage.Relation.newBuilder()
                .setId(skuId)
                .setCategoryId(skuCategoryId)
                .setType(ModelStorage.RelationType.SKU_MODEL)
                .build();
            this.model.addRelations(skuRelation);
        }
        return this;
    }

    public ModelBuilder createdDate(Date createdDate) {
        this.model.setCreatedDate(createdDate.getTime());
        return this;
    }

    public ModelBuilder modelRelation(long modelId, long categoryId, ModelStorage.RelationType type) {
        this.model.addRelations(ModelStorage.Relation.newBuilder()
            .setId(modelId).setCategoryId(categoryId).setType(type)
            .build());
        return this;
    }

    public ModelBuilder clearRelations() {
        model.clearRelations();
        return this;
    }

    public ModelStorage.Model build() {
        return model.build();
    }
}
