package ru.yandex.market.mbi.orderservice.tms.persistence.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.orderservice.common.enum.LogisticServiceType
import ru.yandex.market.mbi.orderservice.common.enum.ShipmentType
import ru.yandex.market.mbi.orderservice.common.model.pg.LogisticServiceEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.LogisticServiceRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.LocalDateTime

@DbUnitDataSet(before = ["logisticService/logisticServiceRepositoryTest.before.csv"])
class LogisticServiceRepositoryTest : FunctionalTest() {

    @Autowired
    lateinit var logisticServiceRepository: LogisticServiceRepository

    @Test
    fun `test find by id`() {
        val logisticService = logisticServiceRepository.findByLogisticServiceId(3L)
        assertLogisticService(logisticService)
    }

    @Test
    fun `test upsert`() {
        // Вставка нового
        logisticServiceRepository.upsert(
            LogisticServiceEntity(
                id = 1,
                humanReadableId = "Kola",
                externalId = "kola-129",
                serviceType = LogisticServiceType.FULFILLMENT,
                serviceName = "Склад Кола",
                address = "Улица Сезам",
                shipmentType = ShipmentType.IMPORT,
                settlement = "Минас-Тирит",
                LocalDateTime.of(2011, 11, 11, 11, 11, 11)
            )
        )

        // Обновление существующего
        logisticServiceRepository.upsert(
            LogisticServiceEntity(
                id = 3,
                humanReadableId = "Murmansk Warehouse 12",
                externalId = "olgenegorsk-12",
                serviceType = LogisticServiceType.SORTING_CENTER,
                serviceName = "Склад Оленегорск",
                address = "Улица Павлова",
                shipmentType = ShipmentType.EXPRESS,
                settlement = "Оленегорск",
                LocalDateTime.of(2012, 12, 12, 12, 12, 12)
            )
        )

        var logisticService = logisticServiceRepository.findByLogisticServiceId(1L)

        assertNotNull(logisticService!!)
        assertThat(logisticService.id).isEqualTo(1L)
        assertThat(logisticService.humanReadableId).isEqualTo("Kola")
        assertThat(logisticService.externalId).isEqualTo("kola-129")
        assertThat(logisticService.serviceType).isEqualTo(LogisticServiceType.FULFILLMENT)
        assertThat(logisticService.serviceName).isEqualTo("Склад Кола")
        assertThat(logisticService.address).isEqualTo("Улица Сезам")
        assertThat(logisticService.shipmentType).isEqualTo(ShipmentType.IMPORT)
        assertThat(logisticService.settlement).isEqualTo("Минас-Тирит")
        assertThat(logisticService.updatedAt).isEqualTo(
            LocalDateTime.of(2011, 11, 11, 11, 11, 11)
        )

        logisticService = logisticServiceRepository.findByLogisticServiceId(3L)

        assertNotNull(logisticService!!)
        assertThat(logisticService.id).isEqualTo(3L)
        assertThat(logisticService.humanReadableId).isEqualTo("Murmansk Warehouse 12")
        assertThat(logisticService.externalId).isEqualTo("olgenegorsk-12")
        assertThat(logisticService.serviceType).isEqualTo(LogisticServiceType.SORTING_CENTER)
        assertThat(logisticService.serviceName).isEqualTo("Склад Оленегорск")
        assertThat(logisticService.address).isEqualTo("Улица Павлова")
        assertThat(logisticService.shipmentType).isEqualTo(ShipmentType.EXPRESS)
        assertThat(logisticService.settlement).isEqualTo("Оленегорск")
        assertThat(logisticService.updatedAt).isEqualTo(
            LocalDateTime.of(2012, 12, 12, 12, 12, 12)
        )
    }

