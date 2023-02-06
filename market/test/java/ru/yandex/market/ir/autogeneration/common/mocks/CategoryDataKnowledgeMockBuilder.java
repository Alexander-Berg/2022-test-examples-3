package ru.yandex.market.ir.autogeneration.common.mocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import ru.yandex.market.extractor.ExtractorConfig;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.util.RandomValuesGenerator;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.robot.db.ParameterValueComposer;

public class CategoryDataKnowledgeMockBuilder {
    private final Map<Long, CategoryData> categoryDataHelperMap = new HashMap<>();

    private CategoryDataKnowledgeMockBuilder() {
    }

    public static CategoryDataKnowledgeMockBuilder builder() {
        return new CategoryDataKnowledgeMockBuilder();
    }

    public CategoryDataMockBuilder startCategory(long categoryId) {
        return new CategoryDataMockBuilder(categoryId, this);
    }

    public CategoryDataKnowledge build() {
        CategoryDataKnowledge categoryDataKnowledge = Mockito.mock(CategoryDataKnowledge.class);
        Mockito
            .when(categoryDataKnowledge.getCategoryData(Mockito.anyLong()))
            .then(req -> categoryDataHelperMap.get(req.getArgument(0)));
        return categoryDataKnowledge;
    }

    public static class CategoryDataMockBuilder {
        private final long categoryId;
        private String name;
        private String uniqueName;
        private boolean leaf = true;
        private boolean allowNonWhiteFirstPictureBackground = false;
        private final List<MboParameters.Parameter> parameters = new ArrayList<>();
        private List<MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse> sizeMeasuresList = new ArrayList<>();
        private final List<MboParameters.ParameterValueLinks> parameterValueLinks = new ArrayList<>();

        private final CategoryDataKnowledgeMockBuilder upper;

        public CategoryDataMockBuilder(long categoryId, CategoryDataKnowledgeMockBuilder upper) {
            this.categoryId = categoryId;
            this.upper = upper;
        }

        public CategoryDataMockBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public CategoryDataMockBuilder setUniqueName(String uniqueName) {
            this.uniqueName = uniqueName;
            return this;
        }

        public CategoryDataMockBuilder setLeaf(boolean leaf) {
            this.leaf = leaf;
            return this;
        }

        public CategoryDataMockBuilder setAllowNonWhiteFirstPictureBackground(
            boolean allowNonWhiteFirstPictureBackground
        ) {
            this.allowNonWhiteFirstPictureBackground = allowNonWhiteFirstPictureBackground;
            return this;
        }


        public CategoryDataMockBuilder setSizeMeasureList(List<MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse> sizeMeasuresList) {
            this.sizeMeasuresList = sizeMeasuresList;
            return this;
        }

        public CategoryDataMockBuilder linkParamValues(
            long paramId,
            long linkedParamId,
            Map<Long, List<Long>> paramOptionIdToLinkedOptionIds) {
            parameterValueLinks.add(
                MboParameters.ParameterValueLinks.newBuilder()
                    .setParamId(paramId)
                    .setLinkedParamId(linkedParamId)
                    .setType(MboParameters.ValueLinkRestrictionType.DIRECT)
                    .addAllLinkedValue(paramOptionIdToLinkedOptionIds.entrySet().stream()
                        .map(entry -> MboParameters.ValueLink.newBuilder()
                            .setOptionId(entry.getKey())
                            .addAllLinkedOptionId(entry.getValue())
                            .build())
                        .collect(Collectors.toList()))
                    .build());
            return this;
        };

        public EnumParameterBuilder enumParameterBuilder() {
            return new EnumParameterBuilder(this);
        }

        public EnumParameterBuilder createRandomEnumParameterBuilder() {
            return new EnumParameterBuilder(this)
                .setParamId(RandomValuesGenerator.randomPositiveInt())
                .setXlsName(RandomValuesGenerator.randomAlphabetic8());
        }

        public EnumParameterBuilder vendorParameterBuilder() {
            return new EnumParameterBuilder(this)
                .setParamId(ParameterValueComposer.VENDOR_ID)
                .setXlsName(ParameterValueComposer.VENDOR)
                .setHidden(true);
        }

        public NumericParameterBuilder numericParameterBuilder() {
            return new NumericParameterBuilder(this);
        }

