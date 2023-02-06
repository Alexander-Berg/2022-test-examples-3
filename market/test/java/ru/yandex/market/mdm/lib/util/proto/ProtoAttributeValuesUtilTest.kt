package ru.yandex.market.mdm.lib.util.proto

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.lib.model.mdm.ProtoAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.ProtoAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.ProtoEntity

class ProtoAttributeValuesUtilTest {

    @Test
    fun `should create search attribute from base attribute values`() {
        // given
        val lowLevelAttributeValues = ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(555L)
            .addValues(ProtoAttributeValue.newBuilder().setInt64(100L))
            .build()
        val lowLevelEntity = ProtoEntity.newBuilder()
            .putMdmAttributeValues(lowLevelAttributeValues.mdmAttributeId, lowLevelAttributeValues)
            .build()

        val topLevelAttributeValues = ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(777L)
            .addValues(ProtoAttributeValue.newBuilder().setStruct(lowLevelEntity))
            .build()
        val topLevelEntity = ProtoEntity.newBuilder()
            .putMdmAttributeValues(topLevelAttributeValues.mdmAttributeId, topLevelAttributeValues)
            .build()

        // when
        val searchAttribute = topLevelEntity.toSearchAttributeValues()

        // then
        searchAttribute.mdmAttributeId shouldBe 555L
        searchAttribute.mdmAttributePathList shouldContainInOrder listOf(777L, 555L)
        searchAttribute.valuesList shouldContainInOrder lowLevelAttributeValues.valuesList
    }
}
