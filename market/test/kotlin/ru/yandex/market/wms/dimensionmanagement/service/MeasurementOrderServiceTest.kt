package ru.yandex.market.wms.dimensionmanagement.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.core.base.request.DimensionItem
import ru.yandex.market.wms.core.base.request.SaveDimensionsRequest
import ru.yandex.market.wms.core.base.request.SkuDimensionsItem
import ru.yandex.market.wms.core.base.request.SourceType
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import ru.yandex.market.wms.dimensionmanagement.exception.InvalidOrderStatusForFinishEndingException
import ru.yandex.market.wms.dimensionmanagement.exception.NullableMeasurementsException
import java.math.BigDecimal
import java.util.stream.Stream

class MeasurementOrderServiceTest : DimensionManagementIntegrationTest() {
    @Autowired
    private val service: MeasurementOrderService? = null

    @Autowired
    @MockBean
    private lateinit var coreClient: CoreClient

    @Autowired
    private lateinit var securityDataProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider

    @Test
    @DatabaseSetup("/service/measurement-order-service/right-status/before.xml")
    @ExpectedDatabase(
        value = "/service/measurement-order-service/right-status/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getOrderByOrderKeyTest() {
        val request = SaveDimensionsRequest(
            listOf(
                SkuDimensionsItem(
                    skuId = SkuId("10264169", "0000000520"),
                    manufacturerSku = null,
                    dimensions = DimensionItem(
                        weight = BigDecimal("1.000"),
                        length = BigDecimal("2.000"),
                        height = BigDecimal("3.000"),
                        width = BigDecimal("4.000"),
                    )
                )
            ),
            securityDataProvider.user,
            SourceType.MEASUREMENT
        )

        service!!.finishMeasurementOrder("5", securityDataProvider.user)

        Mockito.verify(coreClient, Mockito.times(1)).saveSkuDimensions(request)
    }

    @ParameterizedTest
    @MethodSource("getOrderByOrderKeyTest_wrongStatusArgs")
    @DatabaseSetup("/service/measurement-order-service/wrong-status/before.xml")
    @ExpectedDatabase(
        value = "/service/measurement-order-service/wrong-status/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getOrderByOrderKeyTest_wrongStatus(orderKey: String, expectedMessage: String) {
        val exception = Assertions.assertThrows(InvalidOrderStatusForFinishEndingException::class.java) {
            service!!.finishMeasurementOrder(orderKey, securityDataProvider.user)
        }

        Assertions.assertEquals(expectedMessage, exception.message)
    }

    @ParameterizedTest
    @MethodSource("getOrderByOrderKeyTest_emptyDimensionsArgs")
    @DatabaseSetup("/service/measurement-order-service/empty-dimensions/before.xml")
    @ExpectedDatabase(
        value = "/service/measurement-order-service/empty-dimensions/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getOrderByOrderKeyTest_emptyDimensions(orderKey: String, expectedMessage: String) {
        val exception = Assertions.assertThrows(NullableMeasurementsException::class.java) {
            service!!.finishMeasurementOrder(orderKey, securityDataProvider.user)
        }

        Assertions.assertEquals(expectedMessage, exception.message)
    }

    companion object {
        @JvmStatic
        private fun getOrderByOrderKeyTest_wrongStatusArgs(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(
                    "1",
                    "400 BAD_REQUEST \"Unable to end finishing order in status NEW (orderKey=1)\""
                ), Arguments.of(
                    "2",
                    "400 BAD_REQUEST \"Unable to end finishing order in status ASSIGNED (orderKey=2)\""
                ), Arguments.of(
                    "3",
                    "400 BAD_REQUEST \"Unable to end finishing order in status IN_PROGRESS (orderKey=3)\""
                ), Arguments.of(
                    "4",
                    "400 BAD_REQUEST \"Unable to end finishing order in status FINISHED (orderKey=4)\""
                ), Arguments.of(
                    "5",
                    "400 BAD_REQUEST \"Unable to end finishing order in status CANCELED (orderKey=5)\""
                ), Arguments.of(
                    "6",
                    "400 BAD_REQUEST \"Unable to end finishing order in status FAILED (orderKey=6)\""
                )
            )
        }

        @JvmStatic
        private fun getOrderByOrderKeyTest_emptyDimensionsArgs(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(
                    "1",
                    "500 INTERNAL_SERVER_ERROR \"For finish measurement order all measurements must be not null. Height: null, length: null, width: null, weight: null\""
                ), Arguments.of(
                    "2",
                    "500 INTERNAL_SERVER_ERROR \"For finish measurement order all measurements must be not null. Height: 3.000, length: 2.000, width: null, weight: null\""
                ), Arguments.of(
                    "3",
                    "500 INTERNAL_SERVER_ERROR \"For finish measurement order all measurements must be not null. Height: 3.000, length: null, width: 1.000, weight: 1.000\""
                ), Arguments.of(
                    "4",
                    "500 INTERNAL_SERVER_ERROR \"For finish measurement order all measurements must be not null. Height: null, length: 2.000, width: 1.000, weight: 1.000\""
                ), Arguments.of(
                    "5",
                    "500 INTERNAL_SERVER_ERROR \"For finish measurement order all measurements must be not null. Height: 3.000, length: 2.000, width: 1.000, weight: 1.000\""
                )
            )
        }
    }
}
