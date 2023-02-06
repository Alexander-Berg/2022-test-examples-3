package ru.yandex.market.contentmapping.services.security

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.controllers.exceptions.Forbidden
import ru.yandex.market.contentmapping.dao.UserRepositoryMock.Companion.ADMIN_USER_WITH_NO_SHOPS
import ru.yandex.market.contentmapping.dao.UserRepositoryMock.Companion.MANAGER_USER
import ru.yandex.market.contentmapping.dao.UserRepositoryMock.Companion.NOT_ATTACHED_SHOP
import ru.yandex.market.contentmapping.dao.UserRepositoryMock.Companion.NOT_EXISTED_USER_ID
import ru.yandex.market.contentmapping.dao.UserRepositoryMock.Companion.OPERATOR_USER
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class AccessControlServiceImplTest : BaseAppTestClass() {
    @Autowired
    private lateinit var accessControlService: AccessControlServiceImpl

    @Test
    fun `userCanRead returns true for specified roles`() {
        accessControlService.userCanRead(ADMIN_USER_WITH_NO_SHOPS.id) shouldBe true
        accessControlService.userCanRead(OPERATOR_USER.id) shouldBe true
        accessControlService.userCanRead(MANAGER_USER.id) shouldBe true
    }

    @Test
    fun `userCanRead returns false for everyone else`() {
        accessControlService.userCanRead(NOT_EXISTED_USER_ID) shouldBe false
    }

    @Test
    fun `verifyShopAccess always passes for ADMIN`() {
        accessControlService.verifyShopReadAccess(ADMIN_USER_WITH_NO_SHOPS.id, NOT_ATTACHED_SHOP)
    }

    @Test
    fun `verifyShopAccess passes for OPERATOR with attached shop`() {
        accessControlService.verifyShopReadAccess(OPERATOR_USER.id, OPERATOR_USER.shopIds.first())
    }

    @Test(expected = Forbidden::class)
    fun `verifyShopAccess forbids OPERATOR access to unattached shop`() {
        accessControlService.verifyShopReadAccess(OPERATOR_USER.id, NOT_ATTACHED_SHOP)
    }

    @Test(expected = Forbidden::class)
    fun `verifyShopAccess forbids MANAGER access to unattached shop`() {
        accessControlService.verifyShopReadAccess(MANAGER_USER.id, NOT_ATTACHED_SHOP)
    }

    @Test
    fun `verifyUserIsOperator counts ADMIN as operator`() {
        accessControlService.verifyUserIsOperator(ADMIN_USER_WITH_NO_SHOPS.id, NOT_ATTACHED_SHOP)
    }

    @Test(expected = Forbidden::class)
    fun `verifyUserIsOperator forbids OPERATOR access to unattached shop`() {
        accessControlService.verifyUserIsOperator(OPERATOR_USER.id, NOT_ATTACHED_SHOP)
    }

    @Test
    fun `verifyUserIsOperator passes for OPERATOR with attached shop`() {
        accessControlService.verifyUserIsOperator(OPERATOR_USER.id, OPERATOR_USER.shopIds.first())
    }
}
