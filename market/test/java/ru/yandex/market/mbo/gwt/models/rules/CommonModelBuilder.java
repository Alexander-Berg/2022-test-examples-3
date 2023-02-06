package ru.yandex.market.mbo.gwt.models.rules;

import com.google.common.collect.Lists;
import org.springframework.util.Assert;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.gwt.models.HasId;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.YangTaskEnum;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.builders.EnumValueAliasBuilder;
import ru.yandex.market.mbo.gwt.models.builders.ModelRelationBuilder;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueMetadata;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;
import ru.yandex.market.mbo.gwt.models.modelstorage.Video;
import ru.yandex.market.mbo.gwt.models.modelstorage.VideoBuilder;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.OptionUtils;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author gilmulla
 */
public class CommonModelBuilder<T> {
    public static final int PIC_SIZE = 300;

    private CommonModel model = new CommonModel();
    private ParameterValue paramValue;
    private Map<Long, ThinCategoryParam> paramsById = new HashMap<>();
    private Map<String, ThinCategoryParam> paramsByXslName = new HashMap<>();
    private final Function<CommonModel, T> endModelCallback;

    CommonModelBuilder(Function<CommonModel, T> endModelCallback) {
        currentType(CommonModel.Source.GURU);
        source(CommonModel.Source.GURU);
        published(true);
        publishedOnBlue(true);
        this.endModelCallback = endModelCallback;
    }

    public static <T> CommonModelBuilder<T> builder(Function<CommonModel, T> endModelCallback) {
        return new CommonModelBuilder<>(endModelCallback);
    }

    public static <T> CommonModelBuilder<T> builder(CommonModelBuilder baseBuilder,
                                                    Function<CommonModel, T> endModelCallback) {
        Collection<ThinCategoryParam> params = baseBuilder != null
            ? baseBuilder.paramsById.values()
            : Collections.emptyList();
        CommonModelBuilder<T> builder = new CommonModelBuilder<>(endModelCallback);
        builder.parameters(params);
        return builder;
    }

    public static CommonModelBuilder<CommonModel> newBuilder() {
        return new CommonModelBuilder<>(Function.identity());
    }

    public static CommonModelBuilder<CommonModel> newBuilder(long id, long categoryId) {
        return newBuilder()
            .id(id)
            .category(categoryId);
    }

    public static CommonModelBuilder<CommonModel> newBuilder(long id, long categoryId, long vendorId) {
        return newBuilder()
            .id(id)
            .category(categoryId)
            .vendorId(vendorId);
    }

    public ThinCategoryParam getParamDescription(Long id) {
        return paramsById.get(id);
    }

    public ThinCategoryParam getParamDescription(String xslName) {
        return paramsByXslName.get(xslName);
    }

    public static <T> CommonModelBuilder<T> model(List<? extends ThinCategoryParam> params) {
        return new CommonModelBuilder<T>(null).parameters(params);
    }

    public CommonModelBuilder<T> startModel() {
        this.model = new CommonModel();
        this.paramValue = null;
        return this;
    }

    public CommonModelBuilder<T> parameters(Collection<? extends ThinCategoryParam> params) {
        for (ThinCategoryParam param : params) {
            paramsById.put(param.getId(), param);
            paramsByXslName.put(param.getXslName(), param);
        }
        return this;
    }

    public CommonModelBuilder<T> parameters(Map<Long, ThinCategoryParam> paramsById,
                                            Map<String, ThinCategoryParam> paramsByName) {
        this.paramsById = paramsById;
        this.paramsByXslName = paramsByName;
        return this;
    }

    public CommonModelBuilder<T> title(String title) {
        ParameterValues parameterValues = new ParameterValues(KnownIds.NAME_PARAM_ID, XslNames.NAME, Param.Type.STRING,
            WordUtil.defaultWord(title));
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.removeAllParameterValues(XslNames.NAME);
        model.putParameterValues(parameterValues);
        return this;
    }

