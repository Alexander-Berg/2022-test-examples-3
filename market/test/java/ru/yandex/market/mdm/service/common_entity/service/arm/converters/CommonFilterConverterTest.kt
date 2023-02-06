package ru.yandex.market.mdm.service.common_entity.service.arm.converters

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType.NUMERIC
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.MdmEntity
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmSearchCondition
import ru.yandex.market.mdm.service.common_entity.model.BinaryBooleanFunction
import ru.yandex.market.mdm.service.common_entity.model.CommonFilter
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.model.CommonPredicate
import ru.yandex.market.mdm.service.common_entity.model.PaginationCondition
import ru.yandex.market.mdm.service.common_entity.service.arm.flatMdmMetadataWithOneStructAttribute
import java.math.BigDecimal

class CommonFilterConverterTest {
    @Test
    fun `should convert CommonFilter to MdmSearchFilter`() {
        // given
        val baseEntity = mdmEntityType()
        val structAttributeId = 100L
        val innerAttributeId = 101L
        val innerEntity = mdmEntityType(
            attributes = listOf(
                mdmAttribute(
                    internalName = "inner",
                    dataType = NUMERIC,
                    mdmId = innerAttributeId
                )
            )
        )
        val metadata = flatMdmMetadataWithOneStructAttribute(
            baseEntity,
            innerEntity,
            structAttributeName = "outer",
            structAttributeId = structAttributeId
        )
        val pathToInnerAttribute = MdmPath.fromLongs(
            listOf(baseEntity.mdmId, structAttributeId, innerEntity.mdmId, innerAttributeId),
            MdmMetaType.MDM_ATTR
        )
        val commonPredicate = CommonPredicate(
            CommonParamValue.byLong(commonParamName = "outer||inner", value = 100L),
            BinaryBooleanFunction.EQ
        )
        val commonFilter = CommonFilter(
            listOf(listOf(commonPredicate)), PaginationCondition(
                pageSize = 101,
                offset = 501
            )
        )

        // when
        val mdmFilter = commonFilter.toMdmSearchFilter(metadata)

        // then
        mdmFilter.page.pageSize shouldBe 101
        mdmFilter.page.offset shouldBe 501
        mdmFilter.predicates.conjunctions[0].disjunctions[0].condition shouldBe MdmSearchCondition.EQ
        mdmFilter.predicates.conjunctions[0].disjunctions[0].value shouldNotBe null
        mdmFilter.predicates.conjunctions[0].disjunctions[0].value
            .getAttributeByPath(pathToInnerAttribute)[0].values[0].numeric shouldBe BigDecimal(100)
    }

    private fun MdmEntity.getAttributeByPath(path: MdmPath): List<MdmAttributeValues> {
        if (path.extractAttributeIds().size == 1) {
            return values[path.extractAttributeIds().first()]?.let { listOf(it) } ?: emptyList()
        }
        val attributesOnCurrentLevel = values[path.extractAttributeIds().first()]
        return if (attributesOnCurrentLevel == null || attributesOnCurrentLevel.values.isEmpty()) {
            emptyList()
        } else {
            attributesOnCurrentLevel.values.mapNotNull { it.struct }
                .flatMap { it.getAttributeByPath(path.nextLevel().nextLevel()) }
        }
    }
}