    @Test
    fun `test batch upsert`() {
        logisticServiceRepository.batchUpsert(
            listOf(
                LogisticServiceEntity(
                    // Вставка нового
                    id = 1,
                    humanReadableId = "Kola",
                    externalId = "kola-129",
                    serviceType = LogisticServiceType.FULFILLMENT,
                    serviceName = "Склад Кола",
                    address = "Улица Сезам",
                    shipmentType = ShipmentType.IMPORT,
                    settlement = "Минас-Тирит",
                    LocalDateTime.of(2011, 11, 11, 11, 11, 11)
                ),
                LogisticServiceEntity(
                    // Обновление существующего
                    id = 3,
                    humanReadableId = "Murmansk Warehouse 12",
                    externalId = "olgenegorsk-12",
                    serviceType = LogisticServiceType.SORTING_CENTER,
                    serviceName = "Склад Оленегорск",
                    address = "Улица Павлова",
                    shipmentType = ShipmentType.EXPRESS,
                    settlement = "Оленегорск",
                    LocalDateTime.of(2012, 12, 12, 12, 12, 12)
                )
            )
        )

        var logisticService = logisticServiceRepository.findByLogisticServiceId(1L)

        assertNotNull(logisticService!!)
        assertThat(logisticService.id).isEqualTo(1L)
        assertThat(logisticService.humanReadableId).isEqualTo("Kola")
        assertThat(logisticService.externalId).isEqualTo("kola-129")
        assertThat(logisticService.serviceType).isEqualTo(LogisticServiceType.FULFILLMENT)
        assertThat(logisticService.serviceName).isEqualTo("Склад Кола")
        assertThat(logisticService.address).isEqualTo("Улица Сезам")
        assertThat(logisticService.shipmentType).isEqualTo(ShipmentType.IMPORT)
        assertThat(logisticService.settlement).isEqualTo("Минас-Тирит")
        assertThat(logisticService.updatedAt).isEqualTo(
            LocalDateTime.of(2011, 11, 11, 11, 11, 11)
        )

        logisticService = logisticServiceRepository.findByLogisticServiceId(3L)

        assertNotNull(logisticService!!)
        assertThat(logisticService.id).isEqualTo(3L)
        assertThat(logisticService.humanReadableId).isEqualTo("Murmansk Warehouse 12")
        assertThat(logisticService.externalId).isEqualTo("olgenegorsk-12")
        assertThat(logisticService.serviceType).isEqualTo(LogisticServiceType.SORTING_CENTER)
        assertThat(logisticService.serviceName).isEqualTo("Склад Оленегорск")
        assertThat(logisticService.address).isEqualTo("Улица Павлова")
        assertThat(logisticService.shipmentType).isEqualTo(ShipmentType.EXPRESS)
        assertThat(logisticService.settlement).isEqualTo("Оленегорск")
        assertThat(logisticService.updatedAt).isEqualTo(
            LocalDateTime.of(2012, 12, 12, 12, 12, 12)
        )
    }

    @Test
    fun `get logistic sesrvices by partnerId`() {
        val services = logisticServiceRepository.findByPartnerIds(listOf(101L))
        assertThat(services).hasSize(1)
        assertThat(services.containsKey(101L))
        assertLogisticService(services[101L]!![0])
    }

    private fun assertLogisticService(logisticService: LogisticServiceEntity?) {
        assertNotNull(logisticService!!)
        assertThat(logisticService.id).isEqualTo(3L)
        assertThat(logisticService.humanReadableId).isEqualTo("Murmansk 11")
        assertThat(logisticService.externalId).isEqualTo("Monchegorsk 11")
        assertThat(logisticService.serviceType).isEqualTo(LogisticServiceType.SORTING_CENTER)
        assertThat(logisticService.serviceName).isEqualTo("Склад в Мончегорске")
        assertThat(logisticService.address).isEqualTo("Улица Ленина")
        assertThat(logisticService.shipmentType).isEqualTo(ShipmentType.WITHDRAW)
        assertThat(logisticService.settlement).isEqualTo("Мончегорск")
        assertThat(logisticService.updatedAt).isEqualTo(
            LocalDateTime.of(2010, 10, 10, 10, 10, 10)
        )

        assertNull(logisticServiceRepository.findByLogisticServiceId(1L))
    }
}