    public CommonModelBuilder<T> quality(Long valueId) {
        ParameterValues parameterValues = new ParameterValues(KnownIds.MODEL_QUALITY_PARAM_ID, XslNames.MODEL_QUALITY,
            Param.Type.ENUM, valueId);
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.removeAllParameterValues(XslNames.MODEL_QUALITY);
        model.putParameterValues(parameterValues);
        return this;
    }

    public CommonModelBuilder<T> id(long id) {
        model.setId(id);
        return this;
    }

    public CommonModelBuilder<T> parentModelId(long parentModelId) {
        this.model.setParentModelId(parentModelId);
        return this;
    }

    public CommonModelBuilder<T> parentModel(CommonModel parentModel) {
        this.model.setParentModelId(parentModel.getId());
        this.model.setParentModel(parentModel);
        return this;
    }

    public CommonModelBuilder<T> setDeleted(boolean deleted) {
        this.model.setDeleted(deleted);
        return this;
    }

    public CommonModelBuilder<T> vendorId(long vendorId) {
        ParameterValues parameterValues = new ParameterValues(KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR,
            Param.Type.ENUM, vendorId);
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.putParameterValues(parameterValues);
        return this;
    }

    public CommonModelBuilder<T> parameterValues(CategoryParam categoryParam, String... strs) {
        return parameterValues(categoryParam.getId(), categoryParam.getXslName(), strs);
    }

    public CommonModelBuilder<T> parameterValues(long paramId, String xslName, String... strs) {
        ParameterValues parameterValues = new ParameterValues(paramId, xslName, Param.Type.STRING,
            Arrays.stream(strs).map(WordUtil::defaultWord).toArray(Word[]::new));
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.putParameterValues(parameterValues);
        return this;
    }

    public CommonModelBuilder<T> parameterValues(long paramId, String xslName,
                                                 ModificationSource modificationSource, String... strs) {
        ParameterValues parameterValues = new ParameterValues(paramId, xslName, Param.Type.STRING,
            Arrays.stream(strs).map(WordUtil::defaultWord).toArray(Word[]::new));
        parameterValues.setModificationSource(modificationSource);
        model.putParameterValues(parameterValues);
        return this;
    }


    public CommonModelBuilder<T> parameterValues(CategoryParam categoryParam, Long... optionIds) {
        return parameterValues(categoryParam.getId(), categoryParam.getXslName(), optionIds);
    }

    public CommonModelBuilder<T> parameterValues(long paramId, String xslName, Long... optionIds) {
        ParameterValues parameterValues = new ParameterValues(paramId, xslName, Param.Type.ENUM, optionIds);
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.putParameterValues(parameterValues);
        return this;
    }

    public CommonModelBuilder<T> parameterValues(long paramId, String xslName, Integer... optionIds) {
        Long[] longIds = new Long[optionIds.length];
        for (int i = 0; i < optionIds.length; i++) {
            longIds[i] = optionIds[i].longValue();
        }
        return this.parameterValues(paramId, xslName, longIds);
    }

    public CommonModelBuilder<T> parameterValues(long paramId, String xslName, BigDecimal... numbers) {
        ParameterValues parameterValues = new ParameterValues(paramId, xslName, Param.Type.NUMERIC, numbers);
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.putParameterValues(parameterValues);
        return this;
    }

    public CommonModelBuilder<T> parameterValues(long paramId, String xslName, Boolean bool) {
        ParameterValues parameterValues = new ParameterValues(paramId, xslName, Param.Type.BOOLEAN, bool, 1L);
        parameterValues.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.putParameterValues(parameterValues);
        return this;
    }

    public CommonModelBuilder<T> parameterValueHypothesis(ThinCategoryParam param, String... values) {
        return parameterValueHypothesis(param.getId(), param.getXslName(), param.getType(), values);
    }

