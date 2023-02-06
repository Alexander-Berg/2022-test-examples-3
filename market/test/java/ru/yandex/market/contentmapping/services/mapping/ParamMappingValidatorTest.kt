package ru.yandex.market.contentmapping.services.mapping

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.mockito.Mockito.*
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.StringValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.services.category.info.CategoryParameterInfoService

class ParamMappingValidatorTest {
    @Test
    fun `Prevents editing of assortment mappings`() {
        val validator = ParamMappingValidator(mock(CategoryParameterInfoService::class.java))

        val existing = ParamMapping(
                id = 0,
                shopId = 123,
                mappingType = ParamMappingType.MAPPING,
                rank = -1
        )
        val new = existing.copy(isHypothesis = true)

        val result = validator.validate(existing, new, emptyList())
        existing shouldNotBe new
        result.isValid shouldBe false
        result.messages.size shouldBe 1
    }

    @Test
    fun `Prevents creation of mappings with rank less than 0`() {
        val validator = ParamMappingValidator(mock(CategoryParameterInfoService::class.java))

        val new = ParamMapping(
                id = 0,
                shopId = 123,
                mappingType = ParamMappingType.MAPPING,
                rank = -1
        )

        val result = validator.validate(null, new, emptyList())
        result.isValid shouldBe false
        result.messages.size shouldBe 1
    }

    @Test
    fun `Requires shopId to be present`() {
        val validator = ParamMappingValidator(mock(CategoryParameterInfoService::class.java))

        val new = ParamMapping(
                id = 0,
                mappingType = ParamMappingType.MAPPING,
                rank = 0
        )

        val result = validator.validate(null, new, emptyList())
        result.isValid shouldBe false
        result.messages.size shouldBe 1
    }

    @Test
    fun `Prevents rules that are not multi-value to have multiple market values`() {
        val categoryParameterInfoService = mock(CategoryParameterInfoService::class.java)
        doReturn(true).`when`(categoryParameterInfoService).isMultivalue(eq(333L))
        doReturn(false).`when`(categoryParameterInfoService).isMultivalue(eq(666L))
        val validator = ParamMappingValidator(categoryParameterInfoService)

        val new = ParamMapping(
                id = 333,
                shopId = 123,
                mappingType = ParamMappingType.MAPPING,
                rank = 0
        )

        val rule = ParamMappingRule(
                id = 123,
                paramMappingId = 200,
                marketValues = mapOf(
                        333L to setOf(StringValue("Is MV"), StringValue("Second")),
                        666L to setOf(StringValue("Non MV"), StringValue("Second"))
                )
        )

        val result = validator.validate(null, new, listOf(rule))
        result.isValid shouldBe false
        result.messages.size shouldBe 1
    }

    @Test
    fun `Valid mapping and rules do pass`() {
        val validator = ParamMappingValidator(mock(CategoryParameterInfoService::class.java))

        val existing = ParamMapping(
                id = 333,
                shopId = 123,
                mappingType = ParamMappingType.MAPPING,
                rank = 0
        )
        val new = existing.copy(
                shopParams = listOf(
                        ShopParam("Test", ".")
                )
        )

        val rule = ParamMappingRule(
                id = 123,
                paramMappingId = 200,
                marketValues = mapOf(
                        333L to setOf(StringValue("Test")),
                ),
                shopValues = mapOf(
                        "TestKey" to "TestVal"
                )
        )

        val result = validator.validate(existing, new, listOf(rule))

        result.isValid shouldBe true
        result.messages.isEmpty() shouldBe true
    }
}
