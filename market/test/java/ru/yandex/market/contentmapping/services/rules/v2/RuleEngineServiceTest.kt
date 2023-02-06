package ru.yandex.market.contentmapping.services.rules.v2

import com.google.common.collect.ImmutableMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterValue
import ru.yandex.market.contentmapping.dto.mapping.MarketParam
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.HypothesisValue
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.OptionValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingWithRules
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.MarketParameterValue
import ru.yandex.market.contentmapping.dto.model.Picture
import ru.yandex.market.contentmapping.dto.model.ValueSource
import ru.yandex.market.contentmapping.services.category.info.CategoryParameterInfoService
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingDirectStrategy
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingForceMappingStrategy
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingMappingStrategy
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingPictureStrategy
import ru.yandex.market.contentmapping.testdata.ParamMappingFactory
import ru.yandex.market.contentmapping.testdata.TestDataUtils.TEST_CATEGORY_ID
import ru.yandex.market.contentmapping.testdata.TestDataUtils.nextParameter
import ru.yandex.market.contentmapping.testdata.TestDataUtils.nextParameterEnum
import ru.yandex.market.contentmapping.testdata.TestDataUtils.nextShopModel
import ru.yandex.market.contentmapping.utils.MboParameterConstants
import ru.yandex.market.mbo.export.MboParameters


class RuleEngineServiceTest {
    private val weight = nextParameter("Вес").copy(valueType = MboParameters.ValueType.NUMERIC)
    private val color = nextParameterEnum("Цвет", 100, 120)
    private val colorVendor = nextParameterEnum("Вендорский цвет", 200, 210)
    private val categories: Map<Long, Map<Long, CategoryParameterInfo>> = mapOf(
            TEST_CATEGORY_ID to mapOf(
                    weight.parameterId to weight,
                    color.parameterId to color,
                    colorVendor.parameterId to colorVendor)
    )
    private val sampleModel = nextShopModel()
            .copy(shopValues = mapOf(
                    "weight" to "100",
                    "color" to "red",
                    "color_vendor" to "reddish, green, brown",
                    "height" to "200"))
    private lateinit var ruleEngineService: RuleEngineService
    private lateinit var mappingApplyer: MappingApplyer
    private lateinit var parametersService: CategoryParameterInfoService
    private lateinit var ruleApplyResultMerger: RuleApplyResultMerger

    @Before
    fun setup() {
        parametersService = Mockito.mock(CategoryParameterInfoService::class.java)
        mappingApplyer = MappingApplyer(
                parametersService,
                mapOf(
                        ParamMappingType.MAPPING to MappingMappingStrategy(),
                        ParamMappingType.FORCE_MAPPING to MappingForceMappingStrategy(),
                        ParamMappingType.DIRECT to MappingDirectStrategy(),
                        ParamMappingType.FIRST_PICTURE to MappingPictureStrategy(true),
                        ParamMappingType.PICTURE to MappingPictureStrategy(false),
                )
        )
        ruleApplyResultMerger = RuleApplyResultMerger()
        ruleEngineService = RuleEngineService(mappingApplyer, ruleApplyResultMerger)
    }

    @Test
    fun testSimpleResults() {
        val weightMapping = ParamMappingFactory.map(weight, "weight")
        val weight100 = weightMapping.rule("100", numericValueSet(100.0))
        val result = mappingApplyer.applyMappingToModel(
                weightMapping.mappingWithRules(),
                sampleModel.shopValues,
                categories[TEST_CATEGORY_ID]!!
        )?.merge()
        Assertions.assertThat(result).hasSize(1)
        val weightValues = result?.get(weight.parameterId)
        Assertions.assertThat(weightValues)
                .containsExactly(mpv(weight, ValueSource.RULE, weight100.id, weight100.getFirstMarketValue()))
    }

