package ru.yandex.market.contentmapping.dao

import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.User
import ru.yandex.market.contentmapping.kotlin.typealiases.BusinessId
import ru.yandex.market.contentmapping.kotlin.typealiases.ShopId
import ru.yandex.market.contentmapping.model.Role
import ru.yandex.market.contentmapping.model.UserPermissions
import ru.yandex.market.contentmapping.repository.UserRepository

class UserRepositoryMock : UserRepository {
    override fun getUserPermissions(userId: Long): UserPermissions {
        return UserPermissions(userId, getUserRole(userId), findUserShops(userId))
    }

    override fun getUserRole(userId: Long): Role {
        return when (userId) {
            ADMIN_USER_WITH_NO_SHOPS.id -> {
                Role.ADMIN
            }
            OPERATOR_USER.id -> {
                Role.OPERATOR
            }
            else -> {
                Role.NONE
            }
        }
    }

    override fun updateUserShops(userId: Long, shopIds: List<Long>) {
    }

    override fun findUserShops(userId: Long): List<Long> {
        return when (userId) {
            ADMIN_USER_WITH_NO_SHOPS.id -> {
                ADMIN_SHOPS
            }
            OPERATOR_USER.id -> {
                OPERATOR_SHOPS
            }
            else -> {
                emptyList()
            }
        }
    }

    override fun findAllUserPermissions(): List<UserPermissions> {
        TODO("Not yet implemented")
    }

    override fun update(user: User): User {
        TODO("Not yet implemented")
    }

    override fun insert(user: User): User {
        TODO("Not yet implemented")
    }

    override fun delete(users: List<Long>) {
        TODO("Not yet implemented")
    }

    override fun updateShopId(from: ShopId, to: BusinessId) {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmField
        val ADMIN_SHOPS = emptyList<Long>()
        @JvmField
        val ADMIN_USER_WITH_NO_SHOPS = UserPermissions(1L, Role.ADMIN, ADMIN_SHOPS)
        @JvmField
        val OPERATOR_SHOPS = listOf(1L, 2L, 3L)
        @JvmField
        val OPERATOR_USER = UserPermissions(2L, Role.OPERATOR, OPERATOR_SHOPS)
        @JvmField
        val MANAGER_MBI_SHOPS = listOf(
                Shop(id = 7L, name = "Fifth", businessId = 70L),
                Shop(id = 8L, name = "Sixth", businessId = 80L)
        )
        @JvmField
        val MANAGER_USER = UserPermissions(3L, Role.MANAGER, emptyList())
        const val NOT_EXISTED_USER_ID = 4L
        const val NOT_ATTACHED_SHOP = 5L
    }
}
