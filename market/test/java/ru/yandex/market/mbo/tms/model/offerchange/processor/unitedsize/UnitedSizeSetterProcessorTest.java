package ru.yandex.market.mbo.tms.model.offerchange.processor.unitedsize;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOfferMarketContent;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.export.modelstorage.pipe.CategoryInfo;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleOption;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleSizeMeasure;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.ReflectionUtils;
import ru.yandex.utils.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Market.DataCamp.DataCampOfferMarketContent.MarketSpecificContent;

@SuppressWarnings("checkstyle:magicnumber")
public class UnitedSizeSetterProcessorTest {

    private static final String PARAM_NAME = "Единый размер";

    @Test
    @DisplayName("Категория не является размерной")
    public void notSizeCategoryTest() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();
        categoryInfoMap.put(categoryId, createCategoryInfo(
            categoryId, null, Collections.emptyList(), Collections.emptyList()
        ));
        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );
        MarketSpecificContent content = MarketSpecificContent.newBuilder().build();
        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId + 1,
            null, null, content);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Категория не содержит параметр united_size")
    public void categoryDoesNotHaveUnitedSizeParamTest() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();
        categoryInfoMap.put(categoryId, createCategoryInfo(
            categoryId, null, Collections.emptyList(), Collections.emptyList()
        ));
        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );
        MarketSpecificContent content = MarketSpecificContent.newBuilder().build();
        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId, null, null, content);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Категория не содержит шаблон")
    public void setUnitedSizeWithoutTemplate() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();

        List<Long> definingParametersIds = Stream.of(1L, 2L).collect(Collectors.toList());
        Map<Long, Pair<Long, String>> definingParametersOptions = definingParametersIds.stream()
            .collect(Collectors.toMap(id -> id, id -> new Pair<>(id + 10, "option" + id)));

        List<ForTitleParameter> parameters = Stream.concat(
            definingParametersIds.stream()
                .map(id -> createParameter(id, SkuParameterMode.SKU_DEFINING, "xsl_name_" + id))
                .peek(forTitleParameter -> {
                    ForTitleOption option = new ForTitleOption();
                    option.setId(definingParametersOptions.get(forTitleParameter.getId()).getFirst());
                    option.setName(definingParametersOptions.get(forTitleParameter.getId()).getSecond());
                    forTitleParameter.setOptions(Collections.singletonList(option));
                }),
            Stream.of(
                createUnitedSizeParameter(),
                createParameter(3L, SkuParameterMode.SKU_NONE, "xsl_name_3")
            )
        ).collect(Collectors.toList());

        List<ForTitleSizeMeasure> sizeMeasures = definingParametersIds.stream()
            .map(this::createSizeMeasure)
            .collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(categoryId, null, parameters, sizeMeasures);

        categoryInfoMap.put(categoryId, categoryInfo);

        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );

        categoryInfoMap.put(
            categoryId,
            createCategoryInfo(categoryId, null, parameters, sizeMeasures)
        );

        MarketSpecificContent content = MarketSpecificContent.newBuilder()
            .setParameterValues(createMarketParameterValues(
                parameters.stream().filter(e -> e.getSkuParameterMode() != null &&
                    e.getSkuParameterMode().equals(SkuParameterMode.SKU_DEFINING))
                    .map(e -> createMarketParameterValue(e.getId(), e.getXslName(), e.getOptions().get(0).getId()))
                    .collect(Collectors.toList())
            ))
            .build();
        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId, null, null, content);
        final String expectedResult = parameters.stream().sorted(Comparator.comparing(ForTitleParameter::getId))
            .filter(p -> p.getOptions() != null && !p.getOptions().isEmpty())
            .map(p -> p.getOptions().get(0).getName())
            .collect(Collectors.joining("/"));

        Optional<DataCampContentMarketParameterValue.MarketParameterValue> unitedSize =
            result.get().getParameterValues()
            .getParameterValuesList()
            .stream()
            .filter(pv -> pv.getParamName().equals(PARAM_NAME))
            .findAny();

        Assert.assertTrue(unitedSize.isPresent());
        Assert.assertEquals(
            DataCampContentMarketParameterValue.MarketValueType.HYPOTHESIS,
            unitedSize.get().getValue().getValueType()
        );
        Assert.assertEquals(
            expectedResult,
            unitedSize.get().getValue().getStrValue()
        );
    }

    @Test
    @DisplayName("Категория содержит шаблон")
    public void setUnitedSizeByTemplate() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();

        List<Long> definingParametersIds = Stream.of(1L, 2L).collect(Collectors.toList());
        Map<Long, Pair<Long, String>> definingParametersOptions = definingParametersIds.stream()
            .collect(Collectors.toMap(id -> id, id -> new Pair<>(id + 10, "option" + id)));

        List<ForTitleParameter> parameters = Stream.concat(
            definingParametersIds.stream()
                .map(id -> createParameter(id, SkuParameterMode.SKU_DEFINING, "xsl_name_" + id))
                .peek(forTitleParameter -> {
                    ForTitleOption option = new ForTitleOption();
                    option.setId(definingParametersOptions.get(forTitleParameter.getId()).getFirst());
                    option.setName(definingParametersOptions.get(forTitleParameter.getId()).getSecond());
                    forTitleParameter.setOptions(Collections.singletonList(option));
                }),
            Stream.of(
                createUnitedSizeParameter(),
                createParameter(3L, SkuParameterMode.SKU_NONE, "xsl_name_3")
            )
        ).collect(Collectors.toList());

        List<ForTitleSizeMeasure> sizeMeasures = definingParametersIds.stream()
            .map(this::createSizeMeasure)
            .collect(Collectors.toList());

        final String expectedTemplateValue = parameters.stream().sorted(Comparator.comparing(ForTitleParameter::getId))
            .filter(p -> p.getOptions() != null && !p.getOptions().isEmpty())
            .map(p -> p.getOptions().get(0).getName())
            .collect(Collectors.joining("/"));
        final String template = parameters.stream().sorted(Comparator.comparing(ForTitleParameter::getId))
            .filter(p -> SkuParameterMode.SKU_DEFINING.equals(p.getSkuParameterMode()))
            .map(p -> "{" + p.getXslName() + "}")
            .collect(Collectors.joining("/"));

        CategoryInfo categoryInfo = createCategoryInfo(categoryId, template, parameters, sizeMeasures);

        categoryInfoMap.put(categoryId, categoryInfo);

        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );

        categoryInfoMap.put(
            categoryId,
            createCategoryInfo(categoryId, null, parameters, sizeMeasures)
        );

        MarketSpecificContent content = MarketSpecificContent.newBuilder()
            .setParameterValues(createMarketParameterValues(
                parameters.stream().filter(e -> e.getSkuParameterMode() != null &&
                    e.getSkuParameterMode().equals(SkuParameterMode.SKU_DEFINING))
                    .map(e -> createMarketParameterValue(
                        e.getId(), "param_name for " + e.getXslName(), e.getOptions().get(0).getId())
                    )
                    .collect(Collectors.toList())
            ))
            .build();
        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId, null, null, content);

        Optional<DataCampContentMarketParameterValue.MarketParameterValue> unitedSize =
            result.get().getParameterValues()
            .getParameterValuesList()
            .stream()
            .filter(pv -> pv.getParamName().equals(PARAM_NAME))
            .findAny();

        Assert.assertTrue(unitedSize.isPresent());
        Assert.assertEquals(
            DataCampContentMarketParameterValue.MarketValueType.HYPOTHESIS,
            unitedSize.get().getValue().getValueType()
        );
        Assert.assertEquals(
            expectedTemplateValue,
            unitedSize.get().getValue().getStrValue()
        );
    }

    @Test
    @DisplayName("Категория содержит шаблон, в котором участвует параметр, отсутствующий в оффере")
    public void setUnitedSizeByTemplateWhenTemplateHasUndefinedParameter() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();

        List<Long> definingParametersIds = Stream.of(1L, 2L).collect(Collectors.toList());
        Map<Long, Pair<Long, String>> definingParametersOptions = definingParametersIds.stream()
            .collect(Collectors.toMap(id -> id, id -> new Pair<>(id + 10, "option" + id)));

        List<ForTitleParameter> parameters = Stream.concat(
            definingParametersIds.stream()
                .map(id -> createParameter(id, SkuParameterMode.SKU_DEFINING, "xsl_name_" + id))
                .peek(forTitleParameter -> {
                    ForTitleOption option = new ForTitleOption();
                    option.setId(definingParametersOptions.get(forTitleParameter.getId()).getFirst());
                    option.setName(definingParametersOptions.get(forTitleParameter.getId()).getSecond());
                    forTitleParameter.setOptions(Collections.singletonList(option));
                }),
            Stream.of(
                createUnitedSizeParameter(),
                createParameter(3L, SkuParameterMode.SKU_NONE, "xsl_name_3")
            )
        ).collect(Collectors.toList());

        List<ForTitleSizeMeasure> sizeMeasures = definingParametersIds.stream()
            .map(this::createSizeMeasure)
            .collect(Collectors.toList());

        final String expectedTemplateValue = parameters.stream().sorted(Comparator.comparing(ForTitleParameter::getId))
            .filter(p -> p.getOptions() != null && !p.getOptions().isEmpty())
            .map(p -> p.getOptions().get(0).getName())
            .collect(Collectors.joining("/"));

        final String template = Stream.concat(
            parameters.stream().sorted(Comparator.comparing(ForTitleParameter::getId))
                .filter(p -> SkuParameterMode.SKU_DEFINING.equals(p.getSkuParameterMode()))
                .map(p -> "{" + p.getXslName() + "}"),
            Stream.of("{undefined_parameter}")
        )
            .collect(Collectors.joining("/"));

        CategoryInfo categoryInfo = createCategoryInfo(categoryId, template, parameters, sizeMeasures);

        categoryInfoMap.put(categoryId, categoryInfo);

        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );

        categoryInfoMap.put(
            categoryId,
            createCategoryInfo(categoryId, null, parameters, sizeMeasures)
        );

        MarketSpecificContent content = MarketSpecificContent.newBuilder()
            .setParameterValues(createMarketParameterValues(
                parameters.stream().filter(e -> e.getSkuParameterMode() != null &&
                    e.getSkuParameterMode().equals(SkuParameterMode.SKU_DEFINING))
                    .map(e -> createMarketParameterValue(
                        e.getId(), "param_name for " + e.getXslName(), e.getOptions().get(0).getId()
                    ))
                    .collect(Collectors.toList())
            ))
            .build();
        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId, null, null, content);

        Optional<DataCampContentMarketParameterValue.MarketParameterValue> unitedSize =
            result.get().getParameterValues()
            .getParameterValuesList()
            .stream()
            .filter(pv -> pv.getParamName().equals(PARAM_NAME))
            .findAny();

        Assert.assertTrue(unitedSize.isPresent());
        Assert.assertEquals(
            DataCampContentMarketParameterValue.MarketValueType.HYPOTHESIS,
            unitedSize.get().getValue().getValueType()
        );
        Assert.assertEquals(
            expectedTemplateValue,
            unitedSize.get().getValue().getStrValue()
        );
    }

    @Test
    @DisplayName("Категория не содержит шаблон, оффер не содержит определяющих размерных параметров")
    public void notSetUnitedSizeWithoutTemplateAndWithoutDefiningParameters() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();

        List<Long> definingParametersIds = Stream.of(1L, 2L).collect(Collectors.toList());
        Map<Long, Pair<Long, String>> definingParametersOptions = definingParametersIds.stream()
            .collect(Collectors.toMap(id -> id, id -> new Pair<>(id + 10, "option" + id)));

        List<ForTitleParameter> parameters = Stream.concat(
            definingParametersIds.stream()
                .map(id -> createParameter(id, SkuParameterMode.SKU_DEFINING, "xsl_name_" + id))
                .peek(forTitleParameter -> {
                    ForTitleOption option = new ForTitleOption();
                    option.setId(definingParametersOptions.get(forTitleParameter.getId()).getFirst());
                    option.setName(definingParametersOptions.get(forTitleParameter.getId()).getSecond());
                    forTitleParameter.setOptions(Collections.singletonList(option));
                }),
            Stream.of(
                createUnitedSizeParameter(),
                createParameter(3L, SkuParameterMode.SKU_NONE, "xsl_name_3")
            )
        ).collect(Collectors.toList());

        List<ForTitleSizeMeasure> sizeMeasures = definingParametersIds.stream()
            .map(this::createSizeMeasure)
            .collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(categoryId, null, parameters, sizeMeasures);

        categoryInfoMap.put(categoryId, categoryInfo);

        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );

        categoryInfoMap.put(
            categoryId,
            createCategoryInfo(categoryId, null, parameters, sizeMeasures)
        );

        MarketSpecificContent content = MarketSpecificContent.newBuilder().build();

        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId, null, null, content);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Категория не содержит шаблон и определяющих размерных параметров")
    public void notSetUnitedSizeCategoryWithoutTemplateAndDefiningParameters() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();

        List<Long> offerParametersIds = Stream.of(1L, 2L).collect(Collectors.toList());
        Map<Long, Pair<Long, String>> offerParametersOptions = offerParametersIds.stream()
            .collect(Collectors.toMap(id -> id, id -> new Pair<>(id + 10, "option" + id)));

        List<ForTitleParameter> offerParameters = offerParametersIds.stream()
            .map(id -> createParameter(id, SkuParameterMode.SKU_DEFINING, "xsl_name_" + id))
            .peek(forTitleParameter -> {
                ForTitleOption option = new ForTitleOption();
                option.setId(offerParametersOptions.get(forTitleParameter.getId()).getFirst());
                option.setName(offerParametersOptions.get(forTitleParameter.getId()).getSecond());
                forTitleParameter.setOptions(Collections.singletonList(option));
            }).collect(Collectors.toList());

        List<ForTitleParameter> parameters = Stream.of(
            createUnitedSizeParameter(),
            createParameter(3L, SkuParameterMode.SKU_NONE, "xsl_name_3")
        ).collect(Collectors.toList());

        CategoryInfo categoryInfo = createCategoryInfo(categoryId, null, parameters, Collections.emptyList());

        categoryInfoMap.put(categoryId, categoryInfo);

        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );

        MarketSpecificContent content = MarketSpecificContent.newBuilder()
            .setParameterValues(createMarketParameterValues(
                offerParameters.stream().filter(e -> e.getSkuParameterMode() != null)
                    .map(e -> createMarketParameterValue(e.getId(), e.getXslName(), e.getOptions().get(0).getId()))
                    .collect(Collectors.toList())
            ))
            .build();

        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId, null, null, content);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Категория содержит шаблон из параметров, которых нет в оффере")
    public void notSetUnitedSizeWithTemplateOfParametersNotInOffer() throws Exception {
        final Long categoryId = 1L;
        Map<Long, CategoryInfo> categoryInfoMap = new HashMap<>();

        List<Long> definingParametersIds = Stream.of(1L, 2L).collect(Collectors.toList());
        Map<Long, Pair<Long, String>> definingParametersOptions = definingParametersIds.stream()
            .collect(Collectors.toMap(id -> id, id -> new Pair<>(id + 10, "option" + id)));

        List<ForTitleParameter> parameters = Stream.concat(
            definingParametersIds.stream()
                .map(id -> createParameter(id, SkuParameterMode.SKU_DEFINING, "xsl_name_" + id))
                .peek(forTitleParameter -> {
                    ForTitleOption option = new ForTitleOption();
                    option.setId(definingParametersOptions.get(forTitleParameter.getId()).getFirst());
                    option.setName(definingParametersOptions.get(forTitleParameter.getId()).getSecond());
                    forTitleParameter.setOptions(Collections.singletonList(option));
                }),
            Stream.of(
                createUnitedSizeParameter(),
                createParameter(3L, SkuParameterMode.SKU_NONE, "xsl_name_3")
            )
        ).collect(Collectors.toList());

        List<ForTitleSizeMeasure> sizeMeasures = definingParametersIds.stream()
            .map(this::createSizeMeasure)
            .collect(Collectors.toList());

        final String template =
            String.join("/", "{undefined_parameter1}", "{undefined_parameter1}");

        CategoryInfo categoryInfo = createCategoryInfo(categoryId, template, parameters, sizeMeasures);

        categoryInfoMap.put(categoryId, categoryInfo);

        UnitedSizeSetterProcessor unitedSizeSetterProcessor = new UnitedSizeSetterProcessor(
            initCategoryCache(categoryInfoMap)
        );

        categoryInfoMap.put(
            categoryId,
            createCategoryInfo(categoryId, null, parameters, sizeMeasures)
        );

        MarketSpecificContent content = MarketSpecificContent.newBuilder()
            .setParameterValues(createMarketParameterValues(
                parameters.stream().filter(e -> e.getSkuParameterMode() != null &&
                    e.getSkuParameterMode().equals(SkuParameterMode.SKU_DEFINING))
                    .map(e -> createMarketParameterValue(
                        e.getId(), "param_name for " + e.getXslName(), e.getOptions().get(0).getId()
                    ))
                    .collect(Collectors.toList())
            ))
            .build();
        Optional<MarketSpecificContent> result = unitedSizeSetterProcessor.processData(categoryId, null, null, content);

        Assert.assertFalse(result.isPresent());
    }

    private DataCampOfferMarketContent.MarketParameterValues createMarketParameterValues(
        List<DataCampContentMarketParameterValue.MarketParameterValue> values
    ) {
        DataCampOfferMarketContent.MarketParameterValues.Builder builder =
            DataCampOfferMarketContent.MarketParameterValues.newBuilder();

        values.forEach(builder::addParameterValues);

        return builder.build();
    }

    private DataCampContentMarketParameterValue.MarketParameterValue createMarketParameterValue(Long paramId,
                                                                                                String paramName,
                                                                                                Long optionId) {
        DataCampContentMarketParameterValue.MarketParameterValue.Builder builder =
            DataCampContentMarketParameterValue.MarketParameterValue.newBuilder();
        builder.setParamId(paramId);
        builder.setParamName(paramName);
        builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
            .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
            .setOptionId(optionId)
            .build()
        );
        return builder.build();
    }

    private SizeCategoryInfoCache initCategoryCache(Map<Long, CategoryInfo> categoryInfoMap) throws Exception {

        SizeCategoryInfoCache sizeCategoryInfoCache = new SizeCategoryInfoCache();

        categoryInfoMap.values().forEach(category -> {
            sizeCategoryInfoCache.loadDefiningParameters(category);
            sizeCategoryInfoCache.loadCategorySizeMeasureParametersWithNames(category);
        });

        Map<Long, String> unitedSizeTemplate = new HashMap<>();
        categoryInfoMap.values().forEach(category -> {
            unitedSizeTemplate.put(category.getCategoryId(), category.getUnitedSizeTemplate());
        });
        ReflectionUtils.set(
            sizeCategoryInfoCache,
            "categoryUnitedSizeTemplateCache",
            unitedSizeTemplate
        );
        ReflectionUtils.set(
            sizeCategoryInfoCache,
            "categoryInfosCache",
            categoryInfoMap
        );

        return sizeCategoryInfoCache;
    }

    private CategoryInfo createCategoryInfo(Long id,
                                            String unitedSizeTemplate,
                                            List<ForTitleParameter> parameters,
                                            List<ForTitleSizeMeasure> sizeMeasureNames) {
        return new CategoryInfo(
            id, true, Collections.emptyList(), Collections.emptyList(), Collections.emptySet(),
            "name", new TMTemplate(), unitedSizeTemplate,
            parameters, sizeMeasureNames, null, null
        );
    }

    private ForTitleParameter createParameter(Long id,
                                              SkuParameterMode mode,
                                              String xslName) {
        ForTitleParameter parameter = new ForTitleParameter();
        parameter.setId(id);
        parameter.setSkuParameterMode(mode);
        parameter.setXslName(xslName);
        return parameter;
    }

    private ForTitleSizeMeasure createSizeMeasure(Long id) {
        return ForTitleSizeMeasure.from(id + 1, "name " + id, 0, id, 0, 0);
    }

    private ForTitleParameter createUnitedSizeParameter() {
        ForTitleParameter parameter = new ForTitleParameter();
        parameter.setId(999L);
        parameter.setXslName(XslNames.UNITED_SIZE);
        return parameter;
    }

}
