package ru.yandex.market.wms.placement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import reactor.core.publisher.Mono
import ru.yandex.market.wms.common.model.enums.LocationType
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.dao.entity.Loc
import ru.yandex.market.wms.common.spring.dao.entity.LotLocId
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao
import ru.yandex.market.wms.common.spring.enums.ContainerIdType
import ru.yandex.market.wms.common.spring.service.time.WarehouseDateTimeService
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.core.response.CheckByLocAndSkuResponse
import ru.yandex.market.wms.constraints.core.response.GetRestrictedRowsResponse
import ru.yandex.market.wms.core.base.dto.RowFullness
import ru.yandex.market.wms.core.base.request.UpdateSusr7Request
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse
import ru.yandex.market.wms.core.base.response.GetIdTypesResponse
import ru.yandex.market.wms.core.base.response.GetMostPopulatedZoneResponse
import ru.yandex.market.wms.core.base.response.GetParentContainerResponse
import ru.yandex.market.wms.core.base.response.GetRowsFullnessResponse
import ru.yandex.market.wms.core.base.response.GetSerialInventoriesByIdResponse
import ru.yandex.market.wms.core.base.response.IdWithType
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.exception.BalancesDiscrepancyException
import ru.yandex.market.wms.placement.exception.EmptyOrderException
import ru.yandex.market.wms.placement.service.CoreService
import ru.yandex.market.wms.placement.service.transportation.ConveyorIdService
import ru.yandex.market.wms.placement.service.transportation.async.CancelTransportationOrdersProducer
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants
import java.math.BigDecimal
import java.time.LocalDate

