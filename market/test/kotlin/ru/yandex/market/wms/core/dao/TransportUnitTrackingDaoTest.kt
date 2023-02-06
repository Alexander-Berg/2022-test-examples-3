package ru.yandex.market.wms.core.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.model.enums.TransportUnitStatus
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.servicebus.vendor.VendorProvider
import ru.yandex.market.wms.servicebus.core.async.model.request.ConveyorType
import ru.yandex.market.wms.servicebus.core.async.model.request.TransportUnitTrackingLogRequest

class TransportUnitTrackingDaoTest : IntegrationTest() {

    @Autowired
    private val transportUnitTrackingDao: TransportUnitTrackingDao? = null

    @Test
    @DatabaseSetup("/transport-unit-tracking/db/before.xml")
    @ExpectedDatabase(value = "/transport-unit-tracking/db/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun insert() {
        val request = TransportUnitTrackingLogRequest.builder()
            .area("AREA")
            .conveyorType(ConveyorType.SHIPPINGSORTER)
            .currentLocation("location")
            .status(TransportUnitStatus.FINISHED)
            .unitId("UNIT01")
            .vendorProvider(VendorProvider.DEMATIC)
            .build()

        transportUnitTrackingDao!!.insert(request)
    }

    @Test
    @DatabaseSetup("/transport-unit-tracking/db/before.xml")
    @ExpectedDatabase(
        value = "/transport-unit-tracking/db/after-when-status-null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun insertWhenStatusIsNull() {
        val request = TransportUnitTrackingLogRequest.builder()
            .area("AREA")
            .conveyorType(ConveyorType.SHIPPINGSORTER)
            .currentLocation("location")
            .unitId("UNIT01")
            .status(null)
            .vendorProvider(VendorProvider.SCHAEFER)
            .build()

        transportUnitTrackingDao!!.insert(request)
    }
}
