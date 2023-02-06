package ru.yandex.market.replenishment.autoorder.service.tender

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.application.properties.utils.Environments.PRESTABLE
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mboc.http.DeliveryParams
import ru.yandex.market.mboc.http.MboMappingsForDelivery
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
import ru.yandex.market.mboc.http.MbocCommon
import ru.yandex.market.mboc.http.MbocMatrixAvailability
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.replenishment.autoorder.api.dto.tender.PriceSpecificationAxaptaSplitDto
import ru.yandex.market.replenishment.autoorder.api.dto.tender.PriceSpecificationDto
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationFilters
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException
import ru.yandex.market.replenishment.autoorder.model.DemandType
import ru.yandex.market.replenishment.autoorder.model.PartnerTenderRequest
import ru.yandex.market.replenishment.autoorder.model.PartnerTenderStatus
import ru.yandex.market.replenishment.autoorder.model.PriceSpecificationStatus
import ru.yandex.market.replenishment.autoorder.model.ShopSkuKeyWithStatus
import ru.yandex.market.replenishment.autoorder.model.SskuStatus
import ru.yandex.market.replenishment.autoorder.model.TenderStatus
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO
import ru.yandex.market.replenishment.autoorder.model.dto.TenderTruckDTO
import ru.yandex.market.replenishment.autoorder.model.dto.TenderTruckSskuDTO
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import ru.yandex.market.replenishment.autoorder.service.DbReplenishmentService
import ru.yandex.market.replenishment.autoorder.service.MailSenderService
import ru.yandex.market.replenishment.autoorder.service.client.Cabinet1PClient
import ru.yandex.market.replenishment.autoorder.service.client.DeepMindClient
import java.time.LocalDateTime

@MockBean(Cabinet1PClient::class)
@WithMockLogin
class TenderServiceTest : FunctionalTest() {
    companion object {
        private val MOCKED_DATE_TIME = LocalDateTime.of(2020, 9, 6, 8, 0)
        private val MOCKED_SSKUS_STATUSES = listOf(
            ShopSkuKeyWithStatus(4201, "234234", SskuStatus.DELISTED),
            ShopSkuKeyWithStatus(4201, "235235", SskuStatus.INACTIVE_TMP),
            ShopSkuKeyWithStatus(4201, "234234_2", SskuStatus.ACTIVE),
            ShopSkuKeyWithStatus(4202, "234234", SskuStatus.ACTIVE)
        )

        init {
            // чтобы тесты не ломались после REPLENISHMENT-7979
            System.setProperty("environment", PRESTABLE)
        }
    }

    private lateinit var emailService: MailSenderService

    @Autowired
    private lateinit var tenderService: TenderService

    @Autowired
    private lateinit var dbReplenishmentService: DbReplenishmentService

    @Autowired
    private lateinit var cabinet1PClient: Cabinet1PClient

    private var deepMindClient: DeepMindClient = Mockito.mock(DeepMindClient::class.java)

    @Before
    fun mockDateTime() {
        setTestTime(MOCKED_DATE_TIME)
    }

    @Before
    fun mockServices() {
        `when`(deepMindClient.getSskusStatuses(any())).thenReturn(MOCKED_SSKUS_STATUSES)
        `when`(deepMindClient.updateSskusStatuses(any())).thenReturn(true)
        emailService = Mockito.mock(MailSenderService::class.java)
    }

    @Test
    @DbUnitDataSet(
        before = ["testSendLettersToCatman_isOk.before.csv"],
    )
    fun testSendLettersToCatman_isOk() {
        val filter = RecommendationFilters(null, null)
        val subjectCaptor = argumentCaptor<String>()
        doNothing().`when`(emailService)
            .sendWithAttach(anyOrNull(), subjectCaptor.capture(), anyOrNull(), anyOrNull(), anyOrNull())
        ReflectionTestUtils.setField(tenderService, "mailSenderService", emailService)
        tenderService.sendLettersToCatman(1, setOf(1), filter)
        val subjects = subjectCaptor.allValues
        assertThat(subjects, hasSize(1))
        assertEquals("Потребность 06.09.2020 (36) \"Поставщик №010\"", subjects[0])
    }

