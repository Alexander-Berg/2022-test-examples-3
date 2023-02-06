package ru.yandex.market.contentmapping.dto.mapping

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.market.contentmapping.utils.JsonUtils

class MarketParamValueTest {
    @Test
    fun `it serializes and deserializes the same`() {
        val mapper = JsonUtils.commonObjectMapper()
        listOf(
                MarketParamValue.StringValue("something"),
                MarketParamValue.NumericValue(10.0),
                MarketParamValue.BooleanValue(true),
                MarketParamValue.BooleanValue(true, 123),
                MarketParamValue.OptionValue(123),
                MarketParamValue.OptionValue(123, "hypothesis"),
                MarketParamValue.HypothesisValue( "hypothesis"),
        ).forEach { value ->
            val json = mapper.writeValueAsString(value)
            val recreated = mapper.readValue(json, MarketParamValue::class.java)
            Assertions.assertThat(recreated).isEqualTo(value)
        }
    }
}
