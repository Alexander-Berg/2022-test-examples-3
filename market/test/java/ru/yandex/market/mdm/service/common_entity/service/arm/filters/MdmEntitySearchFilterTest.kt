package ru.yandex.market.mdm.service.common_entity.service.arm.filters

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonFilter
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.model.MdmProtocol
import ru.yandex.market.mdm.service.common_entity.service.arm.flatMdmMetadataWithOneStructAttribute

class MdmEntitySearchFilterTest {

    @Test
    fun `should create filter for ids search from common Entity`() {
        // given
        val metadata = flatMdmMetadataWithOneStructAttribute()
        val commonEntity = CommonEntity.Builder()
            .commonEntityType(metadata.provideCommonEntityType())
            .commonFilter(CommonFilter()) // default filter
            .commonParamValues(listOf(
                CommonParamValue.byLong(MdmProtocol.REQUEST_ENTITY_ID.paramName(), 123),
                CommonParamValue.byLong(MdmProtocol.REQUEST_ENTITY_TYPE_ID.paramName(), 1234),
            ))
            .build()

        // when
        val filter = MdmEntitySearchFilter(commonEntity, metadata)

        // then
        filter.forByIdsSearch() shouldBe true
        filter.ids shouldContainExactly listOf(123)
    }
}
