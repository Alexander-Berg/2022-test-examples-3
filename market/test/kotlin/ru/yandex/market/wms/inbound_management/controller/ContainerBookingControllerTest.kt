package ru.yandex.market.wms.inbound_management.controller

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.DatabaseTearDown
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.arrayContainingInAnyOrder
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.CollectionUtils.toMultiValueMap
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.inbound_management.config.CoreDBClient
import ru.yandex.market.wms.inbound_management.config.TransportationDBClient
import ru.yandex.market.wms.inbound_management.controller.InboundManagementRequestPath.Companion.DELETE_CONTAINER_BOOKINGS_PATH
import ru.yandex.market.wms.inbound_management.controller.InboundManagementRequestPath.Companion.GET_BOOKINGS_ON_CONDITION_PATH
import ru.yandex.market.wms.inbound_management.controller.InboundManagementRequestPath.Companion.REQUEST_CONTAINER_BOOKING_PATH
import ru.yandex.market.wms.inbound_management.controller.InboundManagementRequestPath.Companion.UPDATE_CONTAINER_BOOKINGS_PATH
import ru.yandex.market.wms.inbound_management.entity.Error
import ru.yandex.market.wms.inbound_management.model.dto.BookingStatus.COMPLETED
import ru.yandex.market.wms.inbound_management.model.dto.BookingStatus.PENDING
import ru.yandex.market.wms.inbound_management.model.dto.ContainerBooking
import ru.yandex.market.wms.inbound_management.model.dto.ContainerId
import ru.yandex.market.wms.inbound_management.model.dto.LocationId
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ContextConfiguration(classes = [CoreDBClient::class, TransportationDBClient::class])
@DatabaseSetups(
    DatabaseSetup("/db/loc/locations.xml"),
    DatabaseSetup("/db/container-bookings/chunk-size.xml", type = DatabaseOperation.INSERT),
)
@Transactional(propagation = Propagation.SUPPORTS)
@DatabaseTearDown("/db/void.xml", type = DatabaseOperation.DELETE_ALL)
class ContainerBookingControllerTest(
    private val clock: Clock,
    context: WebApplicationContext
) : IntegrationTest() {
    private val client = MockMvcWebTestClient.bindToApplicationContext(context).build()

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
    )
    @ExpectedDatabase(
        "/db/container-bookings/one-booking.xml", assertionMode = NON_STRICT_UNORDERED
    )
    fun firstBooking() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A1"))
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerBookingResponse>().isEqualTo(
                ContainerBookingResponse(
                    containerBookings = listOf(
                        ContainerBooking(
                            container = "PLT007",
                            status = PENDING,
                            location = "STAGE_A1",
                            receipt = "0000000004",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        )
                    )
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
    )
    fun nothingToBook() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A1"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ContainerBookingResponse>().isEqualTo(
                ContainerBookingResponse(
                    containerBookings = emptyList(),
                    wmsErrorCode = Error.NoAvailableContainers.code
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/all-booked.xml"),
    )
    fun allBooked() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A1"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ContainerBookingResponse>().isEqualTo(
                ContainerBookingResponse(
                    containerBookings = emptyList(),
                    wmsErrorCode = Error.NoAvailableContainers.code
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/partly-booked-1.xml"),
    )
    @ExpectedDatabase(
        "/db/container-bookings/partly-booked-2.xml", assertionMode = NON_STRICT_UNORDERED
    )
    fun filterBookedContainers() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A1"))
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerBookingResponse>().isEqualTo(
                ContainerBookingResponse(
                    containerBookings = listOf(
                        ContainerBooking(
                            container = "PLT004",
                            status = PENDING,
                            location = "STAGE_A1",
                            receipt = "0000000003",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        )
                    )
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/partly-booked-1.xml"),
    )
    @ExpectedDatabase(
        "/db/container-bookings/partly-booked-3.xml", assertionMode = NON_STRICT_UNORDERED
    )
    fun processPendingBookings() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A2"))
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerBookingResponse>()
            .consumeWith {
                assertNotNull(it.responseBody)
                assertThat(
                    it.responseBody!!.containerBookings.toTypedArray(),
                    arrayContainingInAnyOrder(
                        ContainerBooking(
                            container = "PLT003",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000002",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT002",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000002",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                    )
                )
            }
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/all-booked.xml"),
    )
    fun limitActiveBookings() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A2"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ContainerBookingResponse>().isEqualTo(
                ContainerBookingResponse(
                    containerBookings = emptyList(),
                    wmsErrorCode = "ACTIVE_BOOKINGS_LIMIT",
                    wmsErrorData = mapOf("limit" to 1, "activeBookings" to listOf("PLT003"))
                )
            )
    }

    @Test
    fun incorrectTargetLocation() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("INCORRECT_LOC"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ContainerBookingResponse>().isEqualTo(
                ContainerBookingResponse(
                    containerBookings = emptyList(),
                    wmsErrorCode = "WRONG_TARGET_LOCATION",
                    wmsErrorData = mapOf("location" to "INCORRECT_LOC")
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
    )
    @ExpectedDatabase(
        "/db/container-bookings/booking-from-another-zone.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun bookFromLinkedZone() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_C1"))
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerBookingResponse>().isEqualTo(
                ContainerBookingResponse(
                    containerBookings = listOf(
                        ContainerBooking(
                            container = "PLT002",
                            status = PENDING,
                            location = "STAGE_C1",
                            receipt = "0000000002",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        )
                    )
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
    )
    @ExpectedDatabase(
        "/db/container-bookings/one-pending-booking.xml", assertionMode = NON_STRICT_UNORDERED
    )
    fun transportOrderError() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_D1"))
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/all-pl-in-one-loc.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/one-booking.xml"),
    )
    @ExpectedDatabase(
        "/db/container-bookings/multiple-booked-by-table.xml", assertionMode = NON_STRICT_UNORDERED
    )
    fun requestMultipleFromLoc() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A2"))
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerBookingResponse>()
            .consumeWith {
                assertNotNull(it.responseBody)
                assertThat(
                    it.responseBody!!.containerBookings.toTypedArray(),
                    arrayContainingInAnyOrder(
                        ContainerBooking(
                            container = "PLT003",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000002",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT002",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000002",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                    )
                )
            }
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/all-pl-in-one-loc.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/include-all-from-source.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/one-booking.xml"),
    )
    @ExpectedDatabase(
        "/db/container-bookings/booked-all-from-loc.xml", assertionMode = NON_STRICT_UNORDERED
    )
    fun requestAllFromSameLocation() {
        client.post()
            .uri(REQUEST_CONTAINER_BOOKING_PATH)
            .bodyValue(LocationId("STAGE_A2"))
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerBookingResponse>()
            .consumeWith {
                assertNotNull(it.responseBody)
                assertThat(
                    it.responseBody!!.containerBookings.toTypedArray(),
                    arrayContainingInAnyOrder(
                        ContainerBooking(
                            container = "PLT003",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000002",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT002",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000002",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT004",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000003",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT005",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000003",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT006",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000003",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT009",
                            status = PENDING,
                            location = "STAGE_A2",
                            receipt = "0000000005",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                    )
                )
            }
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/container-bookings/partly-booked-2.xml")
    )
    @ExpectedDatabase("/db/container-bookings/updated-booking.xml", assertionMode = NON_STRICT_UNORDERED)
    fun updateContainerBooking() {
        client.put()
            .uri { uriBuilder ->
                uriBuilder.path(UPDATE_CONTAINER_BOOKINGS_PATH).build()
            }
            .bodyValue(
                UpdateContainerBookingsRequest(
                    listOf(
                        ContainerBooking(
                            container = "PLT004",
                            status = COMPLETED,
                            location = "STAGE_A2",
                            receipt = "0000000004",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        )
                    )
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<UpdateContainerBookingsResponse>().isEqualTo(
                UpdateContainerBookingsResponse(updated = 1)
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/partly-booked-1.xml"),
    )
    fun updateNonexistentContainerBooking() {
        client.put()
            .uri { uriBuilder ->
                uriBuilder.path(UPDATE_CONTAINER_BOOKINGS_PATH).build()
            }
            .bodyValue(
                UpdateContainerBookingsRequest(
                    listOf(
                        ContainerBooking(
                            container = "PLT012",
                            status = COMPLETED,
                            location = "STAGE_A2",
                            receipt = "0000000012",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        )
                    )
                )
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<UpdateContainerBookingsResponse>().isEqualTo(
                UpdateContainerBookingsResponse(
                    updated = 0,
                    wmsErrorCode = "NO_BOOKINGS_TO_UPDATE",
                    wmsErrorData = mapOf()
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/partly-booked-4.xml"),
    )
    fun requestBookingsOnCondition() {
        val conditions = mutableMapOf<String, List<String>>()
        conditions["STATUS"] = listOf(COMPLETED.code.toString(), PENDING.code.toString())
        conditions["RECEIPTKEY"] = listOf("0000000001")
        client.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(GET_BOOKINGS_ON_CONDITION_PATH)
                    .queryParams(toMultiValueMap(conditions))
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerBookingResponse>()
            .consumeWith {
                assertNotNull(it.responseBody)
                assertThat(
                    it.responseBody!!
                        .containerBookings
                        .map { booking -> booking.copy(editDate = OffsetDateTime.now(clock)) }
                        .toTypedArray(),
                    arrayContainingInAnyOrder(
                        ContainerBooking(
                            container = "PLT001",
                            status = COMPLETED,
                            location = "STAGE_B1",
                            receipt = "0000000001",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                        ContainerBooking(
                            container = "PLT005",
                            status = PENDING,
                            location = "STAGE_B2",
                            receipt = "0000000001",
                            editDate = OffsetDateTime.now(clock),
                            editWho = "TEST",
                        ),
                    )
                )
            }
    }

    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/receiptDetail/all-pl-in-one-loc.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/db/container-bookings/multiple-booked-by-table.xml"),
    )
    @ExpectedDatabase("/db/container-bookings/one-booking.xml", assertionMode = NON_STRICT_UNORDERED)
    @Test
    fun delete() {
        client.put()
            .uri(DELETE_CONTAINER_BOOKINGS_PATH)
            .bodyValue(listOf(ContainerId("PLT003"), ContainerId("PLT002")))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun deleteEmpty() {
        client.put()
            .uri(DELETE_CONTAINER_BOOKINGS_PATH)
            .bodyValue(listOf<Nothing>())
            .exchange()
            .expectStatus().isBadRequest
    }
}