    public CommonModelBuilder<T> parameterValueHypothesis(long paramId, String xslName, Param.Type type,
                                                          String... values) {
        ParameterValueHypothesis hypothesis = new ParameterValueHypothesis();
        hypothesis.setParamId(paramId);
        hypothesis.setXslName(xslName);
        hypothesis.setType(type);
        hypothesis.setStringValue(Arrays.stream(values).map(WordUtil::defaultWord).collect(Collectors.toList()));
        model.putParameterValueHypothesis(hypothesis);
        return this;
    }

    public CommonModelBuilder<T> putParameterValue(ParameterValue parameterValue) {
        ParameterValues values = this.model.getParameterValues(parameterValue.getParamId());
        if (values == null) {
            values = ParameterValues.of(parameterValue);
        } else {
            values.addValue(parameterValue);
        }
        return putParameterValues(values);
    }

    public CommonModelBuilder<T> putParameterValues(List<ParameterValue> parameterValues) {
        parameterValues.forEach(this::putParameterValue);
        return this;
    }

    public CommonModelBuilder<T> putParameterValues(ParameterValues parameterValues) {
        model.putParameterValues(parameterValues);
        return this;
    }

    public ParameterValueBuilder<T> startParameterValueLink() {
        return ParameterValueBuilder.newBuilder(this, parameterValue -> {
            this.model.addParameterValueLink(parameterValue);
        });
    }

    public ParameterValueBuilder<T> startParameterValue() {
        return ParameterValueBuilder.newBuilder(this, this::putParameterValue);
    }

    public EnumValueAliasBuilder<T> enumAlias(long paramId) {
        EnumValueAliasBuilder<T> builder = EnumValueAliasBuilder.newBuilder(this);
        builder.paramId(paramId);
        return builder;
    }

    public CommonModelBuilder<T> published(boolean published) {
        model.setPublished(published);
        return this;
    }

    public CommonModelBuilder<T> publishedOnBlue(boolean publishedOnBlue) {
        model.setBluePublished(publishedOnBlue);
        return this;
    }

    public CommonModelBuilder<T> source(CommonModel.Source currentType) {
        model.setSource(currentType);
        return this;
    }

    public CommonModelBuilder<T> sourceYangTask(YangTaskEnum sourceYangTask) {
        model.setSourceYangTask(sourceYangTask);
        return this;
    }

    public CommonModelBuilder<T> creationSource(AuditAction.Source creationSource) {
        model.setCreationSource(creationSource);
        return this;
    }

    public CommonModelBuilder<T> sourceId(String sourceId) {
        model.setSourceId(sourceId);
        return this;
    }

    public CommonModelBuilder<T> currentType(CommonModel.Source type) {
        model.setCurrentType(type);
        return this;
    }

    public CommonModelBuilder<T> supplierId(Long supplierId) {
        model.setSupplierId(supplierId);
        return this;
    }

    public CommonModelBuilder<T> supplierId(long supplierId) {
        model.setSupplierId(supplierId);
        return this;
    }

    public CommonModelBuilder<T> experimentFlag(String flag) {
        model.setExperimentFlag(flag);
        return this;
    }


    public CommonModelBuilder<T> category(long category) {
        model.setCategoryId(category);
        return this;
    }

    public CommonModelBuilder<T> deleted(boolean deleted) {
        model.setDeleted(deleted);
        return this;
    }

    public CommonModelBuilder<T> modificationDate(Date date) {
        model.setModificationDate(date);
        return this;
    }

    public CommonModelBuilder<T> modifiedUserId(long uid) {
        model.setModifiedUserId(uid);
        return this;
    }

    public CommonModelBuilder<T> withIsSkuParam(boolean value) {
        return this.putParameterValues(new ParameterValues(1L, XslNames.IS_SKU, Param.Type.BOOLEAN, value, 1L));
    }

    public CommonModelBuilder<T> param(long paramId) {
        createParamValue(paramsById.get(paramId));
        paramValue.setParamId(paramId);
        return this;
    }

