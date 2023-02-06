package ru.yandex.market.wms.placement.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.dao.model.PlacementOrderStatus
import ru.yandex.market.wms.placement.dao.model.PlacementOrderType
import ru.yandex.market.wms.placement.model.dto.IdInfo
import ru.yandex.market.wms.placement.model.dto.PlacementOrderContent

class PlacementOrderContentIntegrationTest(
    @Autowired private val contentService: PlacementOrderContentService
) : PlacementIntegrationTest() {
    @MockBean
    @Autowired
    private lateinit var dbConfigService: DbConfigService

    @BeforeEach
    fun clean() {
        reset(dbConfigService)
    }

    @Test
    @DatabaseSetup("/service/placement-order-content/get-content/common-before.xml")
    fun `getOrderContent - ok`() {
        val expectedOrderContent = baseExpectedOrderContent

        val order = contentService.getOrCreatePlacementOrder(PlacementOrderType.PLACEMENT)
        assertThat(order.placementOrderKey).isEqualTo(expectedOrderContent.orderKey)
        assertThat(order.status).isEqualTo(expectedOrderContent.status)
        assertThat(order.type).isEqualTo(expectedOrderContent.orderType)

        val content = contentService.getOrderContent(order)

        assertThat(content).isEqualTo(expectedOrderContent)
    }

    @Test
    @DatabaseSetups(
        value = [DatabaseSetup("/service/placement-order-content/get-content/common-before.xml"),
            DatabaseSetup("/service/placement-order-content/get-content/row-recommend/before.xml")],
    )
    fun `getOrderContent with rows recommendation`() {
        whenever(dbConfigService.getConfigAsStringList(NSqlConfigKey.YM_PLACEMENT_ROW_RECOM_ZONES))
            .thenReturn(listOf("MEZ-1"))

        val recommendedRows = listOf("B1-01", "C1-01")
        val expectedOrderContent = baseExpectedOrderContent.copy(recommendedRows = recommendedRows)

        val order = contentService.getOrCreatePlacementOrder(PlacementOrderType.PLACEMENT)
        val content = contentService.getOrderContent(order)

        assertThat(content).isEqualTo(expectedOrderContent)
    }

    private companion object {
        private val baseExpectedOrderContent =
            PlacementOrderContent(
                orderKey = 1,
                status = PlacementOrderStatus.IN_PROGRESS,
                orderType = PlacementOrderType.PLACEMENT,
                idList = listOf("RCP001", "RCP002"),
                idInfoList = listOf(IdInfo("RCP001", null, 2), IdInfo("RCP002", null, 1)),
                activeId = null,
                uitCount = 3,
                uitPlacedCount = 2
            )
    }
}
