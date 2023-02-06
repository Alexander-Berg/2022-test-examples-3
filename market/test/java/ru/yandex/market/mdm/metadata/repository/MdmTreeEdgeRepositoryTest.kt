package ru.yandex.market.mdm.metadata.repository

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.lib.model.mdm.MdmTreeEdge
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class MdmTreeEdgeRepositoryTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmTreeEdgeRepository: MdmTreeEdgeRepository

    @Test
    fun testFindByTreeId() {
        val expectedTreeEdge = MdmTreeEdge(childId = 5, parentId = 6, treeId = 123)
        mdmTreeEdgeRepository.insert(expectedTreeEdge)
        mdmTreeEdgeRepository.insert(MdmTreeEdge(childId = 5, parentId = 6, treeId = 124))

        val foundList = mdmTreeEdgeRepository.findByTreeId(123)
        assertSoftly {
            foundList shouldContainExactly listOf(expectedTreeEdge)
        }
    }

    @Test
    fun testFindByChildId() {
        val expectedTreeEdge = MdmTreeEdge(childId = 5, parentId = 6, treeId = 123)
        mdmTreeEdgeRepository.insert(expectedTreeEdge)
        mdmTreeEdgeRepository.insert(MdmTreeEdge(childId = 6, parentId = 6, treeId = 123))

        val foundList = mdmTreeEdgeRepository.findByChildId(5, 123)
        assertSoftly {
            foundList shouldContainExactly listOf(expectedTreeEdge)
        }
    }

    @Test
    fun testFindByParentId() {
        val expectedTreeEdge = MdmTreeEdge(childId = 5, parentId = 6, treeId = 123)
        mdmTreeEdgeRepository.insert(expectedTreeEdge)
        mdmTreeEdgeRepository.insert(MdmTreeEdge(childId = 6, parentId = 7, treeId = 123))

        val foundList = mdmTreeEdgeRepository.findByParentId(6, 123)
        assertSoftly {
            foundList shouldContainExactly listOf(expectedTreeEdge)
        }
    }
}
