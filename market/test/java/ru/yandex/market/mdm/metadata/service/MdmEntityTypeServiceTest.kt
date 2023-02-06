package ru.yandex.market.mdm.metadata.service

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment
import ru.yandex.market.mdm.metadata.model.StructuredMdmEntityType
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.TestDataUtils.entityTypeWithOneStruct

class MdmEntityTypeServiceTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var mdmEntityTypeService: MdmEntityTypeService

    @Test
    fun `should return structured entity`() {
        // given
        val entityTypeWithOneStruct = entityTypeWithOneStruct(
            innerSimpleAttributes = listOf(mdmAttribute()),
            outerSimpleAttributes = listOf(mdmAttribute()),
        )
        val outerMdmEntityType = entityTypeWithOneStruct.outerMdmEntityType
        val innerMdmEntityType = entityTypeWithOneStruct.innerMdmEntityType
        mdmEntityTypeRepository.insertOrUpdateBatch(listOf(innerMdmEntityType, outerMdmEntityType))
        mdmAttributeRepository.insertOrUpdateBatch(
            innerMdmEntityType.attributes + outerMdmEntityType.attributes
        )

        val expectedStructuredMdmEntityType = StructuredMdmEntityType(
            baseEntityType = outerMdmEntityType,
            relatedEntityTypes = listOf(outerMdmEntityType, innerMdmEntityType).associateBy { it.mdmId },
            startingPath = listOf()
        )

        // when
        val result =
            mdmEntityTypeService.getStructuredMdmEntityByPath(MdmPath(listOf(MdmPathSegment.entity(outerMdmEntityType.mdmId))))

        // then
        result shouldBe expectedStructuredMdmEntityType
    }
}
