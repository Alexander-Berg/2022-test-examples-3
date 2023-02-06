package ru.yandex.market.wms.dimensionmanagement.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientResponseException
import ru.yandex.market.wms.common.spring.dto.BalanceDto
import ru.yandex.market.wms.common.spring.enums.PutawayZoneType
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.core.base.dto.BalancesDto
import ru.yandex.market.wms.core.base.dto.ChildIdDto
import ru.yandex.market.wms.core.base.dto.ConveyorZoneType
import ru.yandex.market.wms.core.base.request.CreateIdRequest
import ru.yandex.market.wms.core.base.request.MoveIdToLocationRequest
import ru.yandex.market.wms.core.base.request.UpdateIdFillingStatusRequest
import ru.yandex.market.wms.core.base.request.ZoneSelectionRequest
import ru.yandex.market.wms.core.base.response.GetMeasureBuffersResponse
import ru.yandex.market.wms.core.base.response.GetParentPossibleUsageResponse
import ru.yandex.market.wms.core.base.response.Location
import ru.yandex.market.wms.core.base.response.LocationsSelectionResponse
import ru.yandex.market.wms.core.base.response.ZoneSelectionResponse
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.client.exception.ContainerCannotBeUsedAsChildException
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import ru.yandex.market.wms.receiving.client.ReceivingClient
import ru.yandex.market.wms.shared.libs.utils.JsonUtil.readValue
import ru.yandex.market.wms.transportation.client.TransportationClient
import ru.yandex.market.wms.transportation.core.model.response.Resource
import ru.yandex.market.wms.transportation.core.model.response.TransportOrderResourceContent
import java.util.Optional

class ContainerControllerTest : DimensionManagementIntegrationTest() {

    @Autowired
    @MockBean
    private lateinit var coreClient: CoreClient

    @Autowired
    @SpyBean
    private lateinit var receivingClient: ReceivingClient

    @Autowired
    @SpyBean
    private lateinit var transportationClient: TransportationClient

