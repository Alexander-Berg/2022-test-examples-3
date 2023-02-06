package ru.yandex.market.mbo.cms.core.service

import ru.yandex.market.mbo.cms.core.helpers.PermissionCheckHelper
import ru.yandex.market.mbo.cms.core.models.Page
import ru.yandex.market.mbo.cms.core.models.permission.ActionType
import ru.yandex.market.mbo.cms.core.models.permission.EntityType
import ru.yandex.market.mbo.cms.core.models.permission.MultiplePermission
import ru.yandex.market.mbo.cms.core.models.permission.Permission
import ru.yandex.market.mbo.cms.core.service.user.UserRole

class PermissionCheckHelperMock : PermissionCheckHelper {
    override fun checkPermissionsForDocument(
        serviceTicket: String?,
        userId: Long?,
        docId: Long,
        actionType: ActionType
    ) {
    }

    override fun checkPermissionsForAllDocuments(serviceTicket: String?, userId: Long?, actionType: ActionType) {
        TODO("Not yet implemented")
    }

    override fun checkPermissionsForDocument(
        serviceTicket: String?,
        userId: Long?,
        permissionMap: Map<EntityType, String>,
        actionType: ActionType
    ) {
    }

    override fun checkPermissionsForDocument(
        serviceTicket: String?,
        userId: Long?,
        page: Page,
        actionType: ActionType
    ) {
    }

    override fun checkPermissionsForDocument(
        serviceTicket: String?,
        userId: Long?,
        docId: Long?,
        docType: String?,
        namespace: String?,
        actionType: ActionType
    ) {
    }

    override fun checkPermissionsForUserInterface(serviceTicket: String?, userId: Long?, actionType: ActionType) {}

    override fun checkPermissionsForUserInterface(
        serviceTicket: String?,
        userId: Long?,
        entityId: String,
        action: ActionType
    ) {
    }

    override fun checkPermissionsForSupport(serviceTicket: String?, userId: Long?, actionType: ActionType) {}

    override fun checkPermissionsForSchema(
        serviceTicket: String?,
        userId: Long?,
        schemaId: Long?,
        namespace: String,
        actionType: ActionType
    ) {
    }

    override fun checkPermissionsForNamespace(
        serviceTicket: String?,
        userId: Long?,
        namespaceName: String?,
        actionType: ActionType
    ) {
    }

    override fun checkPermissionForNamespaceContent(
        serviceTicket: String?,
        userId: Long?,
        namespace: String,
        actionType: ActionType
    ) {
    }

    override fun buildMultiplePermission(
        serviceTicket: String?,
        userId: Long?,
        entityTypeIds: EntityType,
        actionType: ActionType
    ): MultiplePermission {
        TODO("Not yet implemented")
    }

    override fun checkPermissionForRole(serviceTicket: String?, actionType: ActionType) {
        TODO("Not yet implemented")
    }

    override fun getMaxPermissions(serviceTicket: String?, userId: Long): Map<EntityType, Permission?> {
        TODO("Not yet implemented")
    }

    override fun getOldPermissions(serviceTicket: String?, userId: Long): List<UserRole> {
        TODO("Not yet implemented")
    }
}
