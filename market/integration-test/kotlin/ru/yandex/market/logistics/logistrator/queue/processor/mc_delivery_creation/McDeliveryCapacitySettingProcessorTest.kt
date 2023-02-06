package ru.yandex.market.logistics.logistrator.queue.processor.mc_delivery_creation

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID
import ru.yandex.market.logistics.logistrator.utils.RUSSIA_LOCATION
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto
import ru.yandex.market.logistics.management.entity.type.CapacityType
import ru.yandex.market.logistics.management.entity.type.CountingType

internal class McDeliveryCapacitySettingProcessorTest :
    AbstractQueueProcessorTest<McDeliveryCapacitySettingProcessor>(
        McDeliveryPossibleOrderChangesCreationProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: McDeliveryCapacitySettingProcessor

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/capacity_setting.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/possible_order_changes_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).createCapacity(refEq(LMS_PARTNER_CAPACITY_DTO))
        }
    )

    private companion object {
        val LMS_PARTNER_CAPACITY_DTO = PartnerCapacityDto.newBuilder()
            .partnerId(PARTNER_ID)
            .locationFrom(RUSSIA_LOCATION)
            .locationTo(RUSSIA_LOCATION)
            .type(CapacityType.REGULAR)
            .platformClientId(1L)
            .countingType(CountingType.ORDER)
            .value(200)
            .build()!!
    }
}