    @Test
    fun testMultipleRulesProvideValuesAndHypothesisAndFiltering() {
        // NOTE: splitting enabled
        val colorMapping = ParamMappingFactory.map(colorVendor, "color_vendor", ",")
        // Should be with source formalization
        val reddish = colorMapping.rule("reddish", optionValueSet(200)) { r: ParamMappingRule -> r.copy(isHypothesis = true) }
        val brown = colorMapping.rule("brown", optionValueSet(210))
        // incorrect for category => ignored
        colorMapping.rule("green", optionValueSet(1000))
        val result = mappingApplyer.applyMappingToModel(
                colorMapping.mappingWithRules(),
                sampleModel.shopValues,
                categories[TEST_CATEGORY_ID]!!
        )?.merge()

        Assertions.assertThat(result).hasSize(1)
        val colorVendorValues = result?.get(colorVendor.parameterId)
        Assertions.assertThat(colorVendorValues)
                .containsExactlyInAnyOrder(
                        mpv(colorVendor, ValueSource.FORMALIZATION, reddish.id, reddish.getFirstMarketValue()),
                        mpv(colorVendor, ValueSource.RULE, brown.id, brown.getFirstMarketValue()) // Green is filtered out - it's not in category
                )
    }

    @Test
    fun testEmptyMerge() {
        val model = nextShopModel()
        val updated = ruleApplyResultMerger.merge(
                model, RuleApplyResult(emptyMap())
        )
        Assertions.assertThat(model).isEqualTo(updated)
    }

    @Test
    fun testSimpleMerge() {
        val model = nextShopModel()
        val result = RuleApplyResult(mapOf(
                color.parameterId to listOf(
                        MarketParameterValue(
                                parameterId = color.parameterId,
                                valueSource = ValueSource.RULE,
                                value = optionValue(100),
                                ruleId = 1
                        )
                ),
                colorVendor.parameterId to listOf(
                        MarketParameterValue(
                                parameterId = colorVendor.parameterId,
                                valueSource = ValueSource.RULE,
                                value = optionValue(200),
                                ruleId = 2
                        )
                ),

                ))

        val updated = ruleApplyResultMerger.merge(model, result)
        Assertions.assertThat(model.marketValues).isEmpty()
        Assertions.assertThat(updated).isNotNull
        Assertions.assertThat(updated.marketValues).hasSize(2)
        Assertions.assertThat(updated.marketValues[color.parameterId]).allSatisfy { (_, valueSource, value) ->
            Assertions.assertThat(valueSource).isEqualTo(ValueSource.RULE)
            Assertions.assertThat(value).isEqualTo(optionValue(100))
        }
        Assertions.assertThat(updated.marketValues[colorVendor.parameterId]).allSatisfy { (_, valueSource, value) ->
            Assertions.assertThat(valueSource).isEqualTo(ValueSource.RULE)
            Assertions.assertThat(value).isEqualTo(optionValue(200))
        }

        // Second apply shouldn't change anything
        val secondChange = ruleApplyResultMerger.merge(updated, result)
        Assertions.assertThat(secondChange).isEqualTo(updated)
    }

    @Test
    fun testMergeKeepManualValues() {
        val parameters: Map<Long, List<MarketParameterValue>> = ImmutableMap.of(
                color.parameterId, listOf(mpv(color, ValueSource.MANUAL, 0, optionValue(110))),
                weight.parameterId, listOf(mpv(weight, ValueSource.RULE, 10, numericValue(100.0)))
        )
        val model = nextShopModel().copy(marketValues = parameters)
        val result = RuleApplyResult(
                mapOf(
                        color.parameterId to listOf(
                                MarketParameterValue(
                                        parameterId = color.parameterId,
                                        valueSource = ValueSource.RULE,
                                        value = optionValue(100),
                                        ruleId = 1
                                )
                        ),
                        colorVendor.parameterId to listOf(
                                MarketParameterValue(
                                        parameterId = colorVendor.parameterId,
                                        valueSource = ValueSource.RULE,
                                        value = optionValue(200),
                                        ruleId = 2
                                )
                        ),

                        )
        )
        val updated = ruleApplyResultMerger.merge(model, result)
        Assertions.assertThat(model.marketValues)
                .describedAs("Shouldn't change original params")
                .isSameAs(parameters)
        Assertions.assertThat(updated).isNotNull
        // Drop weight
        Assertions.assertThat(updated.marketValues).hasSize(2)
        // Keep manual
        Assertions.assertThat(updated.marketValues[color.parameterId])
                .contains(mpv(color, ValueSource.MANUAL, 0, optionValue(110)))
        Assertions.assertThat(updated.marketValues[colorVendor.parameterId])
                .containsExactly(mpv(colorVendor, ValueSource.RULE, 2, optionValue(200)))

        // Second apply shouldn't change anything
        val secondChange = ruleApplyResultMerger.merge(updated, result)
        Assertions.assertThat(secondChange).isEqualTo(updated)
    }

