package ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmRelationTypeSearchFilter

class ProtoConvertorsTest {

    @Test
    fun `should convert MdmRelationTypeSearchFilter to proto with ids`() {
        // given
        val filter = MdmRelationTypeSearchFilter(ids = listOf(15L))

        // when
        val proto = filter.toProto()

        // then
        proto.byIds.mdmIdsList shouldContainExactly listOf(15L)
    }

    @Test
    fun `should convert MdmRelationTypeSearchFilter to proto with related ids`() {
        // given
        val filter = MdmRelationTypeSearchFilter(relatedEntityTypeIds = listOf(25))

        // when
        val proto = filter.toProto()

        // then
        proto.byRelatedIds.mdmIdsList shouldContainExactly listOf(25L)
    }
}
