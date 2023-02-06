package ru.yandex.market.contentmapping.repository

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.User
import ru.yandex.market.contentmapping.model.Role
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class UserRepositoryImplTest : BaseAppTestClass() {
    private val userId1 = 1L
    private val userId2 = 2L
    private val shopId1 = 1L
    private val shopId2 = 2L
    private val shopId3 = 3L

    @Autowired
    lateinit var userRepository: UserRepositoryImpl

    @Autowired
    lateinit var shopRepository: ShopRepository

    @Before
    fun setup() {
        clear()
        shopRepository.insert(Shop(shopId1, shopId1.toString()))
        shopRepository.insert(Shop(shopId2, shopId2.toString()))
        shopRepository.insert(Shop(shopId3, shopId3.toString()))
        userRepository.insert(User(userId1, Role.MANAGER))
        userRepository.insert(User(userId2, Role.MANAGER))
    }

    @After
    fun clear() {
        userRepository.updateUserShops(userId1, emptyList())
        userRepository.updateUserShops(userId2, emptyList())
        shopRepository.deleteAll()
    }

    @Test
    fun `test update user shops`() {
        var userPermissions1 = userRepository.getUserPermissions(userId1)
        userPermissions1.id shouldBe userId1
        userPermissions1.role shouldBe Role.MANAGER
        userPermissions1.shopIds shouldHaveSize 0

        var userPermissions2 = userRepository.getUserPermissions(userId2)
        userPermissions2.id shouldBe userId2
        userPermissions2.role shouldBe Role.MANAGER
        userPermissions2.shopIds shouldHaveSize 0

        userRepository.updateUserShops(userPermissions1.id, listOf(shopId1, shopId2))

        userPermissions1 = userRepository.getUserPermissions(userId1)
        userPermissions1.shopIds shouldHaveSize 2
        userPermissions1.shopIds shouldContain shopId1
        userPermissions1.shopIds shouldContain shopId2

        userPermissions2 = userRepository.getUserPermissions(userId2)
        userPermissions2.shopIds shouldHaveSize 0

        userRepository.updateUserShops(userPermissions1.id, listOf(shopId2, shopId3))

        userPermissions1 = userRepository.getUserPermissions(userId1)
        userPermissions1.shopIds shouldHaveSize 2
        userPermissions1.shopIds shouldContain shopId2
        userPermissions1.shopIds shouldContain shopId3

        userPermissions2 = userRepository.getUserPermissions(userId2)
        userPermissions2.shopIds shouldHaveSize 0
    }
}
