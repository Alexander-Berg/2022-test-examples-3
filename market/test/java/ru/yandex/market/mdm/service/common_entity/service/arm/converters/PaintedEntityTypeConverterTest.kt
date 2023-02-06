package ru.yandex.market.mdm.service.common_entity.service.arm.converters

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.MetadataPainter.Companion.SIMPLE_METADATA_PAINTER
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.FlatMdmMetadata
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.converter.EntityTypeConverter
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.converter.PaintedEntityTypeConverter
import ru.yandex.market.mdm.service.common_entity.util.CommonEntityUtil

class PaintedEntityTypeConverterTest {

    private val entityTypeConverter: EntityTypeConverter = PaintedEntityTypeConverter(SIMPLE_METADATA_PAINTER)

    @Test
    fun `singleMetadata should return single metadata`() {
        // given
        val entityType = mdmEntityType(attributes = listOf(mdmAttribute()))
        val metadata = FlatMdmMetadata(entityType, listOf(entityType), listOf(), entityTypeConverter)

        // when
        val response = entityTypeConverter.toCommonEntityType(metadata)

        // then
        response shouldNotBe null
        response.commonEntityTypeEnum = CommonEntityTypeEnum.MDM_ENTITY
        response.commonParams!! shouldContainAll CommonEntityUtil.basicCommonParams(metadata)
        response.commonParams!! shouldContain entityType.attributes.first().toCommonParam()
    }
}
