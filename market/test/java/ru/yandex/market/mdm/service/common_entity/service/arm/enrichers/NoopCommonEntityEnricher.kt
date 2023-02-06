package ru.yandex.market.mdm.service.common_entity.service.arm.enrichers

import ru.yandex.market.mdm.service.common_entity.model.CommonEntity

class NoopCommonEntityEnricher : CommonEntityEnricher {

    override fun enrich(entity: CommonEntity): CommonEntity {
        return entity
    }
}