        public NumericParameterBuilder createRandomNumericParameterBuilder() {
            return new NumericParameterBuilder(this)
                .setParamId(RandomValuesGenerator.randomPositiveInt())
                .setXlsName(RandomValuesGenerator.randomAlphabetic8());
        }

        public NumericParameterBuilder numericEnumParameterBuilder() {
            return new NumericParameterBuilder(this);
        }

        public NumericEnumParameterBuilder createRandomNumericEnumParameterBuilder() {
            return new NumericEnumParameterBuilder(this)
                .setParamId(RandomValuesGenerator.randomPositiveInt())
                .setXlsName(RandomValuesGenerator.randomAlphabetic8());
        }

        public NumericParameterBuilder booleanParameterBuilder() {
            return new NumericParameterBuilder(this);
        }

        public BooleanParameterBuilder createRandomBooleanParameterBuilder() {
            return new BooleanParameterBuilder(this)
                .setParamId(RandomValuesGenerator.randomPositiveInt())
                .setXlsName(RandomValuesGenerator.randomAlphabetic8());
        }

        public StringParameterBuilder stringParameterBuilder() {
            return new StringParameterBuilder(this);
        }

        public StringParameterBuilder stringParameterBuilder(long paramId, String xlsName) {
            return new StringParameterBuilder(this).setParamId(paramId).setXlsName(xlsName);
        }

        public StringParameterBuilder createRandomStringEnumParameterBuilder() {
            return new StringParameterBuilder(this)
                .setParamId(RandomValuesGenerator.randomPositiveInt())
                .setXlsName(RandomValuesGenerator.randomAlphabetic8());
        }

        public StringParameterBuilder nameParameterBuilder() {
            return new StringParameterBuilder(this)
                .setParamId(ParameterValueComposer.NAME_ID)
                .setXlsName(ParameterValueComposer.NAME)
                .setHidden(true)
                .setService(true);
        }

        public StringParameterBuilder barCodeParameterBuilder() {
            return new StringParameterBuilder(this)
                .setParamId(ParameterValueComposer.BARCODE_ID)
                .setXlsName(ParameterValueComposer.BARCODE)
                .setService(true);
        }

        public StringParameterBuilder vendorCodeParameterBuilder() {
            return new StringParameterBuilder(this)
                .setParamId(ParameterValueComposer.VENDOR_CODE_ID)
                .setXlsName(ParameterValueComposer.VENDOR_CODE)
                .setService(true);
        }

        public CategoryDataKnowledgeMockBuilder build() {
            upper.categoryDataHelperMap.put(categoryId, this.construct());
            return upper;
        }

        public CategoryDataKnowledgeMockBuilder buildWithCategoryData(CategoryData categoryData) {
            upper.categoryDataHelperMap.put(categoryId, categoryData);
            return upper;
        }

        private CategoryData construct() {
            MboParameters.Category.Builder builder = MboParameters.Category.newBuilder()
                .setHid(categoryId)
                .setLeaf(leaf)
                .setAllowNonWhiteFirstPictureBackground(allowNonWhiteFirstPictureBackground)
                .addAllParameter(parameters);
            if (name != null) {
                builder.addName(word(name));
            }
            if (uniqueName != null) {
                builder.addUniqueName(word(name));
            }
            if (!parameterValueLinks.isEmpty()) {
                builder.addAllParameterValueLinks(parameterValueLinks);
            }
            return CategoryData.build(builder, sizeMeasuresList, "", Set.of());
        }
    }

    public static abstract class ParameterBuilder<T extends ParameterBuilder<?>> {
        private long paramId;
        private String xlsName;
        private MboParameters.ParameterLevel level = MboParameters.ParameterLevel.MODEL_LEVEL;
        private boolean useForGuru = true;
        private MboParameters.SKUParameterMode skuMode = MboParameters.SKUParameterMode.SKU_NONE;
        private boolean mandatory = false;
        private boolean extractInSkubd = false;
        private boolean multivalue;
        private boolean hidden;
        private boolean service;
        private boolean mdmParameter;
        private MboParameters.SubType subType = MboParameters.SubType.NOT_DEFINED;
        private final CategoryDataMockBuilder upper;
        private boolean showOnSkuTab;
        private boolean mandatoryForPartner;

        private ParameterBuilder(CategoryDataMockBuilder upper) {
            this.upper = upper;
        }