    @BeforeEach
    fun setUp() {
        Mockito.reset(coreClient)
        Mockito.reset(receivingClient)
        Mockito.reset(transportationClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/get-info/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/get-info/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getInfoAboutEmptyContainer() {
        val containerId = "TM00001"
        Mockito.doReturn(BalancesDto()).whenever(coreClient).getBalancesById(containerId)

        testGetRequest(
            "/api/v1/container/$containerId/info",
            "controller/container-controller/get-info/empty-response.json",
            status().isOk
        )
        verify(coreClient).getBalancesById(containerId)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/get-info/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/get-info/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getInfoAboutContainerWithoutBalances() {
        val containerId = "TM00001"
        Mockito.doReturn(BalancesDto.Builder()
            .childIds(setOf(ChildIdDto("TM00005", null)))
            .build()
        ).whenever(coreClient).getBalancesById(containerId)

        testGetRequest(
            "/api/v1/container/$containerId/info",
            "controller/container-controller/get-info/without-balances-response.json",
            status().isOk
        )
        verify(coreClient).getBalancesById(containerId)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/get-info/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/get-info/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getInfoAboutNonEmptyContainer() {
        val containerId = "TM00002"
        Mockito.doReturn(BalancesDto(setOf(BalanceDto.builder().build())))
            .whenever(coreClient).getBalancesById(containerId)

        testGetRequest(
            "/api/v1/container/$containerId/info",
            "controller/container-controller/get-info/non-empty-response.json",
            status().isOk
        )
        verify(coreClient).getBalancesById(containerId)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/get-info/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/get-info/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getInfoWhenContainerNotFound() {
        val containerId = "TM00003"
        testGetRequest(
            "/api/v1/container/$containerId/info",
            "controller/container-controller/get-info/not-found-response.json",
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/assign-happy-path/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignToStationHappyPath() {
        val containerId = "TM00004"
        whenever(coreClient.getBalancesById(containerId)).thenReturn(BalancesDto())
        whenever(coreClient.parentPossibleUsage(containerId)).thenReturn(
            GetParentPossibleUsageResponse(
                suitableForId = true,
                suitableForIdAfterClean = true,
                suitableForSerial = true,
                suitableForSerialAfterClean = true,
                nestedIdCount = 0,
                nestedSerialCount = 0
            )
        )

        testRequest(
            "/api/v1/container/assign-to-station",
            HttpMethod.PUT,
            "controller/container-controller/assign-happy-path/request.json",
            "controller/container-controller/assign-happy-path/response.json",
            status().isOk
        )

        val expectedRequest = MoveIdToLocationRequest(containerId, "LOC2")
        verify(coreClient).getBalancesById(containerId)
        verify(coreClient).parentPossibleUsage(containerId)
        verify(coreClient).moveIdToLoc(expectedRequest)
        verify(coreClient).updateIdFillingStatus(UpdateIdFillingStatusRequest(containerId, "EMPTY"))
        verify(coreClient).updateIdFillingStatus(UpdateIdFillingStatusRequest(containerId, "AFTER_MEASURE"))
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/assign-happy-path/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignToStationHappyPathWhenContainerCannotBeUsedAsParent() {
        val containerId = "TM00004"
        whenever(coreClient.getBalancesById(containerId)).thenReturn(BalancesDto())
        whenever(coreClient.parentPossibleUsage(containerId)).thenReturn(
            GetParentPossibleUsageResponse(
                suitableForId = false,
                suitableForIdAfterClean = false,
                suitableForSerial = true,
                suitableForSerialAfterClean = true,
                nestedIdCount = 0,
                nestedSerialCount = 0
            )
        )

        testRequest(
            "/api/v1/container/assign-to-station",
            HttpMethod.PUT,
            "controller/container-controller/assign-happy-path/request.json",
            "controller/container-controller/assign-happy-path/cannot-be-used-as-parent-response.json",
            status().isOk
        )

        val expectedRequest = MoveIdToLocationRequest(containerId, "LOC2")
        verify(coreClient).getBalancesById(containerId)
        verify(coreClient).parentPossibleUsage(containerId)
        verify(coreClient).moveIdToLoc(expectedRequest)
        verify(coreClient).updateIdFillingStatus(UpdateIdFillingStatusRequest(containerId, "EMPTY"))
        verify(coreClient).updateIdFillingStatus(UpdateIdFillingStatusRequest(containerId, "AFTER_MEASURE"))
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignToStationAlreadyAssignedContainer() {
        testRequest(
            "/api/v1/container/assign-to-station",
            HttpMethod.PUT,
            "controller/container-controller/assign-already-assigned/request.json",
            "controller/container-controller/assign-already-assigned/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignToStationContainerWhichCannotBeUsedAsRoot() {
        testRequest(
            "/api/v1/container/assign-to-station",
            HttpMethod.PUT,
            "controller/container-controller/assign-cannot-be-used-as-root/request.json",
            "controller/container-controller/assign-cannot-be-used-as-root/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignToStationNotEmptyContainer() {
        Mockito.doReturn(
            BalancesDto(
                items = setOf(BalanceDto.builder().build())
            )
        ).`when`(coreClient).getBalancesById(anyString())

        testRequest(
            "/api/v1/container/assign-to-station",
            HttpMethod.PUT,
            "controller/container-controller/assign-not-empty-container/request.json",
            "controller/container-controller/assign-not-empty-container/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    fun assignToStationValidContainerDoesntExist() {
        val containerId = "TM00004"
        whenever(coreClient.getBalancesById(containerId)).thenReturn(BalancesDto())
        whenever(coreClient.parentPossibleUsage(containerId)).thenReturn(
            GetParentPossibleUsageResponse(
                suitableForId = true,
                suitableForIdAfterClean = true,
                suitableForSerial = true,
                suitableForSerialAfterClean = true,
                nestedIdCount = 0,
                nestedSerialCount = 0
            )
        )

        testRequest(
            "/api/v1/container/assign-to-station",
            HttpMethod.PUT,
            "controller/container-controller/assign-happy-path/request.json",
            "controller/container-controller/assign-happy-path/response.json",
            status().isOk
        )

        verify(coreClient).getBalancesById(containerId)
        verify(coreClient).parentPossibleUsage(containerId)
        verify(coreClient).createIdIfNotExists(CreateIdRequest(containerId))
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    fun assignToStationInvalidContainerDoesntExist() {
        Mockito.doReturn(BalancesDto()).`when`(coreClient).getBalancesById(anyString())

        testRequest(
            "/api/v1/container/assign-to-station",
            HttpMethod.PUT,
            "controller/container-controller/assign-not-valid-non-existent-container/request.json",
            "controller/container-controller/assign-not-valid-non-existent-container/response.json",
            status().isBadRequest
        )

        verify(coreClient, times(0)).createIdIfNotExists(CreateIdRequest("Tm00004"))
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-container-happy-path/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeTargetChildContainerHappyPath() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")
        Mockito.doReturn(null).whenever(coreClient).createIdIfNotExists(CreateIdRequest("TM00004"))
        Mockito.doReturn(BalancesDto()).whenever(coreClient).getBalancesById("TM00004")
        Mockito.doNothing().whenever(coreClient).postCreateSingleNesting("TM00004", "TM00003", null)

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-container-happy-path/request.json",
            null,
            status().isOk
        )

        verify(coreClient).checkChild("TM00004")
        verify(coreClient).createIdIfNotExists(CreateIdRequest("TM00004"))
        verify(coreClient).getBalancesById("TM00004")
        verify(coreClient).postCreateSingleNesting("TM00004", "TM00003", null)
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-to-existing-child-container/before.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-to-existing-child-container/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeTargetChildContainerToAlreadyExistingContainer() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-to-existing-child-container/request.json",
            null,
            status().isOk
        )

        verify(coreClient).checkChild("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-to-non-child-container/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-to-non-child-container/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeTargetChildContainerToContainerThatCannotBeChild() {
        Mockito.doThrow(ContainerCannotBeUsedAsChildException("Unsuitable type of child container TM00004"))
            .whenever(coreClient).checkChild("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-to-non-child-container/request.json",
            "controller/container-controller/change-to-non-child-container/response.json",
            status().isBadRequest
        )

        verify(coreClient).checkChild("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-child-when-station-not-exist/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-when-station-not-exist/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeTargetChildContainerWhenStationNotExist() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-when-station-not-exist/request.json",
            "controller/container-controller/change-child-when-station-not-exist/response.json",
            status().isNotFound
        )

        verify(coreClient).checkChild("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-child-when-active-container-not-found/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-when-active-container-not-found/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeTargetChildContainerWhenActiveContainerNotFound() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-when-active-container-not-found/request.json",
            "controller/container-controller/change-child-when-active-container-not-found/response.json",
            status().isNotFound
        )

        verify(coreClient).checkChild("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-child-assigned-to-another-station/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-assigned-to-another-station/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun changeTargetChildContainerWhenContainerAlreadyAssignedToAnotherStation() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-assigned-to-another-station/request.json",
            "controller/container-controller/change-child-assigned-to-another-station/response.json",
            status().isBadRequest
        )

        verify(coreClient).checkChild("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-child-not-linked-to-parent/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-not-linked-to-parent/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun changeTargetChildContainerWhenContainerNotLinkedToParentContainer() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-not-linked-to-parent/request.json",
            "controller/container-controller/change-child-not-linked-to-parent/response.json",
            status().isBadRequest
        )

        verify(coreClient).checkChild("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-child-linked-to-inactive-parent/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-linked-to-inactive-parent/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun changeTargetChildContainerWhenContainerLinkedToInactiveParentContainer() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-linked-to-inactive-parent/request.json",
            "controller/container-controller/change-child-linked-to-inactive-parent/response.json",
            status().isBadRequest
        )

        verify(coreClient).checkChild("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-child-not-empty/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-not-empty/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun changeTargetChildContainerWhenContainerIsNotEmpty() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")
        Mockito.doReturn(null).whenever(coreClient)
            .createIdIfNotExists(CreateIdRequest("TM00004"))
        Mockito.doReturn(BalancesDto(items = setOf(BalanceDto.builder().build())))
            .whenever(coreClient).getBalancesById("TM00004")

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-not-empty/request.json",
            "controller/container-controller/change-child-not-empty/response.json",
            status().isBadRequest
        )

        verify(coreClient).checkChild("TM00004")
        verify(coreClient).createIdIfNotExists(CreateIdRequest("TM00004"))
        verify(coreClient).getBalancesById("TM00004")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/change-child-already-nested/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/change-child-already-nested/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun changeTargetChildContainerWhenContainerIsAlreadyNestedToAnotherParent() {
        Mockito.doNothing().whenever(coreClient).checkChild("TM00004")
        Mockito.doReturn(null).whenever(coreClient).createIdIfNotExists(CreateIdRequest("TM00004"))
        Mockito.doReturn(BalancesDto()).whenever(coreClient).getBalancesById("TM00004")
        Mockito.doThrow(
            WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request from POST http://core/nesting/createNesting",
                HttpHeaders(),
                byteArrayOf(),
                null
            )
        ).whenever(coreClient).postCreateSingleNesting("TM00004", "TM00003", null)

        testRequest(
            "/api/v1/container/change-target-child-container",
            HttpMethod.PUT,
            "controller/container-controller/change-child-already-nested/request.json",
            "controller/container-controller/change-child-already-nested/response.json",
            status().isBadRequest
        )

        verify(coreClient).checkChild("TM00004")
        verify(coreClient).createIdIfNotExists(CreateIdRequest("TM00004"))
        verify(coreClient).getBalancesById("TM00004")
        verify(coreClient).postCreateSingleNesting("TM00004", "TM00003", null)
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-container-using-conveyor/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeContainerUsingConveyor() {
        val dataSet = generateTestDataSet("TM00003")
        val responseBody = getTransportationResponse(
            "controller/container-controller/immutable-transportation-request.json")

        Mockito.doReturn(responseBody).`when`(transportationClient).createTransportOrder(any())
        Mockito.doReturn(ZoneSelectionResponse("TRANSIT-B"))
            .whenever(coreClient).selectZone(
                ZoneSelectionRequest(ConveyorZoneType.FOURTH_FLOOR, dataSet.containerId, false)
            )

        testRequest(
            "/api/v1/container/close",
            HttpMethod.PUT,
            "controller/container-controller/close-container-using-conveyor/request.json",
            "controller/container-controller/close-container-using-conveyor/response.json",
            status().isOk
        )

        verify(transportationClient).createTransportOrder(any())
    }

    @Test
    @DatabaseSetup("/controller/container-controller/close-container-with-children/before.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-container-with-children/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeContainerUsingConveyorWithChildren() {
        val dataSet = generateTestDataSet("TM00003")
        val responseBody = getTransportationResponse(
            "controller/container-controller/immutable-transportation-request.json")

        Mockito.doReturn(responseBody).`when`(transportationClient).createTransportOrder(any())
        Mockito.doReturn(ZoneSelectionResponse("TRANSIT-B"))
            .whenever(coreClient).selectZone(
                ZoneSelectionRequest(ConveyorZoneType.FIRST_FLOOR, dataSet.containerId, false)
            )

        testRequest(
            "/api/v1/container/close",
            HttpMethod.PUT,
            "controller/container-controller/close-container-with-children/request.json",
            "controller/container-controller/close-container-with-children/response.json",
            status().isOk
        )

        verify(transportationClient).createTransportOrder(any())
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-container-to-other-zone-type/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeContainerUsingConveyorOtherConveyorZone() {
        val dataSet = generateTestDataSet("TM00003")
        val responseBody = getTransportationResponse(
            "controller/container-controller/immutable-transportation-request.json")

        Mockito.doReturn(responseBody).`when`(transportationClient).createTransportOrder(any())
        Mockito.doReturn(ZoneSelectionResponse("TRANSIT-B"))
            .whenever(coreClient).selectZone(
                ZoneSelectionRequest(null, dataSet.containerId, false)
            )

        testRequest(
            "/api/v1/container/close",
            HttpMethod.PUT,
            "controller/container-controller/close-container-to-other-zone-type/request.json",
            "controller/container-controller/close-container-to-other-zone-type/response.json",
            status().isOk
        )

        verify(transportationClient).createTransportOrder(any())
    }

    @Test
    @DatabaseSetup("/controller/container-controller/close-container-manually/before.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-container-manually/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeContainerUsingManualTransportOrder() {
        val dataSet = generateTestDataSet("L00003")
        val responseBody = getTransportationResponse(
            "controller/container-controller/immutable-transportation-request.json")

        Mockito.doReturn(responseBody).`when`(transportationClient).createTransportOrder(any())
        Mockito.doReturn(GetMeasureBuffersResponse(
            buffers = listOf(
                Location(dataSet.firstMeasureBuffer, dataSet.measurePutawayZone),
                Location(dataSet.secondMeasureBuffer, null)
            )
        )).whenever(coreClient).getMeasurementBuffers(dataSet.loc, PutawayZoneType.DIMENSIONS_OUTBOUND)
        Mockito.doReturn(LocationsSelectionResponse(listOf("TRANSIT-B")))
            .whenever(coreClient).selectTransportLocations(ConveyorZoneType.FIRST_FLOOR)

        testRequest(
            "/api/v1/container/close",
            HttpMethod.PUT,
            "controller/container-controller/close-container-manually/request.json",
            "controller/container-controller/close-container-manually/response.json",
            status().isOk
        )
        verify(coreClient).getMeasurementBuffers(dataSet.loc, PutawayZoneType.DIMENSIONS_OUTBOUND)
        verify(transportationClient).createTransportOrder(any())
    }

    @Test
    @DatabaseSetup("/controller/container-controller/close-child/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-child/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeContainerWhenContainerIsChild() {
        testRequest(
            "/api/v1/container/close",
            HttpMethod.PUT,
            "controller/container-controller/close-child/request.json",
            "controller/container-controller/close-child/response.json",
            status().isBadRequest
        )
        verifyNoMoreInteractions(coreClient)
        verifyNoMoreInteractions(receivingClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeContainerWhenContainerIsInactive() {
        testRequest(
            "/api/v1/container/close",
            HttpMethod.PUT,
            "controller/container-controller/close-inactive/request.json",
            "controller/container-controller/close-inactive/response.json",
            status().isBadRequest
        )
        verifyNoMoreInteractions(coreClient)
        verifyNoMoreInteractions(receivingClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-empty/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeEmptyContainer() {
        val containerId = "TM00001"
        Mockito.doReturn(BalancesDto()).whenever(coreClient).getBalancesById(containerId)

        testRequest(
            "/api/v1/container/$containerId/close-empty",
            HttpMethod.PUT,
            null,
            null,
            status().isOk
        )
        verify(coreClient).getBalancesById(containerId)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/close-empty-with-empty-child/before.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-empty-with-empty-child/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeEmptyContainerWithEmptyChild() {
        val containerId = "TM00001"
        Mockito.doReturn(BalancesDto.Builder()
            .childIds(setOf(ChildIdDto("TM00002", null)))
            .build()
        ).whenever(coreClient).getBalancesById(containerId)

        testRequest(
            "/api/v1/container/$containerId/close-empty",
            HttpMethod.PUT,
            null,
            null,
            status().isOk
        )
        verify(coreClient).getBalancesById(containerId)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeEmptyContainerWhenBalancesExistInContainer() {
        val containerId = "TM00001"
        Mockito.doReturn(BalancesDto(setOf(BalanceDto.builder().build())))
            .whenever(coreClient).getBalancesById(containerId)

        testRequest(
            "/api/v1/container/$containerId/close-empty",
            HttpMethod.PUT,
            null,
            "controller/container-controller/close-empty/with-balances-response.json",
            status().isBadRequest
        )
        verify(coreClient).getBalancesById(containerId)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/close-empty-child/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/close-empty-child/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeEmptyContainerWhenContainerIsChild() {
        val containerId = "TM00002"

        testRequest(
            "/api/v1/container/$containerId/close-empty",
            HttpMethod.PUT,
            null,
            "controller/container-controller/close-empty-child/response.json",
            status().isBadRequest
        )
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeEmptyContainerWhenContainerIsInactive() {
        val containerId = "TM00002"

        testRequest(
            "/api/v1/container/$containerId/close-empty",
            HttpMethod.PUT,
            null,
            "controller/container-controller/close-empty-inactive/response.json",
            status().isBadRequest
        )
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeEmptyContainerWhenContainerNotFound() {
        val containerId = "TM00004"

        testRequest(
            "/api/v1/container/$containerId/close-empty",
            HttpMethod.PUT,
            null,
            null,
            status().isOk
        )
        verifyNoMoreInteractions(coreClient)
    }

    private fun testGetRequest(path: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(path)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent(response), false))
        }
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun validateContainerSuccess() {
        testRequest(
            "/api/v1/container/validate",
            HttpMethod.POST,
            "controller/container-controller/validate-success/request.json",
            null,
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun validateContainerError() {
        testRequest(
            "/api/v1/container/validate",
            HttpMethod.POST,
            "controller/container-controller/validate-error/request.json",
            "controller/container-controller/validate-error/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/delete-container-exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteWhenContainerExists() {
        val containerId = "TM00003"

        testDeleteRequest(
            "/api/v1/container/$containerId/delete",
            null,
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/container-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteWhenContainerDoesntExist() {

        val containerId = "TM00019"

        testDeleteRequest(
            "/api/v1/container/$containerId/delete",
            null,
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    fun recreateTransportOrderUsingConveyor() {
        val responseBody = getTransportationResponse(
            "controller/container-controller/recreate-transport-order-success/transportation/response.json")

        Mockito.doReturn(responseBody).`when`(transportationClient).createTransportOrder(any())
        Mockito.doReturn(GetMeasureBuffersResponse(
            buffers = listOf(Location("DIMBUF_1", "MEASURE_BUFF_1"))
        )).whenever(coreClient).getMeasurementBuffers("LOC1", PutawayZoneType.DIMENSIONS_INBOUND)

        testRequest(
            "/api/v1/container/recreate-transport-order",
            HttpMethod.POST,
            "controller/container-controller/recreate-transport-order-success/request.json",
            "controller/container-controller/recreate-transport-order-success/response.json",
            status().isOk
        )

        verify(transportationClient).createTransportOrder(any())
    }

    @Test
    @DatabaseSetup("/controller/container-controller/initial-state.xml")
    fun failedRecreateTransportOrder() {
        val responseBody = getTransportationResponse(
            "controller/container-controller/recreate-transport-order-failed/transportation/response.json")

        Mockito.doReturn(responseBody).`when`(transportationClient).createTransportOrder(any())
        Mockito.doReturn(GetMeasureBuffersResponse(buffers = listOf()))
            .whenever(coreClient).getMeasurementBuffers("LOC1", PutawayZoneType.DIMENSIONS_INBOUND)

        testRequest(
            "/api/v1/container/recreate-transport-order",
            HttpMethod.POST,
            "controller/container-controller/recreate-transport-order-failed/request.json",
            "controller/container-controller/recreate-transport-order-failed/response.json",
            status().isBadRequest
        )
    }

    private fun testRequest(
        path: String,
        method: HttpMethod,
        request: String?,
        response: String?,
        status: ResultMatcher
    ) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.request(method, path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .let { if (request == null) it else it.content(FileContentUtils.getFileContent(request)) }
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent(response), false))
        }
    }

    private fun testDeleteRequest(path: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.delete(path)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent(response), false))
        }
    }

    private fun generateTestDataSet(containerId: String): DataSet {
        return DataSet(
            containerId,
            "LOC1",
            "MEASURE_BUFF_1",
            "MEASURE_BUFF_2",
            "MEASURE_ZONE"
        )
    }

    private fun getTransportationResponse(path: String): ResponseEntity<Resource<TransportOrderResourceContent>>? {
        return ResponseEntity.of(Optional.of(Resource.of(readValue<TransportOrderResourceContent>(
            FileContentUtils.getFileContent(path),
            object : TypeReference<TransportOrderResourceContent?>() {}
        ))))
    }

    data class DataSet(
        val containerId: String,
        val loc: String,
        val firstMeasureBuffer: String,
        val secondMeasureBuffer: String,
        val measurePutawayZone: String)
}
