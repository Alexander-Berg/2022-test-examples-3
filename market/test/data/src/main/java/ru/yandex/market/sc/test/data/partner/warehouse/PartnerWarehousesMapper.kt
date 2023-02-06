package ru.yandex.market.sc.test.data.partner.warehouse

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.functional.Functional.filterSuccessValues
import ru.yandex.market.sc.test.data.internal.warehouse.InternalWarehouseMapper

object PartnerWarehousesMapper {
    fun map(dto: PartnerWarehousesDto): Exceptional<PartnerWarehouses> = catch {
        PartnerWarehouses(
            warehouses = dto.warehouses
                .orEmpty()
                .map(InternalWarehouseMapper::map)
                .filterSuccessValues()
        )
    }
}