class PlacementOrderControllerTest : PlacementIntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var conveyorIdService: ConveyorIdService

    @MockBean
    @Autowired
    private lateinit var cancelTransportationOrdersProducer: CancelTransportationOrdersProducer

    @MockBean
    @Autowired
    private lateinit var coreService: CoreService

    @MockBean
    @Autowired
    private lateinit var defaultJmsTemplate: JmsTemplate

    @MockBean
    @Autowired
    private lateinit var dbConfigService: DbConfigService

    @MockBean
    @Autowired
    private lateinit var warehouseDateTimeService: WarehouseDateTimeService

    @MockBean
    @Autowired
    private lateinit var serialInventoryDao: SerialInventoryDao

    @BeforeEach
    fun cleanAndSetupDefaults() {
        reset(coreClient, coreService, dbConfigService)

        whenever(coreClient.getChildContainers(anyString())).thenReturn(GetChildContainersResponse(emptyList()))
        whenever(coreClient.getParentContainer(anyString())).thenReturn(GetParentContainerResponse(null))
        whenever(coreClient.getMostPopulatedZoneByContainer(anyString())).thenReturn(GetMostPopulatedZoneResponse(null))
        whenever(constraintsClient.checkByLocAndSku(anyString(), anyList()))
            .thenReturn(CheckByLocAndSkuResponse(true, emptyList()))
        whenever(dbConfigService.getConfigAsInteger(any(), any()))
            .thenAnswer { it.arguments[1] as Int }

        whenever(warehouseDateTimeService.operationalDate).thenReturn(LocalDate.now())
        whenever(warehouseDateTimeService.shiftOperationalDate).thenReturn(LocalDate.now())
    }

    @Test
    fun `Get order - empty content`() {
        getCurrentOrder(
            expectedContent = content().string(""),
            expectedStatus = status().isNoContent
        )
    }

    @Test
    @DatabaseSetup("/service/placement/controller/current-placement-task/before/two_orders_with_serials.xml")
    @ExpectedDatabase("/service/placement/controller/current-placement-task/before/two_orders_with_serials.xml", // stays same
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Get order with ids`() {
        getCurrentOrder("service/placement/controller/current-placement-task/order_with_ids.json")
    }

    @Test
    @DatabaseSetup("/service/placement/controller/current-placement-task/before/order_with_check.xml")
    @ExpectedDatabase("/service/placement/controller/current-placement-task/before/order_with_check.xml", // stays same
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Get order with check and go to serial flag`() {
        getCurrentOrder("service/placement/controller/current-placement-task/order_with_go_to_serial_flag.json")
    }

    @Test
    @DatabaseSetup("/service/placement/controller/current-placement-task/before/in_progress_empty.xml")
    @ExpectedDatabase("/service/placement/controller/current-placement-task/before/in_progress_empty.xml", // stays same
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Get in_progress order without ids and serials`() {
        getCurrentOrder("service/placement/controller/current-placement-task/empty_in_progress.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/state/before.xml")
    fun `getPlacementOrderState with id statuses`() {
        getPlacementOrderState("ok")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/state/before.xml")
    fun `getPlacementOrderState not placed only`() {
        getPlacementOrderState("not-placed-only", notPlacedOnly = true)
    }

    @Test
    fun `Get placement order id state for not added id`() {
        getPlacementOrderIdState("id-not-added", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/get-id-state/ok/before.xml")
    fun `Get placement order id state successfully`() {
        getPlacementOrderIdState("ok", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/ok/before.xml")
    @ExpectedDatabase(value = "/controller/placement-order/add-id-to-order/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add id to new placement order`() {
        setupIdsExist(listOf("PLT101"))
        setupSerialNumbersById(id = "PLT101", serialNumbers = listOf("0000000100", "0000000101"))
        setupLocLotId("PLT101")
        addIdToPlacementOrder("ok", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/ok/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/add-id-to-order/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Add id from a conveyor container`() {
        val targetId = "PLT101"
        val parentId = "TM101"
        whenever(conveyorIdService.isConveyorId(parentId)).thenReturn(true)
        whenever(coreClient.getParentContainer(targetId)).thenReturn(GetParentContainerResponse(parentId))

        setupIdsExist(listOf(targetId))
        setupSerialNumbersById(id = targetId, serialNumbers = listOf("0000000100", "0000000101"))
        setupLocLotId(targetId)
        addIdToPlacementOrder("ok", status().isOk)
        verify(cancelTransportationOrdersProducer, times(1))
            .produceCancelRequest(anyString())
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/nesting/new-id/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/add-id-to-order/nesting/new-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Add id with nesting to new placement order`() {
        whenever(coreClient.getChildContainers(anyString()))
            .thenReturn(GetChildContainersResponse(listOf("FB110", "FB120")))

        val idWithNesting = "PLT100"
        val nestedIds = listOf("FB110", "FB120")
        setupGetIds(nestedIds, nestedIds)
        setupSerialNumbersById(id = "FB110", serialNumbers = listOf("0000000111", "0000000112"))
        setupSerialNumbersById(id = "FB120", serialNumbers = listOf("0000000121", "0000000122"))
        setupLocLotId("PLT100")
        addIdToPlacementOrder("nesting/new-id", status().isOk, idWithNesting)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/nesting/with-active-order-owner/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/add-id-to-order/nesting/with-active-order-owner/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Add id with nesting from another placement order`() {
        val idWithNesting = "PLT100"

        whenever(coreClient.getChildContainers(idWithNesting))
            .thenReturn(GetChildContainersResponse(listOf("FB110", "FB120", "FB210", "FB220")))

        val nestedIds = listOf("FB110", "FB120", "FB210", "FB220")
        setupGetIds(nestedIds, nestedIds)
        setupSerialNumbersById(id = "FB110", serialNumbers = listOf("0000000111", "0000000112"))
        setupSerialNumbersById(id = "FB120", serialNumbers = listOf("0000000121", "0000000122"))
        setupSerialNumbersById(id = "FB210", serialNumbers = listOf("0000000211", "0000000212"))
        setupSerialNumbersById(id = "FB220", serialNumbers = listOf("0000000221", "0000000222"))
        setupLocLotId("PLT100")

        addIdToPlacementOrder("nesting/with-active-order-owner", status().isOk, idWithNesting)
    }

    @Test
    fun `Add id with nested non-existent ids`() {
        val idWithNesting = "PLT100"

        whenever(coreClient.getChildContainers(idWithNesting))
            .thenReturn(GetChildContainersResponse(listOf("FB110", "FB120", "FB210", "FB220")))

        val nestedIds = listOf("FB110", "FB120", "FB210", "FB220")
        setupGetIds(nestedIds, listOf("FB110", "FB220")) // вернуть только 2 существующих НЗН

        addIdToPlacementOrder("nesting/id-not-found", status().is4xxClientError, idWithNesting)
    }

    @Test
    fun `Add id with nested ids that have balance diff`() {
        val idWithNesting = "PLT100"

        whenever(coreClient.getChildContainers(idWithNesting))
            .thenReturn(GetChildContainersResponse(listOf("FB110", "FB120", "FB210", "FB220")))

        val nestedIds = listOf("FB110", "FB120", "FB210", "FB220")
        val nestedIdsBalances = listOf("FB110", "FB220")
            .map { id ->
                LotLocId.builder()
                    .id(id)
                    .loc("SORCE_LOC")
                    .qtyPicked(BigDecimal.valueOf(1L))
                    .qtyAllocated(BigDecimal.ZERO)
                    .build()
            }
        setupGetIds(nestedIds, nestedIds)
        setupLocLotId(nestedIds, nestedIdsBalances)

        addIdToPlacementOrder("nesting/with-balance-diff", status().is4xxClientError, idWithNesting)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/in-progress/before.xml")
    @ExpectedDatabase(value = "/controller/placement-order/add-id-to-order/in-progress/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add id to placement order in progress stage`() {
        setupIdsExist(listOf("PLT100", "PLT101"))
        setupSerialNumbersById(id = "PLT100", serialNumbers = listOf("0000000100"))
        setupSerialNumbersById(id = "PLT101", serialNumbers = listOf("0000000101"))
        setupLocLotId("PLT101")
        addIdToPlacementOrder("in-progress", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/in-progress-fastzone/before.xml")
    fun `Add id to placement order in progress stage fails because of fastzone hint`() {
        setupIdsExist(listOf("PLT100", "PLT101"))
        setupSerialNumbersById(id = "PLT100", serialNumbers = listOf("0000000100"))
        setupSerialNumbersById(id = "PLT101", serialNumbers = listOf("0000000101"))
        setupLocLotId("PLT101")
        whenever(dbConfigService.getConfigAsBoolean(eq("YM_IN_PROGRESS_FASTZONE_ERROR"), anyBoolean()))
            .thenReturn(true)
        addIdToPlacementOrder("in-progress-fastzone", status().isBadRequest)
    }

    @Test
    @ExpectedDatabase(value = "/controller/placement-order/add-id-to-order/order-not-exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add id to non-existing placement order - creating new order`() {
        setupIdsExist(listOf("PLT101"))
        setupSerialNumbersById(id = "PLT101", serialNumbers = listOf("0000000100", "0000000101"))
        setupLocLotId("PLT101")

        whenever(coreClient.getMostPopulatedZoneByContainer(anyString()))
            .thenReturn(GetMostPopulatedZoneResponse("MEZ-1"))

        addIdToPlacementOrder("order-not-exists", status().isOk)

        verify(coreClient).getMostPopulatedZoneByContainer("PLT101")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/already-added/before.xml")
    @ExpectedDatabase(value = "/controller/placement-order/add-id-to-order/already-added/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add already added id to placement order`() {
        addIdToPlacementOrder("already-added", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/already-added-partly-placed/before.xml")
    @ExpectedDatabase(value = "/controller/placement-order/add-id-to-order/already-added-partly-placed/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add already added id with placed serials`() {
        addIdToPlacementOrder("already-added-partly-placed", status().isOk, id = "RCP001")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/id-not-found/before.xml")
    fun `Add non-existing id to placement order`() {
        addIdToPlacementOrder("id-not-found", status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/id-empty/before.xml")
    fun `Add empty id to placement order`() {
        setupIdsExist(listOf("PLT101"))
        setupSerialNumbersById(id = "PLT101", serialNumbers = emptyList())
        setupLocLotId("PLT101")
        addIdToPlacementOrder("id-empty", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/finished-id/before.xml")
    @ExpectedDatabase("/controller/placement-order/add-id-to-order/finished-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add id from finished order to preparing order`() {
        setupIdsExist(listOf("PLT100", "PLT101"))
        setupSerialNumbersById(id = "PLT100", serialNumbers = listOf("0000000100"))
        setupSerialNumbersById(id = "PLT101", serialNumbers = listOf("0000000101"))
        setupLocLotId("PLT100")
        setupLocLotId("PLT101")
        addIdToPlacementOrder("finished-id", status().isOk, "PLT100")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/prepare/before.xml")
    @ExpectedDatabase("/controller/placement-order/add-id-to-order/prepare/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add id from preparing to preparing order`() {
        setupIdsExist(listOf("PLT100"))
        setupSerialNumbersById(id = "PLT100", serialNumbers = listOf("0000000100"))
        setupLocLotId("PLT101")
        addIdToPlacementOrder("prepare", status().isOk, "PLT100")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/dropped-in-other-order/before.xml")
    @ExpectedDatabase("/controller/placement-order/add-id-to-order/dropped-in-other-order/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add id which was dropped in another order - create new`() {
        setupIdsExist(listOf("RCP001"))
        setupSerialNumbersById(id = "RCP001", serialNumbers = listOf("100", "101"))
        setupLocLotId("RCP001")
        addIdToPlacementOrder("dropped-in-other-order", status().isOk, id = "RCP001")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/picked/before.xml")
    fun `Add id, id is picked & reserved`() {
        setupIdsExist(listOf("PLT101"))
        setupSerialNumbersById(id = "PLT101", serialNumbers = listOf("0000000101"))
        setupLocLotId("PLT101", LotLocId.builder().id("PLT101").loc("LOC").lot("LOT")
            .qtyPicked(BigDecimal.ONE).qtyAllocated(BigDecimal.ZERO).build())
        addIdToPlacementOrder("picked", status().isBadRequest)

        setupLocLotId("PLT101", LotLocId.builder().id("PLT101").loc("LOC").lot("LOT")
            .qtyPicked(BigDecimal.ZERO).qtyAllocated(BigDecimal.ONE).build())
        addIdToPlacementOrder("picked", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/id-has-fake-uit/before.xml")
    @ExpectedDatabase("/controller/placement-order/add-id-to-order/id-has-fake-uit/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Add id which has fake uit`() {
        whenever(coreService.fakeUitExists(listOf("123"))).thenReturn(true)
        addIdToPlacementOrder("id-has-fake-uit", status().isBadRequest, "123")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/added-to-optim-order/before.xml")
    fun `Add id, bad request because id added to optimization order`() {
        setupIdsExist(listOf("RCP1"))
        setupSerialNumbersById(id = "RCP1", serialNumbers = listOf("001", "002"))
        setupLocLotId("RCP1")

        addIdToPlacementOrder("added-to-optim-order", status().isBadRequest, id = "RCP1")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/ok/before.xml")
    fun `Add id from forbidden location`() {
        whenever(dbConfigService.getConfigAsBoolean(eq(NSqlConfigKey.YM_PLACEMENT_CHECK_LOC_ENABLE), any()))
            .thenReturn(true)

        setupIdsExist(listOf("RCP1", "RCP2"))
        setupLocation(Loc.builder().loc("BUF").locationType(LocationType.PLACEMENT_BUF).build())
        setupLocation(Loc.builder().loc("1-01").locationType(LocationType.PICK).build())
        setupSerialNumbersById(id = "RCP1", serialNumbers = listOf("001", "002"))
        setupSerialNumbersById(id = "RCP2", serialNumbers = listOf("003", "004", "005"))
        setupLocLotId("RCP1")
        setupLocLotId("RCP2")
        setupSerialInventoriesByIdMultipleLocs("RCP1", listOf("001", "002"), listOf("BUF", "LOST")) // RCP1 is OK
        setupSerialInventoriesByIdMultipleLocs("RCP2", listOf("003", "004", "006"),
            listOf("BUF", "LOST", "1-01")) // RCP2 is not OK

        mockMvc.perform(post("/api/v1/order/id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("id" to "RCP1")))
            .andExpect(status().isOk)

        mockMvc.perform(post("/api/v1/order/id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("id" to "RCP2")))
            .andExpect(status().isBadRequest)

        whenever(dbConfigService.getConfigAsBoolean(eq(NSqlConfigKey.YM_PLACEMENT_CHECK_LOC_ENABLE), any()))
            .thenReturn(false)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/add-id-to-order/nesting/new-id/before.xml")
    fun `Add id with nesting failed due to forbidden location`() {
        whenever(dbConfigService.getConfigAsBoolean(eq(NSqlConfigKey.YM_PLACEMENT_CHECK_LOC_ENABLE), any()))
            .thenReturn(true)
        whenever(coreClient.getChildContainers(anyString()))
            .thenReturn(GetChildContainersResponse(listOf("FB110", "FB120")))

        val idWithNesting = "PLT100"
        val nestedIds = listOf("FB110", "FB120")
        setupGetIds(nestedIds, nestedIds)
        setupSerialNumbersById(id = "FB110", serialNumbers = listOf("0000000111", "0000000112"))
        setupSerialNumbersById(id = "FB120", serialNumbers = listOf("0000000121", "0000000122"))
        setupLocLotId("PLT100")
        setupSerialInventoriesByIdMultipleLocs("FB110", listOf("0000000111", "0000000112"), listOf("LOST", "LOST"))
        setupSerialInventoriesByIdMultipleLocs("FB120", listOf("0000000111", "0000000112"), listOf("1-01", "1-01"))
        mockMvc.perform(post("/api/v1/order/id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("id" to idWithNesting)))
            .andExpect(status().isBadRequest)

        whenever(dbConfigService.getConfigAsBoolean(eq(NSqlConfigKey.YM_PLACEMENT_CHECK_LOC_ENABLE), any()))
            .thenReturn(false)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/place-id/ok/before.xml")
    @ExpectedDatabase("/controller/placement-order/place-id/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Place id successfully`() {
        reset(defaultJmsTemplate)
        setupSerialInventoriesById(loc = "PLACEMENT", id = "123", serialNumbers = listOf("0000000100", "0000000101"))
        placeId("ok", "123", status().isOk)
        verify(defaultJmsTemplate, times(1))
            .convertAndSend(
                eq(QueueNameConstants.UPDATE_SUSR7),
                isA<UpdateSusr7Request>()
            )
    }

    @Test
    @DatabaseSetup("/controller/placement-order/place-id/ok-with-check-penultimate/before.xml")
    @ExpectedDatabase("/controller/placement-order/place-id/ok-with-check-penultimate/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Place penultimate id in checking order ok`() {
        reset(defaultJmsTemplate)
        setupSerialInventoriesById(loc = "PLACEMENT", id = "123", serialNumbers = listOf("0000000100", "0000000101"))
        placeId("ok-with-check-penultimate", "123", status().isOk)
        verify(defaultJmsTemplate, times(1))
            .convertAndSend(
                eq(QueueNameConstants.UPDATE_SUSR7),
                isA<UpdateSusr7Request>()
            )
    }

    @Test
    @DatabaseSetup("/controller/placement-order/place-id/ok-with-check-last/before.xml")
    fun `Place last id in checking order error`() {
        placeId("ok-with-check-last", "123", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/place-id/ok/before.xml")
    fun `Get last placement data`() {
        whenever(dbConfigService.getConfig(eq(NSqlConfigKey.YM_MORNING_CUTOFF_TIME), anyString()))
            .thenReturn("00:00:00")

        getLastPlacedData(1, "controller/placement-order/place-id/ok/no-placed.json")

        setupSerialInventoriesById(loc = "PLACEMENT", id = "123", serialNumbers = listOf("0000000100", "0000000101"))
        placeId("ok", "123", status().isOk)
        getLastPlacedData(1, "controller/placement-order/place-id/ok/id-placed.json")

        placeUit("1-02", "0000000102")
        getLastPlacedData(1, "controller/placement-order/place-id/ok/uit-placed.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/place-id/id-not-added/before.xml")
    fun `Place not added id`() {
        placeId("id-not-added", "123", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/place-id/id-not-added/before.xml")
    fun `Place empty id`() {
        placeId("", status().isMethodNotAllowed, status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/place-id/id-not-added/before.xml")
    fun `Place id - when ID is blank`() {
        placeId(" ", status().isBadRequest,
                jsonPath("$.wmsErrorCode").value("INVALID_IDENTITY"))
    }

    @Test
    @DatabaseSetup("/controller/placement-order/delete-id/ok/before.xml")
    @ExpectedDatabase("/controller/placement-order/delete-id/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Delete id from placement order successfully`() {
        deleteId("ok", status().isOk, expectResponseContent = false)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/delete-id/ok/before.xml")
    @ExpectedDatabase("/controller/placement-order/delete-id/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Delete id from placement order twice successfully`() {
        deleteId("ok", status().isOk, expectResponseContent = false)
        deleteId("ok", status().isOk, expectResponseContent = false)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/delete-id/id-added-to-another/immutable.xml")
    @ExpectedDatabase("/controller/placement-order/delete-id/id-added-to-another/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Delete id added to another placement order`() {
        deleteId("id-added-to-another", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/delete-id/order-in-progress/immutable.xml")
    @ExpectedDatabase("/controller/placement-order/delete-id/order-in-progress/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Delete id from oder in progress status`() {
        deleteId("order-in-progress", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/delete-all-id/ok/before.xml")
    @ExpectedDatabase("/controller/placement-order/delete-all-id/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Delete all id successfully`() {
        mockMvc.perform(delete("/api/v1/order/1/id"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/finish/ok/before.xml")
    @ExpectedDatabase("/controller/placement-order/finish/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Finish placement order successfully`() {
        finishPlacementOrder("ok", status().isOk, expectResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/finish/not-placed/before.xml")
    fun `Finish placement order with not placed id`() {
        finishPlacementOrder("not-placed", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/finish/preparing/before.xml")
    fun `Finish preparing placement order`() {
        finishPlacementOrder("preparing", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/drop-id/id-not-in-order/before.xml")
    fun `Drop id, id not in order`() {
        dropId("id-not-in-order", "1-01", "666", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/drop-id/partly-placed/before.xml")
    @ExpectedDatabase("/controller/placement-order/drop-id/partly-placed/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Drop partly placed id success`() {
        dropId("partly-placed", "1-01", "110", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/drop-id/not-placed/before.xml")
    @ExpectedDatabase("/controller/placement-order/drop-id/not-placed/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Drop not placed id success`() {
        dropId("not-placed", "1-01", "120", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/drop-id/invalid-id-status/before.xml")
    fun `Drop id with invalid status`() {
        dropId("invalid-id-status", "1-01", "120", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/happy-path/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `The order start changes the status to IN_PROGRESS`() {
        setupIdsExist(listOf("PLT101", "PLACEMENT"))
        setupSerialNumbersById("PLT110", listOf("0000000111", "0000000112"))
        setupSerialNumbersById("PLT120", listOf("0000000121", "0000000122", "0000000123"))
        startPlacementOrder("100", "happy-path", status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/happy-path-w-check/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/happy-path-w-check/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `The order start changes the status to IN_PROGRESS with check flag`() {
        setupIdsExist(listOf("PLT1031", "PLT1032", "PLACEMENT"))
        setupSerialNumbersById("PLT1031", listOf("0000000131"))
        setupSerialNumbersById("PLT1032", listOf("0000000132"))

        whenever(dbConfigService.getConfigAsDouble(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_PERCENT"), anyDouble()))
            .thenReturn(26.0)
        whenever(dbConfigService.getConfigAsInteger(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_MIN_INTERVAL"), anyInt()))
            .thenReturn(2)

        startPlacementOrder("103", "happy-path-w-check", status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/happy-path-w-check/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/happy-path-w-check/after-no-check.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `The order start changes the status to IN_PROGRESS with check flag not set 1`() {
        setupIdsExist(listOf("PLT1031", "PLT1032", "PLACEMENT"))
        setupSerialNumbersById("PLT1031", listOf("0000000131"))
        setupSerialNumbersById("PLT1032", listOf("0000000132"))

        whenever(dbConfigService.getConfigAsDouble(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_PERCENT"), anyDouble()))
            .thenReturn(24.0)
        whenever(dbConfigService.getConfigAsInteger(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_MIN_INTERVAL"), anyInt()))
            .thenReturn(2)

        startPlacementOrder("103", "happy-path-w-check", status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/happy-path-w-check/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/happy-path-w-check/after-no-check.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `The order start changes the status to IN_PROGRESS with check flag not set 2`() {
        setupIdsExist(listOf("PLT1031", "PLT1032", "PLACEMENT"))
        setupSerialNumbersById("PLT1031", listOf("0000000131"))
        setupSerialNumbersById("PLT1032", listOf("0000000132"))

        whenever(dbConfigService.getConfigAsDouble(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_PERCENT"), anyDouble()))
            .thenReturn(0.0)
        whenever(dbConfigService.getConfigAsInteger(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_MIN_INTERVAL"), anyInt()))
            .thenReturn(2)

        startPlacementOrder("103", "happy-path-w-check", status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/happy-path-w-check/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/happy-path-w-check/after-no-check.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `The order start changes the status to IN_PROGRESS with check flag not set 3`() {
        setupIdsExist(listOf("PLT1031", "PLT1032", "PLACEMENT"))
        setupSerialNumbersById("PLT1031", listOf("0000000131"))
        setupSerialNumbersById("PLT1032", listOf("0000000132"))

        whenever(dbConfigService.getConfigAsDouble(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_PERCENT"), anyDouble()))
            .thenReturn(26.0)
        whenever(dbConfigService.getConfigAsInteger(eq("YM_PLACEMENT_CHECK_PLACEMENT_ORDER_MIN_INTERVAL"), anyInt()))
            .thenReturn(3)

        startPlacementOrder("103", "happy-path-w-check", status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/happy-path/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `The order changes the status to IN_PROGRESS twice, no error is expected`() {
        setupIdsExist(listOf("PLT101", "PLACEMENT"))
        setupSerialNumbersById("PLT110", listOf("0000000111", "0000000112"))
        setupSerialNumbersById("PLT120", listOf("0000000121", "0000000122", "0000000123"))
        startPlacementOrder("100", "happy-path", status().is2xxSuccessful)
        startPlacementOrder("100", "happy-path", status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/recommend-rows/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/recommend-rows/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Start order with recommendation rows`() {
        val ids = listOf("RCP01", "RCP02", "RCP03")
        setupIdsExist(ids)
        setupSerialNumbersById("RCP01", listOf("001"))
        setupSerialNumbersById("RCP02", listOf("002"))
        setupSerialNumbersById("RCP03", listOf("003"))

        val row = RowFullness("A1-01", 1, 1, 0.0, 10, ContainerIdType.RCP)
        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, ContainerIdType.RCP) })))
        whenever(coreClient.getRowFullnessMono(any(), any()))
            .thenReturn(Mono.just(GetRowsFullnessResponse(listOf(row))))
        whenever(coreClient.getSerialInventoriesByIdMono(any()))
            .thenReturn(Mono.just(GetSerialInventoriesByIdResponse(emptyList())))
        whenever(constraintsClient.getRestrictedRowsByZoneAndSku(any(), any()))
            .thenReturn(Mono.just(GetRestrictedRowsResponse (putawayzone = "MEZ-1", rows = listOf())))
        whenever(dbConfigService.getConfigAsStringList(NSqlConfigKey.YM_PLACEMENT_ROW_RECOM_ZONES))
            .thenReturn(listOf("MEZ-1"))

        startPlacementOrder("100", "recommend-rows", status().is2xxSuccessful)

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono("MEZ-1", ContainerIdType.RCP)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Throw exceptions when there are discrepancies`() {
        setupIdsExist(listOf("PLT101", "PLACEMENT"))
        setupSerialNumbersById("PLT110", listOf("0000000111", "0000000112", "0000000113"))
        setupSerialNumbersById("PLT120", listOf("0000000121", "0000000123"))
        val result: MvcResult = startPlacementOrder("100", "discrepancy", status().is4xxClientError)
        Assertions.assertTrue(
            result.resolvedException is BalancesDiscrepancyException,
            "Expected: " + BalancesDiscrepancyException::class.simpleName + "; Actual: " + result.resolvedException::class.simpleName
        )
    }

    @Test
    @DatabaseSetup("/controller/placement-order/start/empty-order/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-order/start/empty-order/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Throw exceptions when order is empty`() {
        setupIdsExist(listOf("PLT201", "PLACEMENT"))
        setupSerialNumbersById("PLT210", listOf("0000000211"))
        val result: MvcResult = startPlacementOrder("100", "empty-order", status().isConflict)
        Assertions.assertTrue(
            result.resolvedException is EmptyOrderException,
            "Expected: " + EmptyOrderException::class.simpleName + "; Actual: " + result.resolvedException::class.simpleName
        )
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/all-id-is-placed/all-id-is-placed.xml")
    @ExpectedDatabase("/controller/placement-order/lost/all-id-is-placed/all-id-is-placed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, all IDs are placed`() {
        moveToLost(status().isOk, "all-id-is-placed")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/empty-not-placed-ids/before.xml")
    @ExpectedDatabase("/controller/placement-order/lost/empty-not-placed-ids/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, empty NOT_PLACED ID`() {
        moveToLost(status().isOk, "empty-not-placed-ids")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/partly-placed/before.xml")
    @ExpectedDatabase("/controller/placement-order/lost/partly-placed/after_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, PARTLY_PLACED with NOT_PLACED serials`() {
        mockMoveBalances("123", listOf("0000000102", "0000000103"),2)
        moveToLost(status().isOk, "partly-placed", "response1.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/partly-placed/before.xml")
    @ExpectedDatabase("/controller/placement-order/lost/partly-placed/after_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, PARTLY_PLACED with NOT_PLACED serials 2`() {
        mockMoveBalances("123", listOf("0000000102"),2)
        moveToLost(status().isOk, "partly-placed", "response2.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/partly-placed/before.xml")
    @ExpectedDatabase("/controller/placement-order/lost/partly-placed/after_3.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, nothing to move in SCPRD_SERIAL_INVENTORY`() {
        whenever(coreService.moveIdToLost(anyString(), anyInt(), any())).thenReturn(emptyList())
        moveToLost(status().isOk, "partly-placed", "response3.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/whole-id-not-placed/before.xml")
    @ExpectedDatabase("/controller/placement-order/lost/whole-id-not-placed/after_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, whole ID to LOST`() {
        mockMoveBalances("123", listOf("0000000102", "0000000101"),2)
        moveToLost(status().isOk, "whole-id-not-placed", "response1.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/whole-id-not-placed/before.xml")
    @ExpectedDatabase("/controller/placement-order/lost/whole-id-not-placed/after_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, whole ID to LOST 2`() {
        mockMoveBalances("123", listOf("0000000101"),2)
        moveToLost(status().isOk, "whole-id-not-placed", "response2.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/lost/before-and-after/before-and-after.xml")
    @ExpectedDatabase("/controller/placement-order/lost/before-and-after/before-and-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move to lost, exception during moving to lost in SCPRD`() {
        setupSerialInventoriesById(loc = "PLACEMENT", id = "321", serialNumbers = listOf("0000000101", "0000000102"))
        whenever(coreService.moveIdToLost(anyString(), anyInt(), any()))
            .thenThrow(RuntimeException("Exception during move serial inventories to lost"))
        moveToLost(status().is5xxServerError)
    }

    private fun getCurrentOrder(jsonPath: String) =
        getCurrentOrder(
            expectedContent = content().json(getFileContent(jsonPath), true),
            expectedStatus = status().isOk
        )

    private fun getCurrentOrder(expectedContent : ResultMatcher, expectedStatus: ResultMatcher = status().isOk) =
        mockMvc.perform(get("/api/v1/order"))
            .andExpect(expectedStatus)
            .andExpect(expectedContent)
            .andReturn()

    private fun getPlacementOrderState(testCase: String, notPlacedOnly: Boolean = false) =
        mockMvc.perform(get("/api/v1/order/100/id?notPlacedOnly=$notPlacedOnly"))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("controller/placement-order/state/$testCase/response.json"),true))

    private fun getPlacementOrderIdState(testCase: String, expectedStatus: ResultMatcher) =
        mockMvc.perform(get("/api/v1/order/1/id/RCP123"))
            .andExpect(expectedStatus)
            .andExpect(content().json(getFileContent("controller/placement-order/get-id-state/$testCase/response.json"),true))

    private fun addIdToPlacementOrder(
        testCase: String,
        expectedStatus: ResultMatcher,
        id: String = "PLT101",
        bufferLoc: String? = null,
    ) = mockMvc.perform(post("/api/v1/order/id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("id" to id, "bufferLoc" to bufferLoc)))
            .andExpect(expectedStatus)
            .andExpect(content().json(getFileContent("controller/placement-order/add-id-to-order/$testCase/response.json"), false))

    private fun placeId(testCase: String, id: String, expectedStatus: ResultMatcher) =
        mockMvc.perform(post("/api/v1/order/1/id/$id/place")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("loc" to "1-01")))
            .andExpect(expectedStatus)
            .andExpect(content().json(getFileContent("controller/placement-order/place-id/$testCase/response.json"),true))

    private fun placeId(id: String, expectedStatus: ResultMatcher, expectedResult: ResultMatcher) {
        mockMvc.perform(post("/api/v1/order/1/id/$id/place")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("loc" to "1-01")))
                .andExpect(expectedStatus)
                .andExpect(expectedResult)
    }

    private fun deleteId(testCase: String, expectedStatus: ResultMatcher, expectResponseContent: Boolean = true) {
        val resultActions = mockMvc.perform(delete("/api/v1/order/1/id/123"))
            .andExpect(expectedStatus)
        if (expectResponseContent) {
            resultActions
                .andExpect(content().json(getFileContent("controller/placement-order/delete-id/$testCase/response.json"),true))
        }
    }

    private fun startPlacementOrder(orderKey: String, testCase: String, expectedStatus: ResultMatcher): MvcResult {
        return mockMvc.perform(
            put("/api/v1/order/$orderKey/start")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(expectedStatus)
            .andExpect(
                content().json(
                    getFileContent("controller/placement-order/start/$testCase/response.json"), true
                )
            ).andReturn()
    }

    private fun finishPlacementOrder(testCase: String, expectedStatus: ResultMatcher, expectResponse: Boolean = true) {
        val resultActions =
            mockMvc.perform(put("/api/v1/order/1/finish"))
                .andExpect(expectedStatus)
        if (expectResponse) {
            resultActions
                .andExpect(content().json(getFileContent("controller/placement-order/finish/$testCase/response.json"), true))
        }
    }

    private fun dropId(testCase: String, loc: String, id: String, expectedStatus: ResultMatcher) {
        mockMvc.perform(post("/api/v1/order/1/id/$id/drop")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("loc" to loc)))
            .andExpect(expectedStatus)
            .andExpect(content().json(getFileContent("controller/placement-order/drop-id/$testCase/response.json")))
    }

    private fun moveToLost(status: ResultMatcher, responseJsonDir: String? = null,
                           responseJsonFile: String? = "response.json") {
        val response = mockMvc.perform(put("/api/v1/order/2/lost"))
            .andExpect(status)
        if (responseJsonDir != null) {
            response.andExpect(content().json(
                getFileContent("controller/placement-order/lost/$responseJsonDir/$responseJsonFile"), true))
        }
    }

    private fun getLastPlacedData(orderKey: Int, responsePath: String) =
        mockMvc.perform(get("/api/v1/order/$orderKey/last-place"))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent(responsePath)))

    private fun placeUit(loc: String, uit: String) {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(setOf(uit)))
            .thenReturn(buildSerialInventories(id = "124", serialNumbers = listOf(uit)))

        val request = post("/api/v1/order/1/uit/place")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("{'loc': '$loc'," +
                " 'uitList': ['${uit}']}"))
        mockMvc.perform(request)
            .andExpect(status().isOk)
    }


    private fun setupIdsExist(ids: List<String>) {
        for (id in ids) {
            whenever(coreService.isIdExists(id)).thenReturn(true)
        }
    }

    private fun setupGetIds(requestIds: List<String>, responseIds: List<String>) {
        whenever(coreService.getIds(requestIds)).thenReturn(responseIds)
    }

    private fun setupLocLotId(id: String) {
        setupLocLotId(id,
            LotLocId.builder()
                .id(id)
                .loc("SORCE_LOC")
                .qtyPicked(BigDecimal.ZERO)
                .qtyAllocated(BigDecimal.ZERO)
                .build()
        )
    }

    private fun setupLocLotId(id: String, lli: LotLocId) {
        whenever(coreService.getLotLocIdById(id)).thenReturn(listOf(lli))
    }

    private fun setupLocLotId(id: List<String>, lli: List<LotLocId>) {
        whenever(coreService.getLotLocIdById(id)).thenReturn(lli)
    }

    private fun setupSerialNumbersById(id: String, serialNumbers: List<String>) {
        setupSerialInventoriesById(id = id, serialNumbers = serialNumbers)
    }

    private fun setupSerialInventoriesById(
        lot: String = "", loc: String = "", id: String = "", serialNumbers : List<String>
    ) {
        whenever(coreService.getSerialInventoriesById(id))
            .thenReturn(buildSerialInventories(lot, loc, id, serialNumbers))
    }

    private fun setupLocation(loc: Loc) {
        whenever(coreService.getLocOrNull(loc.loc)).thenReturn(loc)
    }

    private fun mockMoveBalances(id: String, serials: List<String>, orderKey: Int) {
        whenever(coreService.moveIdToLost(eq(id), eq(orderKey), any()))
            .thenReturn(serials.map { SerialInventory.builder().serialNumber(it).build() })
    }

    private fun buildSerialInventories(
        lot: String = "", loc: String = "PLACEMENT", id: String = "", serialNumbers : List<String>
    ) : List<SerialInventory> {
        return serialNumbers.map {
            SerialInventory.builder()
                .sku("ROV123")
                .storerKey("465852")
                .serialNumber(it)
                .lot(lot)
                .loc(loc)
                .id(id)
                .quantity(BigDecimal.ONE)
                .build()
        }
    }

    private fun setupSerialInventoriesByIdMultipleLocs(id: String, serialNumbers: List<String>, locs: List<String>) {
        whenever(coreService.getSerialInventoriesById(id))
            .thenReturn(serialNumbers.zip(locs).map {
                SerialInventory.builder()
                    .sku("ROV123")
                    .storerKey("465852")
                    .serialNumber(it.first)
                    .lot("")
                    .loc(it.second)
                    .id(id)
                    .quantity(BigDecimal.ONE)
                    .build()
            })
    }
}
