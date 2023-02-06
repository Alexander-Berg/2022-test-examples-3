package ru.yandex.market.mbo.cms.core.permission.mock

import ru.yandex.market.mbo.cms.core.models.permission.ActionType
import ru.yandex.market.mbo.cms.core.models.permission.EntityType
import ru.yandex.market.mbo.cms.core.models.permission.Permission
import ru.yandex.market.mbo.cms.core.service.permission.PermissionService
import ru.yandex.market.mbo.cms.core.service.user.UserRole

class PermissionServiceMock : PermissionService {
    override fun savePermission(permission: Permission): Permission {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getPermission(permission: Permission): Permission? {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getPermission(id: Long): Permission? {
        throw NotImplementedError("Not yet implemented")
    }

    override fun deletePermission(id: Long): Long {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getPermissions(
        pageNumber: Int?,
        pageSize: Int?,
        projectName: String?,
        entityTypeId: EntityType?,
        action: ActionType?,
        entityId: String?,
        roleName: String?
    ): List<Permission> {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getPermissions(
        userRoles: List<Pair<String, String>>,
        entityTypeId: EntityType,
        entityId: String
    ): List<Permission> {

        val newEntityId = try {
            try {
                (entityId.slice(0..1)).toInt()
            } catch (e: Throwable) {
                entityId[0].digitToInt()
            }
        } catch (e: Throwable) {
            0
        }

        val action = ActionType.values()[newEntityId % 3]

        if (entityTypeId == EntityType.DOCUMENT && newEntityId < 12)
            return listOf()

        return listOf(
            Permission(
                1,
                entityTypeId,
                if (entityTypeId == EntityType.NAMESPACE_CONTENT) "*" else entityId,
                action,
                "project1",
                "role1",
                null,
                12345
            )
        )
    }

    override fun getPermissions(
        userRoles: List<Pair<String, String>>,
        entityTypeIds: Set<EntityType>
    ): List<Permission> {
        throw NotImplementedError("Not yet implemented")
    }

    override fun buildAllAllowPermissions(): Map<EntityType, Permission> {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getDocumentPermissionInfo(docId: Long): Map<EntityType, String> {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getMaxPermissions(userRoles: List<Pair<String, String>>): Map<EntityType, Permission?> {
        throw NotImplementedError("Not yet implemented")
    }

    override fun mapNewToOldPermissions(userRoles: List<Pair<String, String>>): List<UserRole> {
        throw NotImplementedError("Not yet implemented")
    }
}