    @Test
    fun testMergeWithRulesDropWrongRule() {
        val parameters: Map<Long, List<MarketParameterValue>> = ImmutableMap.of(
                weight.parameterId, listOf(mpv(weight, ValueSource.RULE, 10, numericValue(100.0)))
        )
        val model = nextShopModel().copy(marketValues = parameters)
        val result = RuleApplyResult(
                mapOf(
                        weight.parameterId to listOf(
                                MarketParameterValue(
                                        parameterId = weight.parameterId,
                                        valueSource = ValueSource.FORMALIZATION,
                                        value = numericValue(100.0),
                                        ruleId = 10
                                )
                        ),
                )
        )

        val updated = ruleApplyResultMerger.merge(model, result)
        Assertions.assertThat(updated).describedAs("Should has changes").isNotNull
        Assertions.assertThat(updated.marketValues).hasSize(1)
        Assertions.assertThat(updated.marketValues[weight.parameterId])
                .containsExactly(mpv(weight, ValueSource.FORMALIZATION, 10, numericValue(100.0)))

        // Second apply shouldn't change anything
        val secondChange = ruleApplyResultMerger.merge(updated, result)
        Assertions.assertThat(secondChange).isEqualTo(updated)
    }

    @Test
    fun fullRun() {
        categories.forEach { (parameterId, categoryParameters) ->
            Mockito.`when`(parametersService.getCategoryParameters(parameterId)).thenReturn(categoryParameters)
        }
        val model1 = nextShopModel()
                .copy(shopValues = mapOf(
                        "weight" to "100",
                        "color" to "red",
                        "height" to "200"))
        val model2 = nextShopModel()
                .copy(shopValues = mapOf(
                        "weight" to "150",
                        "color" to "yellow"))
        val models = listOf(model1, model2)
        val weightMapping = ParamMappingFactory.map(weight, "weight")
        val weight100 = weightMapping.rule("100", numericValueSet(100.0))
        val colorMapping = ParamMappingFactory.map(color, "color")
        val colorRed = colorMapping.rule("red", optionValueSet(100)) { r: ParamMappingRule -> r.copy(isHypothesis = true) }
        colorMapping.rule("yellow", optionValueSet(100))
        val mappings = listOf(
                weightMapping.mappingWithRules(),
                colorMapping.mappingWithRules()
        )
        val changedModels = ruleEngineService.applyAllRules(models.asSequence(), mappings).toList()
        Assertions.assertThat(changedModels).hasSize(2)
        val values1 = changedModels[0].marketValues
        Assertions.assertThat(values1).hasSize(2)
        Assertions.assertThat(values1[color.parameterId])
                .containsExactlyInAnyOrder(mpv(color, ValueSource.FORMALIZATION, colorRed.id, colorRed.getFirstMarketValue()))
        Assertions.assertThat(values1[weight.parameterId])
                .containsExactlyInAnyOrder(mpv(weight, ValueSource.RULE, weight100.id, weight100.getFirstMarketValue()))
        val secondApply = ruleEngineService.applyAllRules(changedModels.asSequence(), mappings).toList()
        val values2 = changedModels[0].marketValues
        Assertions.assertThat(secondApply).hasSize(2)
        Assertions.assertThat(values2[color.parameterId])
                .containsExactlyInAnyOrder(mpv(color, ValueSource.FORMALIZATION, colorRed.id, colorRed.getFirstMarketValue()))
        Assertions.assertThat(values2[weight.parameterId])
                .containsExactlyInAnyOrder(mpv(weight, ValueSource.RULE, weight100.id, weight100.getFirstMarketValue()))

    }

