package ru.yandex.market.logistics.logistrator.queue.processor.sc_or_ff_creation

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID
import ru.yandex.market.logistics.logistrator.utils.RUSSIA_LOCATION
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto
import ru.yandex.market.logistics.management.entity.type.CapacityService
import ru.yandex.market.logistics.management.entity.type.CapacityType
import ru.yandex.market.logistics.management.entity.type.CountingType

internal class ScOrFfCapacitySettingProcessorTest :
    AbstractQueueProcessorTest<ScOrFfCapacitySettingProcessor>(
        ScOrFfPlatformClientsAddingProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: ScOrFfCapacitySettingProcessor

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_or_ff_creation/before/setup.xml"),
        DatabaseSetup("/db/sc_or_ff_creation/before/capacity_setting.xml", type = DatabaseOperation.REFRESH)
    )
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/platform_clients_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verifyCreateLmsCapacity(CountingType.ORDER, ORDERS_CAPACITY_VALUE, CapacityService.SHIPMENT)
            verifyCreateLmsCapacity(CountingType.ITEM, ITEMS_CAPACITY_VALUE, CapacityService.SHIPMENT)
        }
    )

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_or_ff_creation/before/setup_empty_optional_fields.xml"),
        DatabaseSetup("/db/sc_or_ff_creation/before/capacity_setting.xml", type = DatabaseOperation.REFRESH)
    )
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/platform_clients_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteEmptyOptionalFields() = abstractTestExecute(
        processor,
        verifyExecution = {
            verifyNoMoreInteractions(lmsClient)
        }
    )

    private fun verifyCreateLmsCapacity(countingType: CountingType, value: Long, capacityService: CapacityService) {
        verify(lmsClient).createCapacity(refEq(
            PartnerCapacityDto.newBuilder()
                .partnerId(PARTNER_ID)
                .locationFrom(RUSSIA_LOCATION)
                .locationTo(RUSSIA_LOCATION)
                .type(CapacityType.REGULAR)
                .capacityService(capacityService)
                .platformClientId(FIRST_PLATFORM_CLIENT_ID)
                .countingType(countingType)
                .value(value)
                .build()
        ))
    }

    private companion object {
        private const val ORDERS_CAPACITY_VALUE = 60000L
        private const val ITEMS_CAPACITY_VALUE = 120000L
        private const val FIRST_PLATFORM_CLIENT_ID = 1L
    }
}
