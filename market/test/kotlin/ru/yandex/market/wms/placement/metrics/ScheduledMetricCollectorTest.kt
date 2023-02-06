package ru.yandex.market.wms.placement.metrics

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.service.HostnameService
import ru.yandex.market.wms.common.spring.solomon.collecting.SolomonMetricCollectorFactory
import ru.yandex.market.wms.common.spring.solomon.config.SolomonMetricConfig.HOST_LABEL
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.constants.SolomonMetrics
import ru.yandex.market.wms.placement.dao.model.PlacementItemStatus
import ru.yandex.market.wms.placement.dao.model.PlacementItemStatus.*
import ru.yandex.market.wms.placement.dao.model.PlacementOrderStatus
import ru.yandex.market.wms.placement.dao.model.PlacementOrderStatus.*
import ru.yandex.market.wms.placement.service.PlacementOrderContentService
import ru.yandex.market.wms.placement.service.PlacementOrderIdService
import ru.yandex.market.wms.placement.service.PlacementOrderService
import ru.yandex.monlib.metrics.labels.Labels
import ru.yandex.monlib.metrics.primitives.GaugeInt64
import ru.yandex.monlib.metrics.registry.MetricId
import ru.yandex.monlib.metrics.registry.MetricRegistry
import java.util.*

class ScheduledMetricCollectorTest : PlacementIntegrationTest() {

    @Autowired
    private lateinit var orderService: PlacementOrderService

    @Autowired
    private lateinit var idService: PlacementOrderIdService

    @Autowired
    private lateinit var contentService: PlacementOrderContentService

    private lateinit var metricRegistry: MetricRegistry
    private lateinit var metricManager: SolomonMetricManager

    @BeforeEach
    fun init() {
        metricRegistry = MetricRegistry(Labels.of(HOST_LABEL, HostnameService().hostname))
        metricManager = SolomonMetricManager(SolomonMetricCollectorFactory(metricRegistry))
    }

    @Test
    @DatabaseSetup("/metrics/status/before.xml")
    fun `The collected order status metrics are written to the registry`() {
        val metricCollector = ScheduledMetricCollector(metricManager, orderService, idService, contentService)
        metricCollector.collectOrderStatus()

        val expectedMetricRecords = 3
        val expectedOrderCountByStatusPrepare = 1L
        val expectedOrderCountByStatusInProgress = 2L
        val expectedOrderCountByStatusFinished = 3L

        Assert.assertEquals(expectedMetricRecords, metricRegistry.estimateCount())

        checkMetricsByOrderStatus(PREPARE, expectedOrderCountByStatusPrepare)
        checkMetricsByOrderStatus(IN_PROGRESS, expectedOrderCountByStatusInProgress)
        checkMetricsByOrderStatus(FINISHED, expectedOrderCountByStatusFinished)
    }

    @Test
    @DatabaseSetup("/metrics/status/before.xml")
    fun `The collected ID status metrics are written to the registry`() {
        val metricCollector = ScheduledMetricCollector(metricManager, orderService, idService, contentService)

        metricCollector.collectIdStatus()

        val expectedMetricRecords = 5
        val expectedIdCountByStatusPlaced = 1L
        val expectedIdCountByStatusPartlyPlaced = 2L
        val expectedIdCountByStatusNotPlaced = 3L
        val expectedIdCountByStatusDropped = 2L
        val expectedIdCountByStatusLost = 1L

        Assert.assertEquals(expectedMetricRecords, metricRegistry.estimateCount())

        checkMetricsByIdStatus(PLACED, expectedIdCountByStatusPlaced)
        checkMetricsByIdStatus(PARTLY_PLACED, expectedIdCountByStatusPartlyPlaced)
        checkMetricsByIdStatus(NOT_PLACED, expectedIdCountByStatusNotPlaced)
        checkMetricsByIdStatus(DROPPED, expectedIdCountByStatusDropped)
        checkMetricsByIdStatus(LOST, expectedIdCountByStatusLost)
    }

    @Test
    @DatabaseSetup("/metrics/status/before.xml")
    fun `The collected serial status metrics are written to the registry`() {
        val metricCollector = ScheduledMetricCollector(metricManager, orderService, idService, contentService)

        metricCollector.collectSerialStatus()

        val expectedMetricRecords = 3
        val expectedSerialCountByStatusPlaced = 1L
        val expectedSerialCountByStatusDropped = 2L
        val expectedSerialCountByStatusLost = 1L

        Assert.assertEquals(expectedMetricRecords, metricRegistry.estimateCount())

        checkMetricsBySerialStatus(PLACED, expectedSerialCountByStatusPlaced)
        checkMetricsBySerialStatus(DROPPED, expectedSerialCountByStatusDropped)
        checkMetricsBySerialStatus(LOST, expectedSerialCountByStatusLost)
    }

    private fun checkMetricsBySerialStatus(status: PlacementItemStatus, count: Long) {
        Assert.assertEquals(
            "Error checking metrics by the status $status",
            count,
            (metricRegistry.getMetric(
                MetricId(
                    SolomonMetrics.SERIAL_STATUS_METRIC,
                    Labels.of(mapOf("status" to status.name.lowercase(Locale.getDefault())))
                )
            ) as GaugeInt64).get()
        )
    }

    private fun checkMetricsByIdStatus(status: PlacementItemStatus, count: Long) {
        Assert.assertEquals(
            "Error checking metrics by the status $status",
            count,
            (metricRegistry.getMetric(
                MetricId(
                    SolomonMetrics.ID_STATUS_METRIC,
                    Labels.of(mapOf("status" to status.name.lowercase(Locale.getDefault())))
                )
            ) as GaugeInt64).get()
        )
    }

    private fun checkMetricsByOrderStatus(status: PlacementOrderStatus, count: Long) {
        Assert.assertEquals(
            "Error checking metrics by the status $status",
            count,
            (metricRegistry.getMetric(
                MetricId(
                    SolomonMetrics.ORDER_STATUS_METRIC,
                    Labels.of(mapOf("status" to status.name.lowercase(Locale.getDefault())))
                )
            ) as GaugeInt64).get()
        )
    }
}