    @Test
    @DbUnitDataSet(before = ["setStatusWinnerFixed_withLockedWarehouse.before.csv"])
    fun setStatusWinnerFixed_withLockedWarehouse() {
        assertEquals(
            assertThrows<UserWarningException> {
                tenderService.setStatus(1, TenderStatus.WINNER_FIXED)
            }.message, "Для поставщика '2' и SSKU 2.1 нет доступных складов\\n" +
                "Для поставщика '3' и SSKU 3.1 нет доступных складов"
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["warehouseWinnerFixedTenderTest.before.csv"],
        after = ["warehouseWinnerFixedTenderTest.after.csv"]
    )
    fun testWarehouseWinnerFixedTender() {
        mockOpenWarehouses(
            OpenWarehouse("ssku1", 6, listOf(172)),
            OpenWarehouse("ssku2", 5),
            OpenWarehouse("ssku3", 1, listOf(172, 304)),
        )
        `when`(deepMindClient.getSskusStatuses(any())).thenReturn(
            listOf(
                ShopSkuKeyWithStatus(6, "ssku1", SskuStatus.ACTIVE),
                ShopSkuKeyWithStatus(5, "ssku2", SskuStatus.ACTIVE),
                ShopSkuKeyWithStatus(1, "ssku3", SskuStatus.ACTIVE),
            )
        )
        mockDeepMindClient()

        tenderService.setStatus(1, TenderStatus.WINNER_FIXED)
    }

    @Test
    @DbUnitDataSet(
        before = ["startCopyOfTender.before.csv"],
        after = ["startCopyOfTender.after.csv"]
    )
    fun startCopyOfTender() {
        tenderService.setStatus(11, TenderStatus.STARTED)
        dbReplenishmentService.adjustRecommendations(
            DemandType.TENDER, 22, 1, listOf(
                AdjustedRecommendationDTO.builder().id(3).msku(1).adjustedPurchQty(14).correctionReason(1).build()
            ), "boris"
        )
        tenderService.setStatus(22, TenderStatus.STARTED)
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults.before.csv"],
        after = ["exportResults_simple.after.csv"]
    )
    fun testExportResults_Simple() {
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults_deliveryAndOrderDateSet.before.csv"],
        after = ["exportResults_deliveryAndOrderDateSet.after.csv"]
    )
    fun testExportResults_DeliveryAndOrderDateSet() {
        setTestTime(LocalDateTime.of(2022, 4, 19, 7, 1))
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults_deliveryAndOrderDateXdockSet.before.csv"],
        after = ["exportResults_deliveryAndOrderDateXdockSet.after.csv"]
    )
    fun testExportResults_DeliveryAndOrderDateXdocSet() {
        setTestTime(LocalDateTime.of(2022, 4, 19, 7, 1))
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResultsWithTrucks.before.csv"],
        after = ["exportResultsWithTrucks.after.csv"]
    )
    fun testExportResultsWithTrucksGrouping() {
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(
        before = ["setDemand_1pIdToTenderTrucks.before.csv"],
        after = ["setDemand_1pIdToTenderTrucks.after.csv"]
    )
    fun testSetDemand1pIdToTenderTrucks() {
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResultsWithoutCatman.before.csv"],
        after = ["exportResults_simpleWithoutCatman.after.csv"]
    )
    fun testExportResults_simpleWithoutCatman() {
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResultsWithoutResponsible.before.csv"],
        after = ["exportResultsWithoutResponsible.after.csv"]
    )
    fun testExportResults_withoutResponsible() {
        mockWarehousesForExport(false)
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults.before.csv"],
        after = ["exportResults_simpleNotCompleted.after.csv"]
    )
    fun testExportResults_simpleNotComplete() {
        mockWarehousesForExport(false)
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults_2ssku_for_msku.before.csv"],
        after = ["exportResults_2ssku_for_msku.after.csv"]
    )
    fun testExportResults_2ssku_for_msku() {
        mockWarehousesForExport(false, listOf("234234", "234234_2"))
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults.filter_empty_ssku.before.csv"],
        after = ["exportResults.filter_empty_ssku.after.csv"]
    )
    fun testExportResults_filter_empty_ssku() {
        mockWarehousesForExport(false)
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults_partial_sent.before.csv"],
        after = ["exportResults_partial_sent.after.csv"]
    )
    fun testExportResults_partial_sent() {
        mockWarehousesForExport(false)
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults_with_group.before.csv"],
        after = ["exportResults_simple.after.csv"]
    )
    fun testExportResults_withGroup() {
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(before = ["exportResults_simple.after.csv"])
    fun testExportResults_already_completed() {
        mockWarehousesForExport()
        assertEquals(assertThrows<UserWarningException> {
            tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
        }.message, "По тендерному листу #123 уже сформированы потребности")
    }

    @Test
    @DbUnitDataSet(before = ["exportResults.before.csv"])
    fun testExportResults_EmptySupplierIds() {
        assertEquals(assertThrows<UserWarningException> {
            tenderService.generateDemandsAndCompleteIfAllSent(123, setOf())
        }.message, "Не указаны идентификаторы поставщиков.")
    }

    @Test
    @DbUnitDataSet(before = ["exportResults_empty_log_params.before.csv"])
    fun testExportResults_empty_log_params() {
        mockWarehousesForExport()
        mockDeepMindClient()
        assertEquals(assertThrows<UserWarningException> {
            tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
        }.message, "Для поставщика TestSupplier2 не найдено лог параметров на склад 304 для типа поставки: DIRECT")
    }

    @Test
    @DbUnitDataSet(before = ["exportResults_wrongSupplyRoute.before.csv"])
    fun testExportResults_wrongSupplyRoute() {
        mockWarehousesForExport()
        mockDeepMindClient()
        assertEquals(
            assertThrows<UserWarningException> {
                tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
            }.message,
            "Для поставщика TestSupplier2 не найдено лог параметров на склад 304 для типа поставки: MONO_XDOC\\n" +
                "Для поставщика TestSupplier1 не найдено лог параметров на склад 172 для типа поставки: XDOC"
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["exportResults_withZeroItems.before.csv"],
        after = ["exportResults_withZeroItems.after.csv"]
    )
    fun testExportResults_withZeroItems() {
        mockWarehousesForExport()
        mockDeepMindClient()
        tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201, 4202))
    }

    @Test
    @DbUnitDataSet(before = ["exportResults_noFittedWarehouse.before.csv"])
    fun testExportResults_noFittedWarehouse() {
        mockWarehousesForExport()
        mockDeepMindClient()
        assertEquals(assertThrows<UserWarningException> {
            tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
        }.message, "Для поставщика 'TestSupplier1' и SSKU 000111.235235 нет доступных складов")
    }

    @Test
    @DbUnitDataSet(before = ["exportResults.before.csv"])
    fun testExportResults_wrong_ssku_statuses_without_status_update() {
        val mockParams = Mockito.mock(DeliveryParams::class.java)
        val builder = SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setAvailability(SupplierOffer.Availability.DELISTED)
                    .setShopSku("234234")
                    .setSupplierId(4201)
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder()
                            .setWarehouseId(172)
                            .addAvailabilityReasons(
                                MbocMatrixAvailability.AvailabilityReason.newBuilder()
                                    .setMessage(
                                        MbocCommon.Message.newBuilder().setMessageCode(
                                            "mboc.msku.error.supply-forbidden.delisted-offer"
                                        ).build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder().setWarehouseId(304).build()
                    )
                    .build()
            )
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setAvailability(SupplierOffer.Availability.INACTIVE)
                    .setShopSku("235235")
                    .setSupplierId(4201)
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder()
                            .setWarehouseId(172)
                            .addAvailabilityReasons(
                                MbocMatrixAvailability.AvailabilityReason.newBuilder()
                                    .setMessage(
                                        MbocCommon.Message.newBuilder().setMessageCode(
                                            "mboc.msku.error.supply-forbidden.inactive-tmp-offer"
                                        ).build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder().setWarehouseId(304).build()
                    )
                    .build()
            )
        `when`(mockParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(builder.build())
        `when`(deepMindClient.updateSskusStatuses(any())).thenReturn(false)

        ReflectionTestUtils.setField(tenderService, "deliveryParams", mockParams)
        mockDeepMindClient()

        assertEquals(
            assertThrows<UserWarningException> {
                tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
            }.message, "SSKU 000111.235235 в статусе INACTIVE_TMP\\n" +
                "SSKU 000111.234234 в статусе DELISTED"
        )
    }

    @Test
    @DbUnitDataSet(before = ["exportResults.before.csv"])
    fun testExportResults_wrong_ssku_statuses_with_status_update() {
        val mockParams = Mockito.mock(DeliveryParams::class.java)
        val builder = SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setAvailability(SupplierOffer.Availability.DELISTED)
                    .setShopSku("234234")
                    .setSupplierId(4201)
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder()
                            .setWarehouseId(172)
                            .addAvailabilityReasons(
                                MbocMatrixAvailability.AvailabilityReason.newBuilder()
                                    .setMessage(
                                        MbocCommon.Message.newBuilder().setMessageCode(
                                            "mboc.msku.error.supply-forbidden.delisted-offer"
                                        ).build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder().setWarehouseId(304).build()
                    )
                    .build()
            )
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setAvailability(SupplierOffer.Availability.INACTIVE)
                    .setShopSku("235235")
                    .setSupplierId(4201)
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder()
                            .setWarehouseId(172)
                            .addAvailabilityReasons(
                                MbocMatrixAvailability.AvailabilityReason.newBuilder()
                                    .setMessage(
                                        MbocCommon.Message.newBuilder().setMessageCode(
                                            "mboc.msku.error.supply-forbidden.inactive-tmp-offer"
                                        ).build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .addWarehouseIntervalAvailabilities(
                        MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder().setWarehouseId(304).build()
                    )
                    .build()
            )
        `when`(mockParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(builder.build())
        `when`(deepMindClient.updateSskusStatuses(any())).thenReturn(true)

        ReflectionTestUtils.setField(tenderService, "deliveryParams", mockParams)
        mockDeepMindClient()

        try {
            tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    @DbUnitDataSet(before = ["getPartnerTenders.before.csv"])
    fun getPartnerTenders() {
        val baseDateTime = LocalDateTime.of(2019, 3, 18, 0, 0)
        val baseDate = baseDateTime.toLocalDate()
        `when`(timeService.nowDate).thenReturn(baseDate)
        val supplierTenders = tenderService.getPartnerTenders(10)
            .sortedBy { it.demandId }
        val sof = "Софьино"
        val samara = "Самара"

        assertNotNull(supplierTenders)
        assertEquals(14, supplierTenders.size)

        val activeTs = LocalDateTime.parse("2021-06-24T00:00")
        assertEquals(
            supplierTenders[0],
            PartnerTenderRequest(1, PartnerTenderStatus.ACTIVE, "catman_2", baseDateTime, sof, activeTs)
        )
        assertEquals(
            supplierTenders[1],
            PartnerTenderRequest(2, PartnerTenderStatus.ACTIVE, "catman_2", baseDateTime, sof, activeTs)
        )
        assertEquals(
            supplierTenders[2],
            PartnerTenderRequest(3, PartnerTenderStatus.SENT, "catman_2", baseDateTime, sof)
        )
        assertEquals(
            supplierTenders[3],
            PartnerTenderRequest(5, PartnerTenderStatus.ACTIVE, "catman_2", baseDateTime, sof, activeTs)
        )

        assertEquals(
            supplierTenders[4],
            PartnerTenderRequest(6, PartnerTenderStatus.COMPLETED, "catman_2", baseDateTime, sof)
        )
        assertEquals(
            supplierTenders[5],
            PartnerTenderRequest(7, PartnerTenderStatus.PROCESSING, "catman_2", baseDateTime, sof)
        )
        assertEquals(
            supplierTenders[6],
            PartnerTenderRequest(8, PartnerTenderStatus.COMPLETED, "catman_2", baseDateTime, sof)
        )

        assertEquals(
            supplierTenders[7],
            PartnerTenderRequest(9, PartnerTenderStatus.COMPLETED, "catman_2", baseDateTime, sof)
        )
        assertEquals(
            PartnerTenderRequest(
                10,
                PartnerTenderStatus.SPLIT_BY_TRUCKS,
                "catman_2",
                baseDateTime,
                samara,
                null,
                listOf(TenderTruckDTO(-1, listOf(TenderTruckSskuDTO("4242", 1))))
            ),
            supplierTenders[8]
        )

        assertEquals(
            supplierTenders[9],
            PartnerTenderRequest(11, PartnerTenderStatus.COMPLETED, "catman_2", baseDateTime, samara)
        )
        assertEquals(
            supplierTenders[10],
            PartnerTenderRequest(12, PartnerTenderStatus.WON, "catman_2", baseDateTime, samara)
        )

        assertEquals(
            supplierTenders[11],
            PartnerTenderRequest(13, PartnerTenderStatus.CANCELLED, "catman_2", baseDateTime, samara)
        )

        assertEquals(
            PartnerTenderRequest(
                14,
                PartnerTenderStatus.APPROVING,
                priceSpec = PriceSpecificationDto(
                    "spec14",
                    listOf(PriceSpecificationAxaptaSplitDto("spec1", "url1", "login1", "login1")),
                    PriceSpecificationStatus.OK,
                    listOf("010.3", "010.2"),
                    listOf(),
                    null
                ),
                catman = "catman_2",
                date = baseDate.atTime(0, 0),
                warehouseName = samara
            ), supplierTenders[12]
        )
        assertEquals(
            supplierTenders[13],
            PartnerTenderRequest(15, PartnerTenderStatus.PROCESSING, "catman_2", baseDateTime, samara)
        )
    }

    @Test
    @DbUnitDataSet(before = ["getPartnerTendersWithGroups.before.csv"])
    fun getPartnerTendersWithGroups() {
        val baseDateTime = LocalDateTime.of(2019, 3, 18, 0, 0)
        val baseDate = baseDateTime.toLocalDate()
        `when`(timeService.nowDate).thenReturn(baseDate)
        val supplierTenders = tenderService.getPartnerTenders(10)
            .sortedBy { it.demandId }
        val sof = "Софьино"
        val sofAndSamara = "Самара,Софьино"

        assertNotNull(supplierTenders)
        assertEquals(2, supplierTenders.size)

        val activeTs = LocalDateTime.parse("2021-06-24T00:00")
        assertEquals(
            supplierTenders[0],
            PartnerTenderRequest(1, PartnerTenderStatus.ACTIVE, "catman_2", baseDateTime, sof, activeTs)
        )

        assertEquals(
            supplierTenders[1],
            PartnerTenderRequest(
                6,
                PartnerTenderStatus.COMPLETED,
                "catman_2",
                baseDateTime,
                sofAndSamara)
        )
    }

    @Test
    @DbUnitDataSet(before = ["getPartnerTenders_anonSupplier.before.csv"])
    fun getPartnerTenders_withOpenTenders() {
        val supplierTenders = tenderService.getPartnerTenders(2).sortedBy { it.demandId }
        assertEquals(supplierTenders.map { it.demandId }, listOf<Long>(1, 3, 4))
        assertEquals(
            supplierTenders.map { it.status },
            listOf(PartnerTenderStatus.ACTIVE, PartnerTenderStatus.ACTIVE, PartnerTenderStatus.ACTIVE)
        )
    }

    @Test
    @DbUnitDataSet(before = ["getPartnerTenders_anonSupplier.before.csv"])
    fun getPartnerTenders_anonSupplier() {
        `when`(cabinet1PClient.getRsId(ArgumentMatchers.anyLong())).thenReturn(null)
        val supplierTenders = tenderService.getPartnerTenders(43).sortedBy { it.demandId }
        assertEquals(supplierTenders.map { it.demandId }, listOf<Long>(1, 3))
        assertEquals(
            supplierTenders.map { it.status },
            listOf(PartnerTenderStatus.ACTIVE, PartnerTenderStatus.ACTIVE)
        )
    }

    @Test
    @DbUnitDataSet(before = ["getPartnerTenders_anonSupplier.before.csv"])
    fun getPartnerTenders_answeredAnonSupplier() {
        `when`(cabinet1PClient.getRsId(ArgumentMatchers.anyLong())).thenReturn(null)
        val supplierTenders = tenderService.getPartnerTenders(42).sortedBy { it.demandId }
        assertEquals(supplierTenders.map { it.demandId }, listOf<Long>(1, 2, 3))
        assertEquals(
            supplierTenders.map { it.status },
            listOf(PartnerTenderStatus.SENT, PartnerTenderStatus.PROCESSING, PartnerTenderStatus.ACTIVE)
        )
    }

    @Test
    @DbUnitDataSet(before = ["getPartnerTenders_twoWeeksInterval.before.csv"])
    fun getPartnerTenders_twoWeeksInterval() {
        val baseDateTime = LocalDateTime.of(2019, 3, 18, 0, 0)
        val baseDate = baseDateTime.toLocalDate()
        `when`(timeService.nowDate).thenReturn(baseDate)
        val supplierTenders = tenderService.getPartnerTenders(10)
            .sortedBy { it.demandId }

        val expectedDemandIdToDate = mapOf<Long, LocalDateTime>(
            1L to baseDateTime,
            15L to baseDateTime.minusDays(13),
            18L to baseDateTime.plusDays(1)
        )

        supplierTenders.forEach {
            assertTrue(expectedDemandIdToDate.containsKey(it.demandId))
            assertEquals(expectedDemandIdToDate[it.demandId], it.date)
        }
    }

    @Test
    @DbUnitDataSet(before = ["exportResults.before.csv"])
    fun testExportResults_ssku_without_statuses() {
        mockOpenWarehouses(
            OpenWarehouse("234234", 4205),
            OpenWarehouse("235235", 4205, listOf(172, 304))
        )
        mockDeepMindClient()

        assertEquals(
            "Не нашли в Разуме статус для SupplierId 4205, SSKU 234234\\n" +
                "Не нашли в Разуме статус для SupplierId 4205, SSKU 235235",
            assertThrows<UserWarningException> {
                tenderService.generateDemandsAndCompleteIfAllSent(123, setOf(4201))
            }.message
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceTest.testGroupTendersAlreadyGrouped.before.csv"],
        after = ["TenderServiceTest.testGroupTendersAlreadyGrouped.after.csv"]
    )
    fun testGroupTendersAlreadyGrouped() {
        tenderService.groupTenders(setOf(10L, 20L, 30L, 40L, 50L))
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceTest.testGroupTendersAlreadyStarted.before.csv"]
    )
    fun testGroupTendersAlreadyStarted() {
        val exception = assertThrows<java.lang.IllegalArgumentException> {
            tenderService.groupTenders(setOf(10L, 20L, 30L))
        }
        assertEquals("Потребности с ид 10, 20 уже запущены в работу!", exception.message)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceTest.testGroupTenders.before.csv"],
        after = ["TenderServiceTest.testGroupTenders.after.csv"]
    )
    fun testGroupTenders() {
        tenderService.groupTenders(setOf(10L, 20L))
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceTest.testGroupTendersIncludeInExistsGroup.before.csv"],
        after = ["TenderServiceTest.testGroupTendersIncludeInExistsGroup.after.csv"]
    )
    fun testGroupTendersIncludeInExistsGroup() {
        tenderService.groupTenders(setOf(10L, 20L, 30L))
    }

    private fun mockDeepMindClient() {
        ReflectionTestUtils.setField(tenderService, "deepMindClient", deepMindClient)
    }

    private data class OpenWarehouse(val ssku: String, val supplierId: Int, val lockedWarehouses: List<Long> = listOf())

    private fun mockOpenWarehouses(vararg warehouses: OpenWarehouse) {
        val deliveryParams = Mockito.mock(DeliveryParams::class.java)
        val response = SearchFulfilmentSskuParamsResponse.newBuilder()
        warehouses.forEach {
            val info = MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setAvailability(SupplierOffer.Availability.ACTIVE)
                .setShopSku(it.ssku)
                .setSupplierId(it.supplierId)
            it.lockedWarehouses.forEach { w ->
                info.addWarehouseIntervalAvailabilities(
                    MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder().setWarehouseId(w).build()
                )
            }
            response.addFulfilmentInfo(info.build())
        }
        `when`(deliveryParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(response.build())
        ReflectionTestUtils.setField(tenderService, "deliveryParams", deliveryParams)
    }

    private fun mockWarehousesForExport(
        need4202: Boolean = true,
        sskus4201: List<String>? = null
    ) {
        val whs = mutableListOf(
            OpenWarehouse(sskus4201?.get(0) ?: "234234", 4201),
            OpenWarehouse(sskus4201?.get(1) ?: "235235", 4201, listOf(172)),
        )
        if (need4202) whs.add(OpenWarehouse("234234", 4202, listOf(172)))
        mockOpenWarehouses(*whs.toTypedArray())
    }
}
