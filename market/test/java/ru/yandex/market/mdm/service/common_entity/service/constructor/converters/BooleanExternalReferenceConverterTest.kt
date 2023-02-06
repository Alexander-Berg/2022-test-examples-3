package ru.yandex.market.mdm.service.common_entity.service.constructor.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmExternalReferenceForBoolean
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType

class BooleanExternalReferenceConverterTest {

    @Test
    fun `should convert to common entity and back`() {
        // given
        val converter = BooleanExternalReferenceConverter()
        val initialReference = mdmExternalReferenceForBoolean()

        // when
        val commonEntity = converter.mdm2common(
            initialReference,
            CommonEntityType(CommonEntityTypeEnum.MDM_BOOLEAN_EXTERNAL_REFERENCE)
        )
        val reconvertedReference = converter.common2mdm(commonEntity)

        // then
        reconvertedReference shouldBe initialReference
    }
}
