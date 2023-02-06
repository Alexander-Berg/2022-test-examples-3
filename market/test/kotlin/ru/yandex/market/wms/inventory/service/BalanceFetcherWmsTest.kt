package ru.yandex.market.wms.inventory.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.wms.common.spring.dto.BalanceDto
import ru.yandex.market.wms.common.spring.dto.IdentityFrontInfoDto
import ru.yandex.market.wms.common.spring.dto.ProductItemDto
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity
import ru.yandex.market.wms.core.base.dto.BalancesDto
import ru.yandex.market.wms.inventory.model.dto.Loc
import ru.yandex.market.wms.inventory.model.enums.IdentityType

class BalanceFetcherWmsTest {

    @Test
    fun adaptWmsBalanceToInventory() {
        val loc = Loc("whs172", "2-01")
        val sku = "ROV123"
        val eans = linkedSetOf("EAN123", "EAN-123")
        val name = "Sku name"
        val storer = "Storer"
        val lot = "Lot"
        val qty = "2"
        val wmsItem1 = ProductItemDto(
            "uit1", setOf(
            IdentityFrontInfoDto.builder()
                .type(TypeOfIdentity.CIS)
                .identity("CIS123")
                .process("receiving").build(),
            IdentityFrontInfoDto.builder()
                .type(TypeOfIdentity.IMEI)
                .identity("IMEI123")
                .process("receiving").build(),
        ), false, false)
        val wmsItem2 = ProductItemDto(
            "uit1", setOf(
            IdentityFrontInfoDto.builder()
                .type(TypeOfIdentity.CIS)
                .identity("CIS124")
                .process("receiving").build()), false, false)
        val wmsBalance = BalancesDto(setOf(
            BalanceDto.builder()
                .loc(loc.name)
                .sku(sku)
                .eans(eans)
                .name(name)
                .storerKey(storer)
                .lot(lot)
                .qty(qty)
                .productItems(linkedSetOf(wmsItem1, wmsItem2))
                .build())
        )

        val inventoryBalance = wmsBalance.adaptToInventory(loc)

        Assertions.assertEquals(loc, inventoryBalance.loc)
        Assertions.assertEquals(1, inventoryBalance.itemGroups.size)

        val itemGroup = inventoryBalance.itemGroups.first()
        Assertions.assertEquals(sku, itemGroup.sku)
        Assertions.assertEquals(storer, itemGroup.storer)
        Assertions.assertEquals(lot, itemGroup.lot)
        Assertions.assertEquals(qty.toLong(), itemGroup.qty)
        Assertions.assertNull(itemGroup.lifetime)
        Assertions.assertNull(itemGroup.lifeTimeTemplate)
        Assertions.assertEquals(2, itemGroup.items.size)

        val items = itemGroup.items.toList()
        val item1 = items[0]
        val item2 = items[1]
        Assertions.assertEquals(5, item1.identities.size)
        Assertions.assertEquals(4, item2.identities.size)
        Assertions.assertEquals(2, item1.identities.filter { IdentityType.EAN == it.type }.size)
        Assertions.assertEquals(1, item1.identities.filter { IdentityType.CIS == it.type }.size)
        Assertions.assertEquals(1, item1.identities.filter { IdentityType.IMEI == it.type }.size)
        Assertions.assertEquals(1, item1.identities.filter { IdentityType.UIT == it.type }.size)
        Assertions.assertEquals(2, item2.identities.filter { IdentityType.EAN == it.type }.size)
        Assertions.assertEquals(1, item2.identities.filter { IdentityType.CIS == it.type }.size)
        Assertions.assertEquals(1, item2.identities.filter { IdentityType.UIT == it.type }.size)
    }
}
