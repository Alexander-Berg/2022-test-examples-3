package ru.yandex.market.sc.test.data.partner.warehouse

import ru.yandex.market.sc.test.data.common.Wrapper
import ru.yandex.market.sc.test.data.internal.warehouse.InternalWarehouseDto

data class PartnerWarehousesDto(val warehouses: List<InternalWarehouseDto>?) :
    Wrapper<List<InternalWarehouseDto>?> {
    override fun unwrap(): List<InternalWarehouseDto>? {
        return warehouses
    }

}
