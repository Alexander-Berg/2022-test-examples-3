package ru.yandex.market.mdm.metadata.repository

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import org.junit.Test
import ru.yandex.market.mdm.metadata.filters.MdmEntityTypeSearchFilter

class MdmEntityTypeRepositoryTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Test
    fun `find mdm_entity_types by id`() {
        val insertedEntityType = mdmEntityTypeRepository.insert(
            MdmEntityType(
            internalName = "internal_name",
            description = "description",
            ruTitle = "ru_title")
        )

        val entityType = mdmEntityTypeRepository.findLatestById(insertedEntityType.mdmId)!!

        entityType.let {
            assertSoftly {
                it shouldBe insertedEntityType
                it.mdmId shouldBeGreaterThan 0
                it.version.to shouldBe null
            }
        }
    }

    @Test
    fun `find mdm entity type by internal name`() {
        val instance1 = MdmEntityType(internalName = "name1", description = "desc1", ruTitle = "ru_title1")
        val instance2 = MdmEntityType(internalName = "name2", description = "desc2", ruTitle = "ru_title2")
        val inserted1 = mdmEntityTypeRepository.insert(instance1)
        val inserted2 = mdmEntityTypeRepository.insert(instance2)

        mdmEntityTypeRepository.retireLatestById(inserted1.mdmId)
        val foundList = mdmEntityTypeRepository
            .findBySearchFilter(MdmEntityTypeSearchFilter(internalNames = listOf("name2")))

        assertSoftly {
            foundList shouldHaveSize 1
            inserted2 shouldBe foundList[0]
        }
    }
}
