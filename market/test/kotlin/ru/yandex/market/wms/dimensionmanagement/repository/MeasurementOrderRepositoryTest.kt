package ru.yandex.market.wms.dimensionmanagement.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import ru.yandex.market.wms.dimensionmanagement.core.domain.MeasurementOrderStatus
import ru.yandex.market.wms.dimensionmanagement.core.domain.MeasurementOrderType
import ru.yandex.market.wms.dimensionmanagement.core.dto.MeasurementOrderDto
import ru.yandex.market.wms.dimensionmanagement.exception.OrderNotFoundException
import java.math.BigDecimal
import java.time.LocalDateTime

class MeasurementOrderRepositoryTest : DimensionManagementIntegrationTest() {
    @Autowired
    private val repository: MeasurementOrderRepository? = null

    @Test
    @DatabaseSetup("/repository/measurement-order-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/measurement-order-repository/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getOrderByOrderKeyTest() {
        val expectedMeasurementOrder = MeasurementOrderDto.Builder()
            .orderKey(5)
            .serialNumber("0987654321")
            .status(MeasurementOrderStatus.ON_SAVING)
            .assigned("test")
            .loc("loc")
            .type(MeasurementOrderType.DIRECT)
            .fromId("from")
            .toId("to")
            .weight(BigDecimal("1.000"))
            .length(BigDecimal("2.000"))
            .height(BigDecimal("3.000"))
            .width(BigDecimal("4.000"))
            .sku("0000000520")
            .storer("10264169")
            .addDate(LocalDateTime.parse("2022-03-01T15:34:56.789"))
            .addWho("TEST")
            .editDate(LocalDateTime.parse("2022-03-01T15:34:56.789"))
            .editWho("TEST")
            .build()

        val measurementOrder = repository!!.findOrderByOrderKey("5")

        Assertions.assertEquals(expectedMeasurementOrder, measurementOrder)
    }

    @Test
    @DatabaseSetup("/repository/measurement-order-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/measurement-order-repository/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getOrderByOrderKeyTest_notFound() {
        val exception = Assertions.assertThrows(OrderNotFoundException::class.java) {
            repository!!.findOrderByOrderKey("4")
        }

        Assertions.assertEquals(
            "404 NOT_FOUND \"Задание обмера с идентификатором 4 не найдено\"",
            exception.message
        )
    }

    @Test
    @DatabaseSetup("/repository/measurement-order-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/measurement-order-repository/after-update-status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateStatusByOrderKeyTest() {
        repository!!.updateStatusByOrderKey("5", "DIMENSION_CONTROL", MeasurementOrderStatus.FINISHED)
    }

    @Test
    @DatabaseSetup("/repository/measurement-order-repository/immutable-state.xml")
    @ExpectedDatabase(
        value = "/repository/measurement-order-repository/after-update-to-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateToIdByOrderKeyTest() {
        repository!!.updateToIdByOrderKey("5", "DIMENSION_CONTROL", "to_id")
    }
}