        protected abstract MboParameters.ValueType getValueType();

        public T setParamId(long paramId) {
            this.paramId = paramId;
            return (T) this;
        }

        public T setXlsName(String xlsName) {
            this.xlsName = xlsName;
            return (T) this;
        }

        public T setLevel(MboParameters.ParameterLevel level) {
            this.level = level;
            return (T) this;
        }

        public T setUseForGuru(boolean useForGuru) {
            this.useForGuru = useForGuru;
            return (T) this;
        }

        public T setSkuMode(MboParameters.SKUParameterMode skuMode) {
            this.skuMode = skuMode;
            return (T) this;
        }

        public T setMandatory(boolean mandatory) {
            this.mandatory = mandatory;
            return (T) this;
        }

        public T setMandatoryForPartner(boolean mandatoryForPartner) {
            this.mandatoryForPartner = mandatoryForPartner;
            return (T) this;
        }

        public T setExtractInSkubd(boolean extractInSkubd) {
            this.extractInSkubd = extractInSkubd;
            return (T) this;
        }

        public T setMultivalue(boolean multivalue) {
            this.multivalue = multivalue;
            return (T) this;
        }

        public T setHidden(boolean hidden) {
            this.hidden = hidden;
            return (T) this;
        }

        public T setService(boolean service) {
            this.service = service;
            return (T) this;
        }

        public T setMdmParameter(boolean mdmParameter) {
            this.mdmParameter = mdmParameter;
            return (T) this;
        }

        public T setSubType(MboParameters.SubType subType) {
            this.subType = subType;
            return (T) this;
        }

        public T setShowOnSkuTab(boolean showOnSkuTab) {
            this.showOnSkuTab = showOnSkuTab;
            return (T) this;
        }

        public CategoryDataMockBuilder build() {
            MboParameters.Parameter.Builder builder = MboParameters.Parameter.newBuilder();
            fillBuilder(builder);
            upper.parameters.add(builder.build());
            return upper;
        }

        protected void fillBuilder(MboParameters.Parameter.Builder builder) {
            builder
                .setId(paramId)
                .setXslName(xlsName)
                .setValueType(getValueType())
                .setSubType(subType)
                .setParamType(level)
                .setSkuMode(skuMode)
                .setIsUseForGuru(useForGuru)
                .setExtractInSkubd(extractInSkubd)
                .setMultivalue(multivalue)
                .setHidden(hidden)
                .setService(service)
                .setMdmParameter(mdmParameter)
                    .setMandatoryForPartner(mandatoryForPartner)
                .setShowOnSkuTab(showOnSkuTab);
            if (skuMode == MboParameters.SKUParameterMode.SKU_NONE) {
                builder.setMandatoryForSignature(mandatory);
            } else {
                builder.setMandatory(mandatory);
            }
        }
    }

    public static class BooleanParameterBuilder extends ParameterBuilder<BooleanParameterBuilder> {
        private List<MboParameters.Option> options = new ArrayList<>(2);

        private BooleanParameterBuilder(CategoryDataMockBuilder upper) {
            super(upper);
        }

        @Override
        protected MboParameters.ValueType getValueType() {
            return MboParameters.ValueType.BOOLEAN;
        }

        public BooleanParameterBuilder addOption(long optionId, boolean value, String name) {
            options.add(
                MboParameters.Option.newBuilder()
                    .setId(optionId)
                    .addName(word(value ? "true" : "false"))
                    .addAlias(
                        MboParameters.EnumAlias.newBuilder()
                            .setAlias(word(name))
                            .build()
                    )
                    .build()
            );
            return this;
        }

        @Override
        protected void fillBuilder(MboParameters.Parameter.Builder builder) {
            super.fillBuilder(builder);
            builder.addAllOption(options);
        }
    }

    public static class NumericEnumParameterBuilder extends ParameterBuilder<NumericEnumParameterBuilder> {
        private List<MboParameters.Option> options = new ArrayList<>();

        private NumericEnumParameterBuilder(CategoryDataMockBuilder upper) {
            super(upper);
        }

        @Override
        protected MboParameters.ValueType getValueType() {
            return MboParameters.ValueType.NUMERIC_ENUM;
        }