    public CommonModelBuilder<T> param(String xslName) {
        createParamValue(paramsByXslName.get(xslName));
        paramValue.setXslName(xslName);
        return this;
    }

    public CommonModelBuilder<T> param(CategoryParam param) {
        if (paramsById.containsKey(param.getId())) {
            return param(param.getId());
        } else if (paramsByXslName.containsKey(param.getXslName())) {
            return param(param.getXslName());
        }

        paramsById.put(param.getId(), param);
        paramsByXslName.put(param.getXslName(), param);
        createParamValue(param);
        paramValue.setXslName(param.getXslName());
        return this;
    }

    public CommonModelBuilder<T> pictureParam(String xslName, String url, Integer height, Integer width,
                                              String urlSource, String urlOrig) {
        return pictureParam(xslName, url, height, width, urlSource, urlOrig, null, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> pictureParam(String xslName, String url, Integer height, Integer width,
                                              String urlSource, String urlOrig, Double colorness, Double colornessAvg) {
        ImageType type = ImageType.getImageType(xslName);
        if (type == ImageType.UNKNOWN) {
            throw new IllegalArgumentException("xslName is not image type: " + xslName);
        }

        if (url != null) {
            Word value = WordUtil.defaultWord(url);
            model.putParameterValues(new ParameterValues(getParamId(xslName), xslName, Param.Type.STRING, value));
        }
        if (height != null) {
            String heightXslName = type.getHeightParamName(xslName);
            BigDecimal value = new BigDecimal(height);
            model.putParameterValues(new ParameterValues(getParamId(heightXslName), heightXslName, Param.Type.NUMERIC,
                value));
        }
        if (width != null) {
            String widthXslName = type.getWidthParamName(xslName);
            BigDecimal value = new BigDecimal(width);
            model.putParameterValues(new ParameterValues(getParamId(widthXslName), widthXslName, Param.Type.NUMERIC,
                value));
        }
        if (urlSource != null) {
            String sourceXslName = type.getUrlParamName(xslName);
            Word value = WordUtil.defaultWord(urlSource);
            model.putParameterValues(new ParameterValues(getParamId(sourceXslName), sourceXslName, Param.Type.STRING,
                value));
        }
        if (urlOrig != null) {
            String origXslName = type.getRawUrlParamName(xslName);
            Word value = WordUtil.defaultWord(urlOrig);
            model.putParameterValues(new ParameterValues(getParamId(origXslName), origXslName, Param.Type.STRING,
                value));
        }
        if (colorness != null) {
            String colornessXslName = type.getColornessParamName(xslName);
            model.putParameterValues(new ParameterValues(getParamId(colornessXslName), colornessXslName,
                Param.Type.NUMERIC, new BigDecimal(colorness)));
        }
        if (colornessAvg != null) {
            String colornessAvgParamName = type.getColornessAvgParamName(xslName);
            model.putParameterValues(new ParameterValues(getParamId(colornessAvgParamName), colornessAvgParamName,
                Param.Type.NUMERIC, new BigDecimal(colornessAvg)));
        }
        return this;
    }

    public CommonModelBuilder<T> pictureParam(Picture picture) {
        return pictureParam(picture.getXslName(), picture.getUrl(), picture.getHeight(), picture.getWidth(),
            picture.getUrlSource(), picture.getUrlOrig());
    }

    private long getParamId(String xslName) {
        return Optional.ofNullable(paramsByXslName.get(xslName))
            .map(HasId::getId)
            .orElse((long) xslName.hashCode());
    }


    public CommonModelBuilder<T> picture(PictureBuilder pictureBuilder) {
        return picture(pictureBuilder.build());
    }

    public CommonModelBuilder<T> picture(Picture picture) {
        model.addPicture(picture);
        return this;
    }

    public CommonModelBuilder<T> picture(String url) {
        return picture(null, url);
    }

    public CommonModelBuilder<T> picture(String xslName, String url) {
        return picture(xslName, url, PIC_SIZE, PIC_SIZE, null, url, ModificationSource.OPERATOR_FILLED);
    }

    public CommonModelBuilder<T> picture(String xslName, String url, ModificationSource source) {
        return picture(xslName, url, PIC_SIZE, PIC_SIZE, null, url, source);
    }

    public CommonModelBuilder<T> picture(String url, Integer height, Integer width, String urlSource, String urlOrig) {
        return picture(null, url, height, width, urlSource, urlOrig, ModificationSource.OPERATOR_FILLED);
    }

    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig) {
        return picture(xslName, url, height, width, urlSource, urlOrig, ModificationSource.OPERATOR_FILLED);
    }

    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, ModificationSource source) {
        return picture(xslName, url, height, width, urlSource, urlOrig, source, null, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, ModificationSource source,
                                         Double colorness, Double colornessAvg) {
        return picture(xslName, url, height, width, urlSource, urlOrig, source, colorness, colornessAvg, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> picture(String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, Long lastModificationUid,
                                         ModificationSource source,
                                         Double colorness, Double colornessAvg, String origMd5,
                                         Boolean isWhiteBackground) {
        return picture(null, url, height, width, urlSource, urlOrig,
            lastModificationUid, source, colorness, colornessAvg, origMd5, isWhiteBackground);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, ModificationSource source,
                                         Double colorness, Double colornessAvg, String origMd5) {
        return picture(xslName, url, height, width, urlSource, urlOrig, null, source, colorness, colornessAvg, origMd5);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, Long lastModificationUid,
                                         ModificationSource source,
                                         Double colorness, Double colornessAvg, String origMd5) {
        return picture(xslName, url, height, width, urlSource, urlOrig,
            null, source, colorness, colornessAvg, origMd5, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, Long lastModificationUid,
                                         ModificationSource source,
                                         Double colorness, Double colornessAvg, String origMd5,
                                         Boolean isWhiteBackground) {
        return picture(xslName, url, height, width, urlSource, urlOrig,
            lastModificationUid, source, colorness, colornessAvg, origMd5, isWhiteBackground, null, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, Long lastModificationUid,
                                         ModificationSource source,
                                         Double colorness, Double colornessAvg, String origMd5,
                                         Boolean isWhiteBackground, Long ownerId) {
        return picture(xslName, url, height, width, urlSource, urlOrig,
            lastModificationUid, source, colorness, colornessAvg, origMd5, isWhiteBackground, ownerId, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommonModelBuilder<T> picture(String xslName, String url, Integer height, Integer width,
                                         String urlSource, String urlOrig, Long lastModificationUid,
                                         ModificationSource source,
                                         Double colorness, Double colornessAvg, String origMd5,
                                         Boolean isWhiteBackground, Long ownerId, Picture.PictureStatus status) {
        Picture picture = new Picture();
        picture.setXslName(xslName);
        picture.setUrl(url);
        picture.setHeight(height);
        picture.setWidth(width);
        picture.setUrlSource(urlSource);
        picture.setUrlOrig(urlOrig);
        picture.setLastModificationUid(lastModificationUid);
        picture.setModificationSource(source);
        picture.setColorness(colorness);
        picture.setColornessAvg(colornessAvg);
        picture.setOrigMd5(origMd5);
        picture.setIsWhiteBackground(isWhiteBackground);
        picture.setOwnerId(ownerId);
        picture.setPictureStatus(status);
        model.addPicture(picture);
        return this;
    }

    public CommonModelBuilder<T> valueAlias(Long paramId, Long optionId, Long aliasId) {

        EnumValueAlias alias = new EnumValueAlias();
        alias.setParamId(paramId);
        alias.setOptionId(optionId);
        alias.setAliasOptionId(aliasId);
        alias.setXslName(paramsById.get(paramId).getXslName());
        alias.setModificationSource(ModificationSource.OPERATOR_FILLED);
        model.addEnumValueAlias(alias);
        return this;
    }

    public CommonModelBuilder<T> parameterMetadata(Long paramId, ModificationSource metadataValue) {

        ParameterValueMetadata metadata = new ParameterValueMetadata(paramId, metadataValue);
        model.getParameterValuesMetadata().put(paramId, metadata);
        return this;
    }

    public CommonModelBuilder<T> setEmpty() {
        return this;
    }

    public CommonModelBuilder<T> setOption(long optionId) {
        paramValue.setOptionId(optionId);
        return this;
    }

    public CommonModelBuilder<T> setBoolean(boolean bool) {
        ThinCategoryParam param = paramsById.get(paramValue.getParamId());
        Assert.notNull(param);
        Option opt = OptionUtils.findOption(
            param, Boolean.valueOf(bool).toString(), false);
        paramValue.setOptionId(opt.getValueId());
        paramValue.setBooleanValue(bool);
        return this;
    }

    public CommonModelBuilder<T> setNumeric(int numericValue) {
        return setNumeric(new BigDecimal(numericValue));
    }

    public CommonModelBuilder<T> setNumeric(double numericValue) {
        return setNumeric(new BigDecimal(numericValue));
    }

    public CommonModelBuilder<T> setNumeric(BigDecimal numericValue) {
        paramValue.setNumericValue(numericValue);
        return this;
    }

    public CommonModelBuilder<T> setString(String... stringValue) {
        List<Word> words = new ArrayList<>(
            WordUtil.defaultWords(Arrays.asList(stringValue)));
        paramValue.setStringValue(words);
        return this;
    }

    public CommonModelBuilder<T> setString(List<String> stringValue) {
        List<Word> words = WordUtil.defaultWords(stringValue);
        paramValue.setStringValue(words);
        return this;
    }

    public CommonModelBuilder<T> setWords(Word... words) {
        paramValue.setStringValue(Lists.newArrayList(words));
        return this;
    }

    public CommonModelBuilder<T> lastModificationUid(long uuid) {
        paramValue.setLastModificationUid(uuid);
        return this;
    }

    public CommonModelBuilder<T> lastModificationDate(Date date) {
        paramValue.setLastModificationDate(date);
        return this;
    }

    public CommonModelBuilder<T> modificationSource(ModificationSource source) {
        paramValue.setModificationSource(source);
        return this;
    }

    public CommonModelBuilder<T> withSkuParentRelation(long parentCategoryId, long parentId) {
        ModelRelation skuRelation = ModelRelationBuilder.newBuilder()
            .id(parentId)
            .categoryId(parentCategoryId)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .build();
        this.model.addRelation(skuRelation);
        return this;
    }

    public CommonModelBuilder<T> withSkuParentRelation(CommonModel parentModel) {
        return withSkuParentRelation(parentModel.getCategoryId(), parentModel.getId());
    }

    public CommonModelBuilder<T> withSkuParentRelation(ModelStorage.ModelOrBuilder parentModel) {
        return withSkuParentRelation(parentModel.getCategoryId(), parentModel.getId());
    }

    public CommonModelBuilder<T> withSkuRelations(long skuCategoryId, long... skuIds) {
        for (long skuId : skuIds) {
            ModelRelation skuRelation = ModelRelationBuilder.newBuilder()
                .id(skuId)
                .categoryId(skuCategoryId)
                .type(ModelRelation.RelationType.SKU_MODEL)
                .build();
            this.model.addRelation(skuRelation);
        }
        return this;
    }

    public CommonModelBuilder<T> withSkuRelations(Collection<CommonModel> skus) {
        for (CommonModel sku : skus) {
            ModelRelation skuRelation = ModelRelationBuilder.newBuilder()
                .id(sku.getId())
                .categoryId(sku.getCategoryId())
                .type(ModelRelation.RelationType.SKU_MODEL)
                .build();
            this.model.addRelation(skuRelation);
        }
        return this;
    }

    public CommonModelBuilder<T> withSkuRelations(CommonModel... skus) {
        return withSkuRelations(Arrays.asList(skus));
    }

    public CommonModelBuilder<T> withSkuRelations(ModelStorage.ModelOrBuilder... skus) {
        for (ModelStorage.ModelOrBuilder sku : skus) {
            ModelRelation skuRelation = ModelRelationBuilder.newBuilder()
                .id(sku.getId())
                .categoryId(sku.getCategoryId())
                .type(ModelRelation.RelationType.SKU_MODEL)
                .build();
            this.model.addRelation(skuRelation);
        }
        return this;
    }

    public CommonModelBuilder<T> createdDate(Date createdDate) {
        this.model.setCreatedDate(createdDate);
        return this;
    }

    public CommonModelBuilder<CommonModelBuilder<T>> startParentModel() {
        return builder(parentModel -> {
            this.parentModel(parentModel);
            return CommonModelBuilder.this;
        }).parameters(this.paramsById.values());
    }

    public ModelRelationBuilder<T> startModelRelation() {
        return ModelRelationBuilder.newBuilder(this);
    }

    public CommonModelBuilder<T> modelRelation(long modelId, long categoryId, ModelRelation.RelationType type) {
        return ModelRelationBuilder.newBuilder(this)
            .id(modelId).categoryId(categoryId).type(type)
            .endModelRelation();
    }

    public CommonModelBuilder<T> clearRelations() {
        model.clearRelations();
        return this;
    }

    public CommonModelBuilder<T> computeIf(boolean condition,
                                           Function<CommonModelBuilder<T>, CommonModelBuilder<T>> func) {
        if (condition) {
            return func.apply(this);
        } else {
            return this;
        }
    }

    public CommonModel getModel() {
        endModel();
        return this.model;
    }

    public ModelStorage.Model getRawModel() {
        endModel();
        return ModelProtoConverter.convert(this.model);
    }

    public ModelStorage.Model.Builder getRawModelBuilder() {
        endModel();
        return ModelProtoConverter.convert(this.model).toBuilder();
    }

    public T endModel() {
        if (paramValue != null) {
            this.model.addParameterValue(paramValue);
            this.paramValue = null;
        }
        if (this.endModelCallback == null) {
            return null;
        }
        return this.endModelCallback.apply(new CommonModel(model));
    }

    private void createParamValue(ThinCategoryParam param) {
        if (paramValue != null) {
            this.model.addParameterValue(paramValue);
        }
        paramValue = doCreateParamValue(param);
    }

    private ParameterValue doCreateParamValue(ThinCategoryParam param) {
        ParameterValue value = new ParameterValue();
        value.setParamId(param.getId());
        value.setXslName(param.getXslName());
        value.setType(param.getType());
        return value;
    }

    public CommonModelBuilder<T> video(VideoBuilder videoBuilder) {
        return video(videoBuilder.build());
    }

    public CommonModelBuilder<T> video(Video video) {
        model.addVideo(video);
        return this;
    }

    public CommonModelBuilder<T> video(String url,
                                       ModificationSource modificationSource,
                                       Long lastModificationUid,
                                       Date lastModificationDate,
                                       String urlSource,
                                       ModelStorage.VideoSource videoSource) {
        Video video = new Video();
        video.setUrl(url);
        video.setModificationSource(modificationSource);
        video.setLastModificationUid(lastModificationUid);
        video.setLastModificationDate(lastModificationDate);
        video.setUrlSource(urlSource);
        video.setVideoSource(Video.VideoSource.valueOf(videoSource.name()));
        model.addVideo(video);
        return this;
    }

    public CommonModelBuilder<T> broken(boolean broken) {
        model.setBroken(broken);
        return this;
    }

    public CommonModelBuilder<T> strictChecksRequired(boolean strictChecksRequired) {
        model.setStrictChecksRequired(strictChecksRequired);
        return this;
    }
}
