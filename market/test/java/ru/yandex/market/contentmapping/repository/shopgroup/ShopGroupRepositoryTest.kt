package ru.yandex.market.contentmapping.repository.shopgroup

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import ru.yandex.market.contentmapping.dto.shopgroup.ShopGroup
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class ShopGroupRepositoryTest : BaseAppTestClass() {
    @Autowired
    lateinit var shopGroupTypeRepository: ShopGroupTypeRepository

    @Autowired
    lateinit var shopGroupRepository: ShopGroupRepository

    @Test
    fun `Uses foreign keys to type repo`() {
        val realGroup = shopGroupTypeRepository.insert(ShopGroup(
                name = "test group", description = "test desc"
        ))
        shouldNotThrow<DataIntegrityViolationException> {
            shopGroupRepository.addShopsToGroups(listOf(1L), listOf(realGroup.id))
        }
        shouldThrow<DataIntegrityViolationException> {
            shopGroupRepository.addShopsToGroups(listOf(2L), listOf(1_234_567L))
        }
    }

    @Test
    fun `Adds, gets and deletes groups of shops`() {
        val shop1 = 1L
        val shop2 = 2L
        val shop3 = 3L
        val shops = listOf(shop1, shop2, shop3)

        val group1 = shopGroupTypeRepository.insert(ShopGroup(name = "1")).id
        val group2 = shopGroupTypeRepository.insert(ShopGroup(name = "2")).id
        val group3 = shopGroupTypeRepository.insert(ShopGroup(name = "3")).id
        val groups = listOf(group1, group2, group3)

        shopGroupRepository.getGroupByShops(shops) shouldHaveSize 0

        shopGroupRepository.addShopsToGroups(shops, listOf(group1)) shouldBe 3

        val result1 = shopGroupRepository.getGroupByShops(shops)
        result1 shouldContainExactly mapOf(
                shop1 to listOf(group1),
                shop2 to listOf(group1),
                shop3 to listOf(group1),
        )

        shopGroupRepository.deleteShopsFromGroups(listOf(shop3), listOf(group1)) shouldBe 1
        shopGroupRepository.addShopsToGroups(listOf(shop1), listOf(group2, group3)) shouldBe 2

        val result2 = shopGroupRepository.getGroupByShops(shops)
        result2 shouldContainExactly mapOf(
                shop1 to groups,
                shop2 to listOf(group1)
        )

        shopGroupRepository.deleteShopsFromGroups(shops, groups) shouldBe 4

        shopGroupRepository.getGroupByShops(shops) shouldHaveSize 0
    }
}