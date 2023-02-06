package ru.yandex.market.mbo.cms.core.permission.mock

import ru.yandex.market.mbo.cms.core.dao.permission.EntityWithoutPermissionInheritanceDao
import ru.yandex.market.mbo.cms.core.models.permission.EntityType
import ru.yandex.market.mbo.cms.core.models.permission.EntityWithoutPermissionInheritance

class EntityWithoutPermissionInheritanceDaoMock: EntityWithoutPermissionInheritanceDao {
    override fun save(entityWithoutPermissionInheritance: EntityWithoutPermissionInheritance) {
        throw NotImplementedError("Not yet implemented")
    }

    override fun delete(entityWithoutPermissionInheritance: EntityWithoutPermissionInheritance) {
        throw NotImplementedError("Not yet implemented")
    }

    override fun get(entityWithoutPermissionInheritance: EntityWithoutPermissionInheritance): EntityWithoutPermissionInheritance? {
        val isNonInheritance = entityWithoutPermissionInheritance.entityId > "11"
        return if (isNonInheritance)
            EntityWithoutPermissionInheritance(
                    entityWithoutPermissionInheritance.entityTypeId,
                    entityWithoutPermissionInheritance.entityId
            )
        else null
    }

    override fun getAll(entities: List<EntityWithoutPermissionInheritance>, entityTypeIds: Set<EntityType>): List<EntityWithoutPermissionInheritance> {
        throw NotImplementedError("Not yet implemented")
    }
}