    @Test
    fun testPicturesResult() {
        val shopValues = mapOf(
                "pics" to "http://1.gif, http://2.gif",
                "first_pic" to "http://first.gif")
        val pics = ParamMapping(
                mappingType = ParamMappingType.PICTURE,
                shopParams = listOf(ShopParam("pics", ",")),
        )
        val firstPic = ParamMapping(
                mappingType = ParamMappingType.FIRST_PICTURE,
                shopParams = listOf(ShopParam("first_pic", ",")),
        )
        val result1 = mappingApplyer.applyMappingToModel(
                ParamMappingWithRules(pics, listOf()),
                shopValues,
                categories[TEST_CATEGORY_ID]!!
        )

        val result2 = mappingApplyer.applyMappingToModel(
                ParamMappingWithRules(firstPic, listOf()),
                shopValues,
                categories[TEST_CATEGORY_ID]!!
        )

        val result = ((result1 ?: emptySequence()) + (result2 ?: emptySequence())).merge()

        val mainPicsResult = result[MboParameterConstants.MAIN_PICTURE_PARAM_ID]
        val picsResult = result[MboParameterConstants.PICTURE_PARAM_ID]

        Assertions.assertThat(mainPicsResult).hasSize(1)
        Assertions.assertThat(picsResult).hasSize(2)
        Assertions.assertThat(mainPicsResult?.map { (it.value as? MarketParamValue.StringValue)?.stringValue })
                .hasSize(1)
                .containsExactly("http://first.gif")
        Assertions.assertThat(picsResult?.map { (it.value as? MarketParamValue.StringValue)?.stringValue })
                .hasSize(2)
                .containsExactly("http://1.gif", "http://2.gif")
    }

