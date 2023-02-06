package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.dto.SupplyItemStockDTO
import ru.yandex.market.logistics.cte.client.dto.SupplyItemsStockDTO
import ru.yandex.market.logistics.cte.client.enums.StockType

class SupplyItemServiceTest(
@Autowired private val supplyItemService: SupplyItemService
) : IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/supply-items/before.xml")
    fun getAscStock() {
        val result = supplyItemService.loadAscStock()
        assertions.assertThat(result.items.size).isEqualTo(2)

        assertions.assertThat(result.items[0].uit).isEqualTo("uit2")
        assertions.assertThat(result.items[0].marketShopSku).isEqualTo("sku2")
        assertions.assertThat(result.items[0].vendorId).isEqualTo(2)
        assertions.assertThat(result.items[0].stockType).isEqualTo(StockType.ASC)

        assertions.assertThat(result.items[1].uit).isEqualTo("uit3")
        assertions.assertThat(result.items[1].marketShopSku).isEqualTo("sku3")
        assertions.assertThat(result.items[1].vendorId).isEqualTo(3)
        assertions.assertThat(result.items[1].stockType).isEqualTo(StockType.ASC)
    }

    @Test
    @DatabaseSetup("classpath:service/supply-items/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/supply-items/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun updateItemsStockType() {
        val result = supplyItemService.updateItemsStockType(SupplyItemsStockDTO(listOf(
            SupplyItemStockDTO("uit1", "sku1", 1L, StockType.ASC),
            SupplyItemStockDTO("uit2", "sku2", 2L, StockType.DAMAGE),
            SupplyItemStockDTO("unknown uit", "unknown sku", 3L, StockType.ASC),
        )))
        assertions.assertThat(result.items.size).isEqualTo(1)
        assertions.assertThat(result.items[0].uit).isEqualTo("unknown uit")
    }
}
