package ru.yandex.direct.ess.logicobjects.bsexport.adgroup

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import ru.yandex.direct.ess.logicobjects.bsexport.DebugInfo
import ru.yandex.direct.utils.JsonUtils

enum class TestEnum {
    THE_ONLY_KNOWN_VALUE
}

internal class AdGroupResourceTypeDeserializationTest {
    @Test
    fun unknownWithoutAnnotation() {
        val json = """"XXX""""
        assertThatThrownBy {
            JsonUtils.fromJson(json, TestEnum::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun knownValue1() {
        val json = """"COMMON_FIELDS""""
        assertThat(JsonUtils.fromJson(json, AdGroupResourceType::class.java))
            .isEqualTo(AdGroupResourceType.COMMON_FIELDS)
    }

    @Test
    fun knownValue2() {
        val json = """"UNKNOWN""""
        assertThat(JsonUtils.fromJson(json, AdGroupResourceType::class.java))
            .isEqualTo(AdGroupResourceType.UNKNOWN)
    }

    @Test
    fun unknownValue() {
        val json = """"XXX-XXX""""
        assertThat(JsonUtils.fromJson(json, AdGroupResourceType::class.java))
            .isEqualTo(AdGroupResourceType.UNKNOWN)
    }

    @Test
    fun complexObjectWithKnownValue() {
        val json = """{"resource_type":"COMMON_FIELDS","adgroup_id":22,"campaign_id":null,"debug_info":{}}"""
        val expected = BsExportAdGroupObject(AdGroupResourceType.COMMON_FIELDS, 22L, null, DebugInfo())
        assertThat(JsonUtils.fromJson(json, BsExportAdGroupObject::class.java)).isEqualTo(expected)
    }

    @Test
    fun complexObjectWithUnknownValue() {
        val json = """{"resource_type":"XXX","adgroup_id":22,"campaign_id":null,"debug_info":{}}"""
        val expected = BsExportAdGroupObject(AdGroupResourceType.UNKNOWN, 22L, null, DebugInfo())
        assertThat(JsonUtils.fromJson(json, BsExportAdGroupObject::class.java)).isEqualTo(expected)
    }

    @Test
    fun complexObjectWithNullValue() {
        val json = """{"resource_type":null,"adgroup_id":22,"campaign_id":null,"debug_info":{}}"""
        assertThatThrownBy {
            JsonUtils.fromJson(json, BsExportAdGroupObject::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