    @Test
    fun testForceMappingResult() {
        val shopParamName = "param1"
        val shopParamValue = "value1"
        val categoryValueName = "value1"
        val marketCategoryId = 1L
        val marketParamId = 2L
        val marketValueId = 3
        val mappingId = 10
        val mappingRank = 10

        val shopValues = mapOf(shopParamName to shopParamValue)

        val originalMapping = ParamMapping(
            id = mappingId,
            mappingType = ParamMappingType.FORCE_MAPPING,
            shopParams = listOf(ShopParam(name = shopParamName)),
            marketParams = listOf(MarketParam(marketParamId)),
            rank = mappingRank
        )
        var mapping = originalMapping

        val options = Int2ObjectOpenHashMap(
            mapOf(
                marketValueId to CategoryParameterValue(
                    marketValueId,
                    categoryValueName
                )
            )
        )

        val categories = mapOf(
            marketParamId to CategoryParameterInfo(
                parameterId = marketParamId,
                xslName = "",
                name = "",
                unitName = "",
                valueType = MboParameters.ValueType.ENUM,
                isImportant = false,
                isMultivalue = false,
                isService = false,
                isRequiredForModelCreation = false,
                isMandatoryForSignature = false,
                commonFilterIndex = 0,
                options = options,
                commentForPartner = null,
                commentForOperator = null,
                minValue = null,
                maxValue = null,
        ))

        //Нет рулов, и опция отсутствует в категории. Должна создаться гипотеза
        var result = MappingForceMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                mapOf("param1" to "v1"),
                categories
        )?.merge()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)).isNotNull
        Assertions.assertThat(result?.get(marketParamId)).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.parameterId).isEqualTo(marketParamId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.valueSource).isEqualTo(ValueSource.RULE)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.value?.javaClass).isEqualTo(HypothesisValue::class.java)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? HypothesisValue)?.hypothesis).isEqualTo("v1")
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.ruleId).isEqualTo(-mappingId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.rank).isEqualTo(mappingRank)

        //Нет рулов, и опция присутствует в категории. Должно создаться значение с указанным optionId
        result = MappingForceMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                mapOf("param1" to categoryValueName),
                categories
        )?.merge()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)).isNotNull
        Assertions.assertThat(result?.get(marketParamId)).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.parameterId).isEqualTo(marketParamId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.valueSource).isEqualTo(ValueSource.RULE)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.value?.javaClass).isEqualTo(OptionValue::class.java)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.hypothesis)
            .isEqualTo(categoryValueName)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.optionId)
            .isEqualTo(marketValueId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.ruleId).isEqualTo(-mappingId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.rank).isEqualTo(mappingRank)

        //Есть рул, должно создаться соответствующее значение с указанным optionId
        val paramMappingRule = ParamMappingRule(
            id = 10,
            shopValues = mapOf("param1" to categoryValueName),
            marketValues = mapOf(marketParamId to setOf(OptionValue(marketValueId.toInt(), categoryValueName)))
        )

        result = MappingForceMappingStrategy().apply(
            ParamMappingWithRules(mapping, listOf(paramMappingRule)),
            mapOf("param1" to categoryValueName),
            categories
        )?.merge()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)).isNotNull
        Assertions.assertThat(result?.get(marketParamId)).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.parameterId).isEqualTo(marketParamId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.valueSource).isEqualTo(ValueSource.RULE)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.value?.javaClass).isEqualTo(OptionValue::class.java)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.hypothesis).isEqualTo(categoryValueName)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.optionId).isEqualTo(marketValueId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.ruleId).isEqualTo(10)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.rank).isEqualTo(mappingRank)

        mapping = originalMapping.copy(shopParams = listOf(ShopParam(name = shopParamName + "1")))
        result = MappingDirectStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                categories
        )?.merge()
        Assertions.assertThat(result).isNull()

        mapping = originalMapping.copy(marketParams = listOf(MarketParam(marketParamId + 1)))
        result = MappingDirectStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                categories
        )?.merge()
        Assertions.assertThat(result).isNull()

        mapping = originalMapping
        result = MappingDirectStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                mapOf()
        )?.merge()
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun testMappingMappingResult() {
        val shopParamName = "param1"
        val shopParamValue = "value1"
        val categoryValueName = "value1"
        val marketCategoryId = 1L
        val marketParamId = 2L
        val marketValueId = 3
        val mappingId = 10
        val mappingRank = 10

        val shopValues = mapOf(shopParamName to shopParamValue)

        val originalMapping = ParamMapping(
            id = mappingId,
            mappingType = ParamMappingType.FORCE_MAPPING,
            shopParams = listOf(ShopParam(name = shopParamName)),
            marketParams = listOf(MarketParam(marketParamId)),
            rank = mappingRank
        )
        var mapping = originalMapping

        val options = Int2ObjectOpenHashMap(
            mapOf(
                marketValueId to CategoryParameterValue(marketValueId, categoryValueName)
            )
        )

        val categories = mapOf(
            marketParamId to CategoryParameterInfo(
                parameterId = marketParamId,
                xslName = "",
                name = "",
                unitName = "",
                valueType = MboParameters.ValueType.ENUM,
                isImportant = false,
                isMultivalue = false,
                isService = false,
                isRequiredForModelCreation = false,
                isMandatoryForSignature = false,
                commonFilterIndex = 0,
                options = options,
                commentForPartner = null,
                commentForOperator = null,
                minValue = null,
                maxValue = null,
        ))

        //Нет рулов, и опция отсутствует в категории. Не должно создаться новых значений
        var result = MappingMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                mapOf("param1" to "v1"),
                categories
        )?.merge()

        Assertions.assertThat(result).isEmpty()

        //Нет рулов, и опция присутствует в категории. Не должно создаться новых значений
        result = MappingMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                mapOf("param1" to categoryValueName),
                categories
        )?.merge()

        Assertions.assertThat(result).isEmpty()

        //Есть рул, должно создаться соответствующее значение с указанным optionId
        var paramMappingRule = ParamMappingRule(
                id = 10,
                shopValues = mapOf("param1" to categoryValueName),
                marketValues = mapOf(marketParamId to setOf(OptionValue(marketValueId.toInt(), categoryValueName))))

        result = MappingMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf(paramMappingRule)),
                mapOf("param1" to categoryValueName),
                categories
        )?.merge()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)).isNotNull
        Assertions.assertThat(result?.get(marketParamId)).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.parameterId).isEqualTo(marketParamId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.valueSource).isEqualTo(ValueSource.RULE)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.value?.javaClass).isEqualTo(OptionValue::class.java)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.hypothesis).isEqualTo(categoryValueName)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.optionId).isEqualTo(marketValueId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.ruleId).isEqualTo(10)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.rank).isEqualTo(mappingRank)

        mapping = originalMapping.copy(shopParams = listOf(ShopParam(name = shopParamName + "1")))
        result = MappingMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                categories
        )?.merge()
        Assertions.assertThat(result).isEmpty()

        mapping = originalMapping.copy(marketParams = listOf(MarketParam(marketParamId + 1)))
        result = MappingMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                categories
        )?.merge()
        Assertions.assertThat(result).isEmpty()

        mapping = originalMapping
        result = MappingMappingStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                mapOf()
        )?.merge()
        Assertions.assertThat(result).isEmpty()
    }

    @Test
    fun testEmptyPictures() {
        val pics = ParamMapping(
                mappingType = ParamMappingType.PICTURE,
                shopParams = listOf(ShopParam("pics", ",")),
        )
        val result = mappingApplyer.applyMappingToModel(
                ParamMappingWithRules(pics, listOf()),
                emptyMap(),
                categories[TEST_CATEGORY_ID]!!
        )?.merge()
        val picValues = result?.get(MboParameterConstants.PICTURE_PARAM_ID)

        Assertions.assertThat(picValues).hasSize(1)
        val stringValue = (picValues?.get(0)?.value as? MarketParamValue.StringValue)?.stringValue
        Assertions.assertThat(stringValue).isNotNull()
        Assertions.assertThat(stringValue).isEmpty()
    }

    @Test
    fun testMergePicturesCleansOld() {
        val model = nextShopModel()
                .copy(pictures = listOf(
                        Picture("url1", ValueSource.RULE, 123, true),
                        Picture("url2", ValueSource.RULE, 124, false)
                ))
        val result = RuleApplyResult(
                mapOf(
                        MboParameterConstants.PICTURE_PARAM_ID to listOf(
                                MarketParameterValue(
                                        parameterId = MboParameterConstants.PICTURE_PARAM_ID,
                                        valueSource = ValueSource.RULE,
                                        value = MarketParamValue.StringValue(""),
                                        ruleId = -123
                                )
                        ),
                )
        )
        val shopModel = ruleApplyResultMerger.merge(model, result)
        Assertions.assertThat(shopModel).isNotNull
        Assertions.assertThat(shopModel.pictures)
                .hasSize(0)
    }

    @Test
    fun testMergePicturesCorrectlyMerges() {
        val model = nextShopModel()
                .copy(pictures = listOf( // 100 - some old mapping, which we don't touch here
                        Picture("oldUrl1", ValueSource.RULE, 100, false),
                        Picture("oldUrl2", ValueSource.RULE, 100, false)
                ))

        val result = RuleApplyResult(
                mapOf(
                        MboParameterConstants.MAIN_PICTURE_PARAM_ID to listOf(
                                MarketParameterValue(
                                        parameterId = MboParameterConstants.MAIN_PICTURE_PARAM_ID,
                                        valueSource = ValueSource.RULE,
                                        value = MarketParamValue.StringValue("newUrl1"),
                                        ruleId = -123
                                ),
                        ),
                        MboParameterConstants.PICTURE_PARAM_ID to listOf(
                                MarketParameterValue(
                                        parameterId = MboParameterConstants.PICTURE_PARAM_ID,
                                        valueSource = ValueSource.RULE,
                                        value = MarketParamValue.StringValue("newUrl2"),
                                        ruleId = -124
                                ),
                        ),
                )
        )

        val shopModel = ruleApplyResultMerger.merge(model, result)
        Assertions.assertThat(shopModel).isNotNull
        Assertions.assertThat(shopModel.pictures)
                .hasSize(2)
                .extracting<String, RuntimeException>(Picture::url)
                .containsExactly("newUrl1", "newUrl2")
        val nextApply = ruleApplyResultMerger.merge(shopModel, result)
        Assertions.assertThat(nextApply.pictures)
                .hasSize(2)
                .extracting<String, RuntimeException>(Picture::url)
                .containsExactly("newUrl1", "newUrl2")

        val resultEmpty = RuleApplyResult(
                mapOf(
                        MboParameterConstants.MAIN_PICTURE_PARAM_ID to listOf(
                                MarketParameterValue(
                                        parameterId = MboParameterConstants.MAIN_PICTURE_PARAM_ID,
                                        valueSource = ValueSource.RULE,
                                        value = MarketParamValue.StringValue(""),
                                        ruleId = -123
                                ),
                        ),
                        MboParameterConstants.PICTURE_PARAM_ID to listOf(
                                MarketParameterValue(
                                        parameterId = MboParameterConstants.PICTURE_PARAM_ID,
                                        valueSource = ValueSource.RULE,
                                        value = MarketParamValue.StringValue(""),
                                        ruleId = -124
                                ),
                        ),
                )
        )

        val nextApplyWithoutPictures = ruleApplyResultMerger.merge(shopModel, resultEmpty)
        Assertions.assertThat(nextApplyWithoutPictures).isNotNull
        Assertions.assertThat(nextApplyWithoutPictures.pictures).hasSize(0)
    }

    @Test
    fun testDirectApplyToEnum() {
        val shopParamName = "param1"
        val shopParamValue = "value1"
        val categoryValueName = "value1"
        val marketCategoryId = 1L
        val marketParamId = 2L
        val marketValueId = 3
        val mappingId = 10
        val mappingRank = 10

        val shopValues = mapOf(shopParamName to shopParamValue)

        val originalMapping = ParamMapping(
                id = mappingId,
                mappingType = ParamMappingType.FORCE_MAPPING,
                shopParams = listOf(ShopParam(name = shopParamName)),
                marketParams = listOf(MarketParam(marketParamId)),
                rank = mappingRank
        )
        var mapping = originalMapping

        val options = Int2ObjectOpenHashMap(
            mapOf(
                marketValueId to CategoryParameterValue(marketValueId, categoryValueName)
            )
        )

        val categories = mapOf(marketParamId to CategoryParameterInfo(
                parameterId = marketParamId,
                xslName = "",
                name = "",
                unitName = "",
                valueType = MboParameters.ValueType.ENUM,
                isImportant = false,
                isMultivalue = false,
                isService = false,
                isRequiredForModelCreation = false,
                isMandatoryForSignature = false,
                commonFilterIndex = 0,
                options = options,
                commentForPartner = null,
                commentForOperator = null,
                minValue = null,
                maxValue = null,
        ))

        //Нет рулов, и опция отсутствует в категории. Значений появиться не должно
        var result = MappingDirectStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                mapOf("param1" to "v1"),
                categories
        )?.merge()

        Assertions.assertThat(result?.get(marketParamId)).isEmpty()

        //Нет рулов, и опция присутствует в категории. Должно создаться значение с указанным optionId
        result = MappingDirectStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                mapOf("param1" to categoryValueName),
                categories
        )?.merge()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)).isNotNull
        Assertions.assertThat(result?.get(marketParamId)).isNotEmpty
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.parameterId).isEqualTo(marketParamId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.valueSource).isEqualTo(ValueSource.RULE)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.value?.javaClass).isEqualTo(OptionValue::class.java)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.hypothesis).isEqualTo(categoryValueName)
        Assertions.assertThat((result?.get(marketParamId)?.get(0)?.value as? OptionValue)?.optionId).isEqualTo(marketValueId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.ruleId).isEqualTo(-mappingId)
        Assertions.assertThat(result?.get(marketParamId)?.get(0)?.rank).isEqualTo(mappingRank)

        mapping = originalMapping.copy(marketParams = listOf(MarketParam(marketParamId + 1)))
        result = MappingDirectStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                categories
        )?.merge()
        Assertions.assertThat(result).isNull()

        mapping = originalMapping
        result = MappingDirectStrategy().apply(
                ParamMappingWithRules(mapping, listOf()),
                shopValues,
                mapOf()
        )?.merge()
        Assertions.assertThat(result).isNull()
    }

    private fun numericValueSet(numericValue: Double): Set<MarketParamValue> {
        return setOf(numericValue(numericValue))
    }

    private fun numericValue(numericValue: Double): MarketParamValue {
        return MarketParamValue.NumericValue(numericValue)
    }

    private fun optionValueSet(optionId: Int): Set<MarketParamValue> {
        return setOf(optionValue(optionId))
    }

    private fun optionValue(optionId: Int): MarketParamValue {
        return OptionValue(optionId, null)
    }

    private fun mpv(
            param: CategoryParameterInfo, valueSource: ValueSource, ruleId: Int, value: MarketParamValue
    ): MarketParameterValue {
        return MarketParameterValue(param.parameterId, valueSource, value, ruleId)
    }
}

fun ParamMappingRule.getFirstMarketValue() = this.marketValues.values.iterator().next().iterator().next()
