package ru.yandex.market.mbo.cms.core.permission.mock

import ru.yandex.market.mbo.cms.core.dao.permission.EntityTypeDao
import ru.yandex.market.mbo.cms.core.models.permission.EntityType

class EntityTypeDaoMock: EntityTypeDao {
    override fun getEntityTypeInheritanceMap(): Map<EntityType, EntityType?> {
        return mapOf(
                EntityType.DOCUMENT_TYPE to EntityType.NAMESPACE_CONTENT,
                EntityType.SCHEMA to EntityType.NAMESPACE_SCHEMA,
                EntityType.DOCUMENT to EntityType.DOCUMENT_TYPE,
                EntityType.NAMESPACE to null,
                EntityType.NAMESPACE_CONTENT to null
        )
    }
}
