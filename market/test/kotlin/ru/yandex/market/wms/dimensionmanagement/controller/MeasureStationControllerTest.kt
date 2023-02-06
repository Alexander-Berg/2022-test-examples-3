package ru.yandex.market.wms.dimensionmanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import ru.yandex.market.wms.servicebus.client.ServicebusInternalClient
import ru.yandex.market.wms.servicebus.core.measurement.enums.MeasureEquipmentType
import ru.yandex.market.wms.servicebus.core.measurement.models.MeasureEquipmentProperties
import ru.yandex.market.wms.servicebus.core.measurement.request.GetDimensionsRequest
import ru.yandex.market.wms.servicebus.core.measurement.response.GetDimensionsResponse
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class MeasureStationControllerTest : DimensionManagementIntegrationTest() {

    @Autowired
    @MockBean(name = "servicebusClient")
    private lateinit var servicebusClient: ServicebusInternalClient

    @BeforeEach
    fun clean() {
        reset(servicebusClient)
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/" +
            "active-containers-for-station-exist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveContainerForStationExist() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-containers-for-station-exist/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/assign-user-to-station/" +
        "active-containers-for-station-exist/with-children-before.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/" +
            "active-containers-for-station-exist/with-children-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenInWorkContainerWithChildrenForStationExist() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-containers-for-station-exist/with-children-response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/active-containers-for-user-exist/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/" +
            "active-containers-for-user-exist/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveContainerForUserExist() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-containers-for-user-exist/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/active-orders-for-station-exist/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/" +
            "active-orders-for-station-exist/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveOrdersForStationExist() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-orders-for-station-exist/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/" +
            "active-orders-for-station-exist/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/" +
            "active-orders-for-station-exist/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveOrdersForStationExistAndContinueWithOrdersFalse() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user?continueWithOrders=false",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-orders-for-station-exist/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/" +
            "active-orders-for-station-exist/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/" +
            "active-orders-for-station-exist/continue-with-orders-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveOrdersForStationExistAndContinueWithOrdersTrue() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user?continueWithOrders=true",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-orders-for-station-exist/continue-with-orders-response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/active-orders-for-station-exist/" +
            "order-by-user/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/active-orders-for-station-exist/" +
            "continue-with-orders-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveOrdersForStationByUserExist() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/active-orders-for-station-exist/" +
                "order-by-user/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/active-orders-for-station-exist/diff-orders/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/active-orders-for-station-exist/" +
            "diff-orders/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveOrdersForStationByUserAndAnotherUserExist() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/active-orders-for-station-exist/" +
                "diff-orders/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/active-orders-for-user-exist/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/active-orders-for-user-exist/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveOrdersForUserExist() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-orders-for-user-exist/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/" +
            "active-containers-and-orders/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/" +
            "active-containers-and-orders/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWhenActiveOrdersAndContainersForStationExistAndContinueWithOrders() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user?continueWithOrders=true&continueWithContainers=true",
            "controller/measure-station-controller/assign-user-to-station/" +
                "active-containers-and-orders/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/user-already-assigned-to-station/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/user-already-assigned-to-station/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationUserAlreadyAssignToStation() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/" +
                "user-already-assigned-to-station/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/user-already-assigned-to-station/with-children-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/user-already-assigned-to-station/with-children-before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationUserAlreadyAssignToStationAndInWorkContainerWithChildren() {
        val loc = "loc1"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/" +
                "user-already-assigned-to-station/with-children-response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/happy-path-without-containers/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationWithoutContainersHappyPath() {
        val loc = "loc2"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/happy-path-without-containers/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationNotFoundLoc() {
        val loc = "loc3"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/not-found-loc/response.json",
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun unassignUserFromStationWhenUserDoesntHaveActiveAssignment() {
        testPutRequest(
            "/api/v1/station/user/finish",
            null,
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/unassign-user-from-station/user-has-active-assigment/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/unassign-user-from-station/user-has-active-assigment/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun unassignUserFromStationWhenUserHasActiveAssignment() {
        testPutRequest(
            "/api/v1/station/user/finish",
            null,
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/unassign-user-from-station/user-has-active-orders/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/unassign-user-from-station/user-has-active-orders/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun unassignUserFromStationWhenUserHasActiveOrders() {
        testPutRequest(
            "/api/v1/station/user/finish",
            "controller/measure-station-controller/unassign-user-from-station/user-has-active-orders/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/unassign-user-from-station/user-has-active-containers/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/unassign-user-from-station/user-has-active-containers/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun unassignUserFromStationWhenUserHasActiveContainer() {
        testPutRequest(
            "/api/v1/station/user/finish",
            "controller/measure-station-controller/unassign-user-from-station/user-has-active-containers/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsHappyPath() {
        val itemId = "1234567890"
        whenever(servicebusClient.getDimensions(dimensionsRequest))
            .thenReturn(
                GetDimensionsResponse(
                    "TST-001",
                    GetDimensionsResponse.Dimensions(BigDecimal(2), BigDecimal(3), BigDecimal(4), BigDecimal(5)),
                    itemId
                )
            )

        testGetRequest(
            "/api/v1/station/dimensions?itemId=$itemId",
            "controller/measure-station-controller/get-dimensions/response.json",
            status().isOk
        )

        verify(servicebusClient).getDimensions(dimensionsRequest)
        verifyNoMoreInteractions(servicebusClient)
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-2.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsShouldFailWhenItemIdIsNull() {
        testGetRequest(
            "/api/v1/station/dimensions?itemId=",
            "controller/measure-station-controller/get-dimensions/item-id-is-null-request.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-3.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-3.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsShouldFailWhenMeasureStationIsNotFoundByUser() {
        val itemId = "1234567890"

        testGetRequest(
            "/api/v1/station/dimensions?itemId=$itemId",
            "controller/measure-station-controller/get-dimensions/station-not-found-response.json",
            status().isInternalServerError
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-4.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-4.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsShouldFailWhenMeasureStationIsNotLinkedWithMeasureEquipment() {
        val itemId = "1234567890"

        testGetRequest(
            "/api/v1/station/dimensions?itemId=$itemId",
            "controller/measure-station-controller/get-dimensions/station-not-linked-with-equipment-response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-6.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-6.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsShouldFailWhenMeasureEquipmentIsNotEnabled() {
        val itemId = "1234567890"

        testGetRequest(
            "/api/v1/station/dimensions?itemId=$itemId",
            "controller/measure-station-controller/get-dimensions/equipment-not-enabled-response.json",
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsShouldFailWhenErrorOccurredWhileGettingDimensionsFromMeasureEquipment() {
        val itemId = "1234567890"
        whenever(servicebusClient.getDimensions(dimensionsRequest))
            .thenThrow(RuntimeException("Error while getting dimensions"))

        testGetRequest(
            "/api/v1/station/dimensions?itemId=$itemId",
            "controller/measure-station-controller/get-dimensions/equipment-error-occurred-response.json",
            status().isInternalServerError
        )

        verify(servicebusClient).getDimensions(dimensionsRequest)
        verifyNoMoreInteractions(servicebusClient)
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsShouldFailWhenOneOfMeasuredDimensionsIsNull() {
        val itemId = "1234567890"
        whenever(servicebusClient.getDimensions(dimensionsRequest))
            .thenReturn(
                GetDimensionsResponse(
                    "TST-001",
                    GetDimensionsResponse.Dimensions(BigDecimal(2), null, BigDecimal(4), BigDecimal(5)),
                    itemId
                )
            )

        testGetRequest(
            "/api/v1/station/dimensions?itemId=$itemId",
            "controller/measure-station-controller/get-dimensions/measurement-is-null-response.json",
            status().isInternalServerError
        )

        verify(servicebusClient).getDimensions(dimensionsRequest)
        verifyNoMoreInteractions(servicebusClient)
    }

    @Test
    @DatabaseSetup("/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml")
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/get-dimensions/immutable-state-station-1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDimensionsShouldFailWhenOneOfMeasuredDimensionsIsZero() {
        val itemId = "1234567890"
        whenever(servicebusClient.getDimensions(dimensionsRequest))
            .thenReturn(
                GetDimensionsResponse(
                    "TST-001",
                    GetDimensionsResponse.Dimensions(BigDecimal.ZERO, BigDecimal(3), BigDecimal(4), BigDecimal.ZERO),
                    itemId
                )
            )

        testGetRequest(
            "/api/v1/station/dimensions?itemId=$itemId",
            "controller/measure-station-controller/get-dimensions/measurement-is-zero-response.json",
            status().isInternalServerError
        )

        verify(servicebusClient).getDimensions(dimensionsRequest)
        verifyNoMoreInteractions(servicebusClient)
    }

    @Test
    @DatabaseSetup(
        "/controller/measure-station-controller/assign-user-to-station/another-user-already-assigned-to-station/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measure-station-controller/assign-user-to-station/another-user-already-assigned-to-station/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToStationAnotherUserAlreadyAssignedToStation() {
        val loc = "loc2"

        testPutRequest(
            "/api/v1/station/$loc/user",
            "controller/measure-station-controller/assign-user-to-station/happy-path-without-containers/response.json",
            status().isOk
        )
    }


    private val dimensionsRequest
        get() =
            GetDimensionsRequest(
                "TST-001",
                MeasureEquipmentProperties(
                    "mast.yandex-team-test.net",
                    8068,
                    "/api/equipment",
                    "admin",
                    "equipPwd#2",
                    MeasureEquipmentType.INFOSCAN_3D90
                ),
                "1234567890"
            )

    private fun testGetRequest(path: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(path)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(content().json(getFileContent(response), false))
        }
    }

    private fun testPutRequest(path: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(path)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(jsonNonExtensible(response))
        }
    }

    private fun jsonNonExtensible(expectedJsonFileName: String): ResultMatcher {
        return ResultMatcher { result: MvcResult ->
            val content = result.response.getContentAsString(StandardCharsets.UTF_8)
            JsonAssertUtils.assertFileEquals(expectedJsonFileName, content, JSONCompareMode.NON_EXTENSIBLE)
        }
    }
}