        public NumericEnumParameterBuilder addOption(long optionId, Number value) {
            options.add(
                MboParameters.Option.newBuilder().setId(optionId)
                    .addName(word(String.valueOf(value)))
                    .build()
            );
            return this;
        }

        @Override
        protected void fillBuilder(MboParameters.Parameter.Builder builder) {
            super.fillBuilder(builder);
            builder.addAllOption(options);
        }
    }

    public static class NumericParameterBuilder extends ParameterBuilder<NumericParameterBuilder> {
        private Double minValue;
        private Double maxValue;

        private NumericParameterBuilder(CategoryDataMockBuilder upper) {
            super(upper);
        }

        public NumericParameterBuilder setMinValue(Double minValue) {
            this.minValue = minValue;
            return this;
        }

        public NumericParameterBuilder setMaxValue(Double maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        @Override
        protected MboParameters.ValueType getValueType() {
            return MboParameters.ValueType.NUMERIC;
        }

        @Override
        protected void fillBuilder(MboParameters.Parameter.Builder builder) {
            super.fillBuilder(builder);
            if (minValue != null) {
                builder.setMinValue(minValue);
            }
            if (maxValue != null) {
                builder.setMaxValue(maxValue);
            }
        }
    }

    public static class EnumParameterBuilder extends ParameterBuilder<EnumParameterBuilder> {
        private List<MboParameters.Option> options = new ArrayList<>();

        private EnumParameterBuilder(CategoryDataMockBuilder upper) {
            super(upper);
        }

        @Override
        protected MboParameters.ValueType getValueType() {
            return MboParameters.ValueType.ENUM;
        }

        public EnumParameterBuilder addOption(long optionId, String name) {
            return optionBuilder().setOptionId(optionId).setName(name).build();
        }

        @Override
        protected void fillBuilder(MboParameters.Parameter.Builder builder) {
            super.fillBuilder(builder);
            builder.addAllOption(options);
        }

        public OptionBuilder optionBuilder() {
            return new OptionBuilder(this);
        }

        public OptionBuilder createRandomOptionBuilder() {
            return new OptionBuilder(this)
                .setOptionId(RandomValuesGenerator.randomPositiveInt())
                .setName(RandomValuesGenerator.randomAlphabetic8());
        }

        public static class OptionBuilder {
            private Long optionId;
            private String name;
            private Set<String> aliases = new HashSet<>();
            private Boolean isGuruVendor = null;
            private Boolean isFakeVendor = null;
            private final EnumParameterBuilder upper;

            private OptionBuilder(EnumParameterBuilder upper) {
                this.upper = upper;
            }

            public OptionBuilder setOptionId(long optionId) {
                this.optionId = optionId;
                return this;
            }

            public OptionBuilder setName(String name) {
                this.name = name;
                return this;
            }

            public OptionBuilder addAlias(String alis) {
                this.aliases.add(alis);
                return this;
            }

            public OptionBuilder setIsGuruVendor(Boolean guruVendor) {
                isGuruVendor = guruVendor;
                return this;
            }

            public OptionBuilder setIsFakeVendor(Boolean fakeVendor) {
                isFakeVendor = fakeVendor;
                return this;
            }

            public EnumParameterBuilder build() {
                MboParameters.Option.Builder optionBuilder =
                    MboParameters.Option.newBuilder().setId(optionId)
                        .addName(word(name));
                aliases.forEach(
                    v -> optionBuilder.addAlias(
                        MboParameters.EnumAlias.newBuilder()
                            .setAlias(word(v))
                            .setType(MboParameters.EnumAlias.Type.GENERAL)
                            .build()
                    )
                );
                if (isGuruVendor != null) {
                    optionBuilder.setIsGuruVendor(isGuruVendor);
                }
                if (isFakeVendor != null) {
                    optionBuilder.setIsFakeVendor(isFakeVendor);
                }
                upper.options.add(optionBuilder.build());
                return upper;
            }
        }
    }

    public static class StringParameterBuilder extends ParameterBuilder<StringParameterBuilder> {

        private StringParameterBuilder(CategoryDataMockBuilder upper) {
            super(upper);
        }

        @Override
        protected MboParameters.ValueType getValueType() {
            return MboParameters.ValueType.STRING;
        }
    }

    @NotNull
    private static MboParameters.Word.Builder word(String value) {
        return MboParameters.Word.newBuilder()
            .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE)
            .setName(value);
    }
}
