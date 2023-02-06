package ru.yandex.market.contentmapping.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.rules.RulesWithVersion
import ru.yandex.market.contentmapping.testdata.ParamMappingFactory
import ru.yandex.market.contentmapping.testdata.TestDataUtils
import ru.yandex.market.mbo.export.MboParameters

class CompressedSerializerTest {
    lateinit var paramMappingRulesWithVersion: RulesWithVersion
    lateinit var paramMappingsWithRules: ParamMappingFactory
    lateinit var colorMapping: ParamMappingFactory

    private val weight = TestDataUtils.nextParameter("Вес").copy(valueType = MboParameters.ValueType.NUMERIC)
    private val colorVendor = TestDataUtils.nextParameterEnum("Вендорский цвет", 200, 210)

    private val serializer: CompressedSerializer<RulesWithVersion> = CompressedSerializer(RulesWithVersion::class.java)

    @Before
    fun setUp() {
        paramMappingsWithRules = ParamMappingFactory.map(weight, "weight")
        paramMappingsWithRules.rule("50", numericValueSet(50.0))
        paramMappingsWithRules.rule("100", numericValueSet(100.0))

        colorMapping = ParamMappingFactory.map(colorVendor, "color_vendor", ",")
        colorMapping.rule("red", optionValueSet(100)) { r: ParamMappingRule -> r.copy(isHypothesis = true) }
        colorMapping.rule("yellow", optionValueSet(100))
        paramMappingRulesWithVersion =
            RulesWithVersion(listOf(paramMappingsWithRules.mappingWithRules(), colorMapping.mappingWithRules()), 1L, 5L)
    }

    @Test
    fun `can serialize`() {
        val serialized = serializer.serialize(paramMappingRulesWithVersion)
        assertThat(serialized).isNotNull
    }

    @Test
    fun `can deserialize`() {
        val serialized = serializer.serialize(paramMappingRulesWithVersion)
        val deserialized = serializer.deserialize(serialized)
        assertThat(deserialized).isNotNull
    }

    @Test
    fun `deserializing is correct`() {
        val serialized = serializer.serialize(paramMappingRulesWithVersion)
        val deserialized = serializer.deserialize(serialized)
        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.rules.size).isEqualTo(paramMappingRulesWithVersion.rules.size)
        Assertions.assertIterableEquals(deserialized.rules, paramMappingRulesWithVersion.rules)
        assertThat(deserialized.shopId).isEqualTo(5L)
        assertThat(deserialized.version).isEqualTo(1L)
        assertThat(deserialized.rules[0].paramMapping.id).isEqualTo(paramMappingRulesWithVersion.rules[0].paramMapping.id)
        assertThat(deserialized.rules[1].paramMapping.id).isEqualTo(paramMappingRulesWithVersion.rules[1].paramMapping.id)
        assertThat(deserialized.rules[0].paramMapping.mappingType).isEqualTo(paramMappingRulesWithVersion.rules[0].paramMapping.mappingType)
        assertThat(deserialized.rules[1].paramMapping.mappingType).isEqualTo(paramMappingRulesWithVersion.rules[1].paramMapping.mappingType)
        assertThat(deserialized.rules[0].rules.size).isEqualTo(2)
        assertThat(deserialized.rules[1].rules.size).isEqualTo(2)
        assertThat(deserialized.rules[0].rules[0]).isEqualToIgnoringGivenFields(paramMappingRulesWithVersion.rules[0].rules[0], "key\$delegate")
        assertThat(deserialized.rules[0].rules[1]).isEqualToIgnoringGivenFields(paramMappingRulesWithVersion.rules[0].rules[1], "key\$delegate")
        assertThat(deserialized.rules[1].rules[0]).isEqualToIgnoringGivenFields(paramMappingRulesWithVersion.rules[1].rules[0], "key\$delegate")
        assertThat(deserialized.rules[1].rules[1]).isEqualToIgnoringGivenFields(paramMappingRulesWithVersion.rules[1].rules[1], "key\$delegate")
        Assertions.assertIterableEquals(
            deserialized.rules[0].rules[0].shopValues.keys,
            paramMappingRulesWithVersion.rules[0].rules[0].shopValues.keys
        )
        Assertions.assertIterableEquals(
            deserialized.rules[0].rules[0].shopValues.values,
            paramMappingRulesWithVersion.rules[0].rules[0].shopValues.values
        )
        Assertions.assertIterableEquals(
            deserialized.rules[1].rules[0].shopValues.keys,
            paramMappingRulesWithVersion.rules[1].rules[0].shopValues.keys
        )
        Assertions.assertIterableEquals(
            deserialized.rules[1].rules[0].shopValues.values,
            paramMappingRulesWithVersion.rules[1].rules[0].shopValues.values
        )
    }

    private fun optionValueSet(optionId: Int): Set<MarketParamValue> {
        return setOf(optionValue(optionId))
    }

    private fun optionValue(optionId: Int): MarketParamValue {
        return MarketParamValue.OptionValue(optionId, null)
    }

    private fun numericValueSet(numericValue: Double): Set<MarketParamValue> {
        return setOf(numericValue(numericValue))
    }

    private fun numericValue(numericValue: Double): MarketParamValue {
        return MarketParamValue.NumericValue(numericValue)
    }
}
