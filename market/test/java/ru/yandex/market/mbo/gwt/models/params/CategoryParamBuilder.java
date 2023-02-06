package ru.yandex.market.mbo.gwt.models.params;

import ru.yandex.market.mbo.gwt.models.gurulight.GLTaskGenInfo;
import ru.yandex.market.mbo.gwt.models.gurulight.Tag;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class CategoryParamBuilder {
    private long id;
    private Param.Type type;
    private String xslName;
    private Map<Integer, Word> localizedNames = new LinkedHashMap<Integer, Word>();
    private List<Word> localizedAliases = new ArrayList<Word>();
    private List<Option> options = new ArrayList<Option>();
    private Long measureId;
    private Long unitId;
    private Unit unit;
    private String maxValueStr;
    private String minValueStr;
    private BigDecimal maxValue;
    private BigDecimal minValue;
    private Integer precision = Param.DEFAULT_PRECISION;
    private Boolean useForClassifier = false;
    private Boolean useForMatcher = false;
    private Boolean useForGurulight = false;
    private Boolean useForGuru = false;
    private Boolean useForImages = false;
    private Tag joinTag;
    private Tag formalizerTag;
    private ParamOptionsAccessType access = ParamOptionsAccessType.SUPER;
    private Boolean published = false;
    private String description;
    private String comment;
    private GLTaskGenInfo taskGenInfo = new GLTaskGenInfo();
    private Boolean hasBoolNo = false;
    private SubType subtype = SubType.NOT_DEFINED;
    private Boolean clusterBreaker = false;
    private Long mainSizeMeasure;
    private List<Long> optionalSizeMeasures;
    private FormalizationScope formalizationScope = FormalizationScope.getDefault();
    private Boolean doNotFormalizePatterns = false;
    private Boolean onlyForClusterCard = false;
    private Boolean dontUseAsAlias = false;
    private Boolean through = false;
    private Boolean clusterFilter = false;
    private CategoryParam.Level level = CategoryParam.Level.MODEL;
    private GuruType guruType = GuruType.TEXT;
    private long categoryHid;
    private String categoryName;
    private long hyperId;
    private long mboDumpId;
    private Boolean otherAllowed = false;
    private Boolean readOnly = false;
    private ModifiedState modified = ModifiedState.NOT_SPECIFIED;
    private Boolean multifield = false;
    private Boolean necessary = false;
    private Boolean omitOnCopy = false;
    private Boolean ordered = false;
    private Integer publishLevel = 0;
    private Integer templateIndex = 0;
    private Boolean viewTypeCreation = false;
    private Boolean viewTypeForm = false;
    private Boolean viewTypeTable = false;
    private String mapping;
    private String commentForOperator;
    private Boolean bindingParam = false;
    private Boolean hidden = false;
    private Boolean creationParam = false;
    private Boolean useInFilter = false;
    private Boolean noAdvFilters = false;
    private Boolean managedExternally = false;
    private String defaultValue;
    private BigDecimal importance;
    private Integer advFilterIndex = -1;
    private Integer commonFilterIndex = -1;
    private Integer incutFilterIndex = -1;
    private Boolean highlightOriginalValue = false;
    private String sortMethod;
    private BigDecimal fillDifficulty;
    private Boolean fillValueFromOffers = false;
    private ParamMatchingType matchingType;
    private Integer outputIndex;
    private Long weight = 0L;
    private Boolean superParameter = false;
    private Boolean mandatory = false;
    private Integer optionsCount = 0;
    private Boolean allowInTitleMaker = false;
    private Timestamp timestamp;
    private Integer shortEnumCount;
    private Param.EnumSortType shortEnumSortType;
    private Boolean isService = false;
    private Boolean isShowOnSkuTab = false;
    private Boolean blueGrouping = false;
    private Boolean mdmParameter = false;
    private SkuParameterMode skuParameterMode;
    private ConfidentFormalization confidentFormalization = ConfidentFormalization.getDefault();
    private List<String> tags = new ArrayList<>();

    private CategoryParamBuilder() {
    }

    public static CategoryParamBuilder newBuilder() {
        return new CategoryParamBuilder();
    }

    public static CategoryParamBuilder newBuilder(long id, String xslNameAndName, long categoryId) {
        return newBuilder(id, xslNameAndName)
            .setCategoryHid(categoryId);
    }

    public static CategoryParamBuilder newBuilder(long id, String xslNameAndName) {
        return newBuilder()
            .setId(id)
            .setXslName(xslNameAndName)
            .setName(xslNameAndName);
    }

    public static CategoryParamBuilder newBuilder(long id, String xslNameAndName, Param.Type type) {
        return newBuilder(id, xslNameAndName)
            .setType(type);
    }

    public CategoryParamBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public CategoryParamBuilder setName(String name) {
        Map<Integer, Word> map = WordUtil.defaultWords(name).stream()
            .collect(Collectors.toMap(Word::getLangId, Function.identity()));
        return setLocalizedNames(map);
    }

    public CategoryParamBuilder setType(Param.Type type) {
        this.type = type;
        return this;
    }

    public CategoryParamBuilder setXslName(String xslName) {
        this.xslName = xslName;
        return this;
    }

    public CategoryParamBuilder setLocalizedNames(Map<Integer, Word> localizedNames) {
        this.localizedNames = localizedNames;
        return this;
    }

    public CategoryParamBuilder setLevel(CategoryParam.Level level) {
        this.level = level;
        return this;
    }

    public CategoryParamBuilder setGuruType(GuruType guruType) {
        this.guruType = guruType;
        return this;
    }

    public CategoryParamBuilder setLocalizedAliases(List<Word> localizedAliases) {
        this.localizedAliases = localizedAliases;
        return this;
    }

    public CategoryParamBuilder setCategoryHid(long categoryHid) {
        this.categoryHid = categoryHid;
        return this;
    }

    public CategoryParamBuilder addOption(OptionBuilder optionBuilder) {
        return addOption(optionBuilder.build());
    }

    public CategoryParamBuilder addOption(Option option) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(option);
        return this;
    }

    public CategoryParamBuilder setOptions(List<Option> options) {
        this.options = options;
        return this;
    }

    public CategoryParamBuilder setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    public CategoryParamBuilder setMeasureId(Long measureId) {
        this.measureId = measureId;
        return this;
    }

    public CategoryParamBuilder setHyperId(long hyperId) {
        this.hyperId = hyperId;
        return this;
    }

    public CategoryParamBuilder setUnitId(Long unitId) {
        this.unitId = unitId;
        return this;
    }

    public CategoryParamBuilder setMboDumpId(long mboDumpId) {
        this.mboDumpId = mboDumpId;
        return this;
    }

    public CategoryParamBuilder setOtherAllowed(Boolean otherAllowed) {
        this.otherAllowed = otherAllowed;
        return this;
    }

    public CategoryParamBuilder setUnit(Unit unit) {
        this.unit = unit;
        return this;
    }

    public CategoryParamBuilder setMaxValueStr(String maxValueStr) {
        this.maxValueStr = maxValueStr;
        return this;
    }

    public CategoryParamBuilder setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public CategoryParamBuilder setMinValueStr(String minValueStr) {
        this.minValueStr = minValueStr;
        return this;
    }

    public CategoryParamBuilder setModified(ModifiedState modified) {
        this.modified = modified;
        return this;
    }

    public CategoryParamBuilder setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public CategoryParamBuilder setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
        return this;
    }

    public CategoryParamBuilder setMultifield(Boolean multifield) {
        this.multifield = multifield;
        return this;
    }

    public CategoryParamBuilder setPrecision(Integer precision) {
        this.precision = precision;
        return this;
    }

    public CategoryParamBuilder setNecessary(Boolean necessary) {
        this.necessary = necessary;
        return this;
    }

    public CategoryParamBuilder setUseForClassifier(Boolean useForClassifier) {
        this.useForClassifier = useForClassifier;
        return this;
    }

    public CategoryParamBuilder setOmitOnCopy(Boolean omitOnCopy) {
        this.omitOnCopy = omitOnCopy;
        return this;
    }

    public CategoryParamBuilder setUseForMatcher(Boolean useForMatcher) {
        this.useForMatcher = useForMatcher;
        return this;
    }

    public CategoryParamBuilder setOrdered(Boolean ordered) {
        this.ordered = ordered;
        return this;
    }

    public CategoryParamBuilder setPublishLevel(Integer publishLevel) {
        this.publishLevel = publishLevel;
        return this;
    }

    public CategoryParamBuilder setUseForGurulight(Boolean useForGurulight) {
        this.useForGurulight = useForGurulight;
        return this;
    }

    public CategoryParamBuilder setTemplateIndex(Integer templateIndex) {
        this.templateIndex = templateIndex;
        return this;
    }

    public CategoryParamBuilder setUseForGuru(Boolean useForGuru) {
        this.useForGuru = useForGuru;
        return this;
    }

    public CategoryParamBuilder setUseForImages(Boolean useForImages) {
        this.useForImages = useForImages;
        return this;
    }

    public CategoryParamBuilder setViewTypeCreation(Boolean viewTypeCreation) {
        this.viewTypeCreation = viewTypeCreation;
        return this;
    }

    public CategoryParamBuilder setViewTypeForm(Boolean viewTypeForm) {
        this.viewTypeForm = viewTypeForm;
        return this;
    }

    public CategoryParamBuilder setViewTypeTable(Boolean viewTypeTable) {
        this.viewTypeTable = viewTypeTable;
        return this;
    }

    public CategoryParamBuilder setMapping(String mapping) {
        this.mapping = mapping;
        return this;
    }

    public CategoryParamBuilder setCommentForOperator(String commentForOperator) {
        this.commentForOperator = commentForOperator;
        return this;
    }

    public CategoryParamBuilder setBindingParam(Boolean bindingParam) {
        this.bindingParam = bindingParam;
        return this;
    }

    public CategoryParamBuilder setHidden(Boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public CategoryParamBuilder setCreationParam(Boolean creationParam) {
        this.creationParam = creationParam;
        return this;
    }

    public CategoryParamBuilder setUseInFilter(Boolean useInFilter) {
        this.useInFilter = useInFilter;
        return this;
    }

    public CategoryParamBuilder setNoAdvFilters(Boolean noAdvFilters) {
        this.noAdvFilters = noAdvFilters;
        return this;
    }

    public CategoryParamBuilder setManagedExternally(Boolean managedExternally) {
        this.managedExternally = managedExternally;
        return this;
    }

    public CategoryParamBuilder setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public CategoryParamBuilder setImportance(BigDecimal importance) {
        this.importance = importance;
        return this;
    }

    public CategoryParamBuilder setAdvFilterIndex(Integer advFilterIndex) {
        this.advFilterIndex = advFilterIndex;
        return this;
    }

    public CategoryParamBuilder setCommonFilterIndex(Integer commonFilterIndex) {
        this.commonFilterIndex = commonFilterIndex;
        return this;
    }

    public CategoryParamBuilder setIncutFilterIndex(Integer incutFilterIndex) {
        this.incutFilterIndex = incutFilterIndex;
        return this;
    }

    public CategoryParamBuilder setHighlightOriginalValue(Boolean highlightOriginalValue) {
        this.highlightOriginalValue = highlightOriginalValue;
        return this;
    }

    public CategoryParamBuilder setSortMethod(String sortMethod) {
        this.sortMethod = sortMethod;
        return this;
    }

    public CategoryParamBuilder setFillDifficulty(BigDecimal fillDifficulty) {
        this.fillDifficulty = fillDifficulty;
        return this;
    }

    public CategoryParamBuilder setFillValueFromOffers(Boolean fillValueFromOffers) {
        this.fillValueFromOffers = fillValueFromOffers;
        return this;
    }

    public CategoryParamBuilder setMatchingType(ParamMatchingType matchingType) {
        this.matchingType = matchingType;
        return this;
    }

//    public CategoryParamBuilder setParamOptionOverrides(List<ParamOptionOverride> paramOptionOverrides) {
//        this.paramOptionOverrides = paramOptionOverrides;
//        return this;
//    }

    public CategoryParamBuilder setJoinTag(Tag joinTag) {
        this.joinTag = joinTag;
        return this;
    }

    public CategoryParamBuilder setFormalizerTag(Tag formalizerTag) {
        this.formalizerTag = formalizerTag;
        return this;
    }

    public CategoryParamBuilder setAccess(ParamOptionsAccessType access) {
        this.access = access;
        return this;
    }

    public CategoryParamBuilder setPublished(Boolean published) {
        this.published = published;
        return this;
    }

    public CategoryParamBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public CategoryParamBuilder setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public CategoryParamBuilder setTaskGenInfo(GLTaskGenInfo taskGenInfo) {
        this.taskGenInfo = taskGenInfo;
        return this;
    }

    public CategoryParamBuilder setHasBoolNo(Boolean hasBoolNo) {
        this.hasBoolNo = hasBoolNo;
        return this;
    }

    public CategoryParamBuilder setSubtype(SubType subtype) {
        this.subtype = subtype;
        return this;
    }

    public CategoryParamBuilder setClusterBreaker(Boolean clusterBreaker) {
        this.clusterBreaker = clusterBreaker;
        return this;
    }

    public CategoryParamBuilder setMainSizeMeasure(Long mainSizeMeasure) {
        this.mainSizeMeasure = mainSizeMeasure;
        return this;
    }

    public CategoryParamBuilder setOptionalSizeMeasures(List<Long> optionalSizeMeasures) {
        this.optionalSizeMeasures = optionalSizeMeasures;
        return this;
    }

    public CategoryParamBuilder setDoNotFormalizePatterns(Boolean doNotFormalizePatterns) {
        this.doNotFormalizePatterns = doNotFormalizePatterns;
        return this;
    }

    public CategoryParamBuilder setOnlyForClusterCard(Boolean onlyForClusterCard) {
        this.onlyForClusterCard = onlyForClusterCard;
        return this;
    }

    public CategoryParamBuilder setDontUseAsAlias(Boolean dontUseAsAlias) {
        this.dontUseAsAlias = dontUseAsAlias;
        return this;
    }

    public CategoryParamBuilder setOutputIndex(Integer outputIndex) {
        this.outputIndex = outputIndex;
        return this;
    }

    public CategoryParamBuilder setThrough(Boolean through) {
        this.through = through;
        return this;
    }

    public CategoryParamBuilder setClusterFilter(Boolean clusterFilter) {
        this.clusterFilter = clusterFilter;
        return this;
    }

    public CategoryParamBuilder setWeight(Long weight) {
        this.weight = weight;
        return this;
    }

    public CategoryParamBuilder setSuperParameter(Boolean superParameter) {
        this.superParameter = superParameter;
        return this;
    }

    public CategoryParamBuilder setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public CategoryParamBuilder setOptionsCount(Integer optionsCount) {
        this.optionsCount = optionsCount;
        return this;
    }

    public CategoryParamBuilder setAllowInTitleMaker(Boolean allowInTitleMaker) {
        this.allowInTitleMaker = allowInTitleMaker;
        return this;
    }

    public CategoryParamBuilder setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public CategoryParamBuilder setShortEnumCount(Integer shortEnumCount) {
        this.shortEnumCount = shortEnumCount;
        return this;
    }

    public CategoryParamBuilder setShortEnumSortType(Param.EnumSortType shortEnumSortType) {
        this.shortEnumSortType = shortEnumSortType;
        return this;
    }

    public CategoryParamBuilder setService(Boolean service) {
        this.isService = service;
        return this;
    }

    public CategoryParamBuilder setShowOnSkuTab(Boolean show) {
        this.isShowOnSkuTab = show;
        return this;
    }


    public CategoryParamBuilder setBlueGrouping(Boolean show) {
        this.blueGrouping = show;
        return this;
    }

    public CategoryParamBuilder setMdmParameter(Boolean show) {
        this.mdmParameter = show;
        return this;
    }

    public CategoryParamBuilder setSkuParameterMode(SkuParameterMode mode) {
        this.skuParameterMode = mode;
        return this;
    }

    public Parameter build() {
        Parameter parameter = new Parameter();
        parameter.setId(id);
        parameter.setType(type);
        parameter.setXslName(xslName);
        parameter.setLocalizedNames(localizedNames);
        parameter.setLevel(level);
        parameter.setGuruType(guruType);
        parameter.setLocalizedAliases(localizedAliases);
        parameter.setCategoryHid(categoryHid);
        parameter.setOptions(options);
        parameter.setCategoryName(categoryName);
        parameter.setMeasureId(measureId);
        parameter.setHyperId(hyperId);
        parameter.setUnitId(unitId);
        parameter.setMboDumpId(mboDumpId);
        parameter.setOtherAllowed(otherAllowed);
        parameter.setService(isService);
        parameter.setUnit(unit);
        parameter.setMaxValueStr(maxValueStr);
        parameter.setReadOnly(readOnly);
        parameter.setMinValueStr(minValueStr);
        parameter.setModified(modified);
        parameter.setMaxValue(maxValue);
        parameter.setMinValue(minValue);
        parameter.setMultifield(multifield);
        parameter.setPrecision(precision);
        parameter.setNecessary(necessary);
        parameter.setUseForClassifier(useForClassifier);
        parameter.setOmitOnCopy(omitOnCopy);
        parameter.setUseForMatcher(useForMatcher);
        parameter.setOrdered(ordered);
        parameter.setPublishLevel(publishLevel);
        parameter.setUseForGurulight(useForGurulight);
        parameter.setTemplateIndex(templateIndex);
        parameter.setUseForGuru(useForGuru);
        parameter.setViewTypeCreation(viewTypeCreation);
        parameter.setViewTypeForm(viewTypeForm);
        parameter.setViewTypeTable(viewTypeTable);
        parameter.setMapping(mapping);
        parameter.setCommentForOperator(commentForOperator);
        parameter.setMandatoryForSignature(bindingParam);
        parameter.setHidden(hidden);
        parameter.setCreationParam(creationParam);
        parameter.setUseInFilter(useInFilter);
        parameter.setNoAdvFilters(noAdvFilters);
        parameter.setManagedExternally(managedExternally);
        parameter.setDefaultValue(defaultValue);
        parameter.setImportance(importance);
        parameter.setAdvFilterIndex(advFilterIndex);
        parameter.setCommonFilterIndex(commonFilterIndex);
        parameter.setIncutFilterIndex(incutFilterIndex);
        parameter.setHighlightOriginalValue(highlightOriginalValue);
        parameter.setSortMethod(sortMethod);
        parameter.setFillDifficulty(fillDifficulty);
        parameter.setFillValueFromOffers(fillValueFromOffers);
        parameter.setMatchingType(matchingType);
        parameter.setJoinTag(joinTag);
        parameter.setFormalizerTag(formalizerTag);
        parameter.setAccess(access);
        parameter.setPublished(published);
        parameter.setDescription(description);
        parameter.setComment(comment);
        parameter.setTaskGenInfo(taskGenInfo);
        parameter.setHasBoolNo(hasBoolNo);
        parameter.setSubtype(subtype);
        parameter.setClusterBreaker(clusterBreaker);
        parameter.setMainSizeMeasure(mainSizeMeasure);
        parameter.setOptionalSizeMeasures(optionalSizeMeasures);
        parameter.setFormalizationScope(formalizationScope);
        parameter.setDoNotFormalizePatterns(doNotFormalizePatterns);
        parameter.setOnlyForClusterCard(onlyForClusterCard);
        parameter.setDontUseAsAlias(dontUseAsAlias);
        parameter.setOutputIndex(outputIndex);
        parameter.setThrough(through);
        parameter.setClusterFilter(clusterFilter);
        parameter.setWeight(weight);
        parameter.setSuperParameter(superParameter);
        parameter.setMandatory(mandatory);
        parameter.setOptionsCount(optionsCount);
        parameter.setAllowInTitleMaker(allowInTitleMaker);
        parameter.setTimestamp(timestamp);

        parameter.setShortEnumCount(shortEnumCount);
        parameter.setShortEnumSortType(shortEnumSortType);
        parameter.setUseForImages(useForImages);
        parameter.setShowOnSkuTab(isShowOnSkuTab);
        parameter.setBlueGrouping(blueGrouping);
        parameter.setMdmParameter(mdmParameter);
        parameter.setSkuParameterMode(skuParameterMode);
        options.forEach(o -> o.setParamId(id));
        return parameter;
    }
}
