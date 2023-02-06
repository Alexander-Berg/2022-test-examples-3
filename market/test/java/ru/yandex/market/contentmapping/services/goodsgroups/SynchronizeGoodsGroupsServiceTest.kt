package ru.yandex.market.contentmapping.services.goodsgroups

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.repository.GoodsGroupRepository
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.http.MdmGoodsGroup
import ru.yandex.market.mbo.http.MdmGoodsGroupService

class SynchronizeGoodsGroupsServiceTest : BaseAppTestClass() {
    @Autowired
    lateinit var synchronizeGoodsGroupsService: SynchronizeGoodsGroupsService

    @Autowired
    lateinit var goodsGroupRepository: GoodsGroupRepository

    @Autowired
    lateinit var mdmGoodsGroupService: MdmGoodsGroupService

    @Test
    fun `test it adds new items`() {
        setupMdm(mdmGroup(42L, "Test", listOf(1L, 2L)))
        synchronizeGoodsGroupsService.synchronize()

        val groups = goodsGroupRepository.findAll()
        groups shouldHaveSize 1
        groups[0].asClue {
            it.id shouldBe 42L
            it.name shouldBe "Test"
            it.categoryIds shouldBe listOf(1L, 2L)
        }
    }

    @Test
    fun `test it removes items`() {
        setupMdm(mdmGroup(42L, "Test", listOf(1L, 2L)))
        synchronizeGoodsGroupsService.synchronize()

        goodsGroupRepository.findAll() shouldHaveSize 1

        setupMdm()
        synchronizeGoodsGroupsService.synchronize()

        goodsGroupRepository.findAll() shouldHaveSize 0
    }

    @Test
    fun `test it updates items`() {
        setupMdm(mdmGroup(42L, "Test", listOf(1L, 2L)))
        synchronizeGoodsGroupsService.synchronize()

        goodsGroupRepository.findAll() shouldHaveSize 1

        setupMdm(mdmGroup(42L, "Test updated", listOf(1L, 2L, 3L)))
        synchronizeGoodsGroupsService.synchronize()

        val groups = goodsGroupRepository.findAll()
        groups shouldHaveSize 1
        groups[0].asClue {
            it.id shouldBe 42L
            it.name shouldBe "Test updated"
            it.categoryIds shouldBe listOf(1L, 2L, 3L)
        }
    }

    @Test
    fun `test it doesn't update items if nothing changed`() {
        setupMdm(mdmGroup(42L, "Test", listOf(1L, 2L)))
        synchronizeGoodsGroupsService.synchronize()

        goodsGroupRepository.findAll() shouldHaveSize 1

        Mockito.reset(goodsGroupRepository)

        synchronizeGoodsGroupsService.synchronize()
        Mockito.verify(goodsGroupRepository, Mockito.never()).insertBatch(Mockito.anyList())
        Mockito.verify(goodsGroupRepository, Mockito.never()).updateBatch(Mockito.anyList())
        Mockito.verify(goodsGroupRepository, Mockito.never()).delete(Mockito.anyList())
    }

    private fun setupMdm(vararg groups: MdmGoodsGroup.GoodsGroup.Builder) {
        Mockito.`when`(mdmGoodsGroupService.getAllGoodsGroups(Mockito.any()))
                .thenReturn(MdmGoodsGroup.GetAllGoodsGroupsResponse.newBuilder()
                        .also { groups.forEach(it::addGoodsGroup) }
                        .build())
    }

    private fun mdmGroup(groupId: Long, name: String, categoryIds: List<Long>) = MdmGoodsGroup.GoodsGroup.newBuilder().apply {
        id = groupId
        groupName = name
        addAllCategoryId(categoryIds)
    }

}
