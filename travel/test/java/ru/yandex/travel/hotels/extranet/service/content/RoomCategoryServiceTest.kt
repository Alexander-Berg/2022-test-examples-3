package ru.yandex.travel.hotels.extranet.service.content

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.test.context.ActiveProfiles
import ru.yandex.travel.hotels.extranet.dto.RoomCategoryType
import ru.yandex.travel.hotels.extranet.entities.Hotel
import ru.yandex.travel.hotels.extranet.entities.HotelManagementSource
import ru.yandex.travel.hotels.extranet.entities.Organization
import ru.yandex.travel.hotels.extranet.entities.Permission
import ru.yandex.travel.hotels.extranet.entities.User
import ru.yandex.travel.hotels.extranet.errors.AuthorizationException
import ru.yandex.travel.hotels.extranet.repository.HotelRepository
import ru.yandex.travel.hotels.extranet.repository.OrganizationRepository
import ru.yandex.travel.hotels.extranet.repository.RoomCategoryRepository
import ru.yandex.travel.hotels.extranet.service.roles.UserRoleService
import ru.yandex.travel.hotels.extranet.service.content.roomcat.RoomCategoryService
import javax.transaction.Transactional

@SpringBootTest
@ActiveProfiles("test")
open class RoomCategoryServiceTest {
    @Autowired
    lateinit var roomCategoryRepository: RoomCategoryRepository

    @Autowired
    lateinit var service: RoomCategoryService

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @MockBean
    lateinit var userRoleService: UserRoleService

    var hotelId1: Long = 0
    var hotelId2: Long = 0

    @BeforeEach
    fun setup() {
        var org = Organization("test");
        organizationRepository.save(org)
        organizationRepository.flush()
        hotelId1 = hotelRepository.save(Hotel(org, "test 1", HotelManagementSource.YANDEX)).id!!
        hotelId2 = hotelRepository.save(Hotel(org, "test 2", HotelManagementSource.YANDEX)).id!!
        hotelRepository.flush()
        given(userRoleService.checkPermission(any(), any(), any(), any())).willReturn(User(1, "test"))
    }

    @AfterEach
    fun tearDown() {
        roomCategoryRepository.deleteAll()
        hotelRepository.deleteAll()
        organizationRepository.deleteAll()

        roomCategoryRepository.flush()
        hotelRepository.flush()
        organizationRepository.flush()
    }

    @Test
    fun testNoRooms() {
        assertThat(service.listRoomCategories(hotelId1, includeDisabled = true, includeHidden = true)).isEmpty()
    }

    @Test
    fun testCreateRoomNoPlacements() {
        var rcd = service.createRoomCategory(
            hotelId1, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            2, 0, false
        )
        assertThat(rcd).isNotNull;
        assertThat(rcd.name).isEqualTo("test room")
        assertThat(rcd.description).isEqualTo("room created for testing")
        assertThat(rcd.id).isGreaterThan(0)
        assertThat(rcd.maxPrimaryPlaces).isEqualTo(2)
        assertThat(rcd.maxExtraPlaces).isEqualTo(0)
        assertThat(rcd.placementsList).isEmpty()
    }

    @Test
    fun testCreateRoomWithPlacements() {
        var rcd = service.createRoomCategory(
            hotelId1, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        )
        assertThat(rcd).isNotNull;
        assertThat(rcd.name).isEqualTo("test room")
        assertThat(rcd.description).isEqualTo("room created for testing")
        assertThat(rcd.id).isGreaterThan(0)
        assertThat(rcd.maxPrimaryPlaces).isEqualTo(5)
        assertThat(rcd.maxExtraPlaces).isEqualTo(2)
        assertThat(rcd.placementsList).hasSize(15)
    }

    @Test
    fun testGet() {
        var rcId = service.createRoomCategory(
            hotelId1, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        ).id
        var rcd = service.getRoomCategory(hotelId1, rcId)
        assertThat(rcd).isNotNull;
        assertThat(rcd.name).isEqualTo("test room")
        assertThat(rcd.description).isEqualTo("room created for testing")
        assertThat(rcd.id).isGreaterThan(0)
        assertThat(rcd.maxPrimaryPlaces).isEqualTo(5)
        assertThat(rcd.maxExtraPlaces).isEqualTo(2)
        assertThat(rcd.placementsList).hasSize(15)
    }

    @Test
    fun testGetMissing() {
        assertThrows<EmptyResultDataAccessException> { service.getRoomCategory(hotelId1, 42) }
    }

    @Test
    fun testGetWrongHotel() {
        var rcId = service.createRoomCategory(
            hotelId2, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        ).id
        assertThrows<EmptyResultDataAccessException> { service.getRoomCategory(hotelId1, rcId) }
    }

    @Test
    fun testNoPermissionForHotelOnGet() {
        given(
            userRoleService.checkPermission(
                Permission.MANAGE_ROOM_TYPES,
                hotelId = hotelId2
            )
        ).willAnswer { throw AuthorizationException("not authorized") }
        var rcId = service.createRoomCategory(
            hotelId1, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        ).id
        assertThrows<AuthorizationException> { service.getRoomCategory(hotelId2, rcId) }
    }

    @Test
    fun testList() {
        service.createRoomCategory(
            hotelId1, "test room 1", "room 1 created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        )
        service.createRoomCategory(
            hotelId1, "test room 2", "room 2 created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            2, 1, true
        )
        service.createRoomCategory(
            hotelId2, "test room 3", "room 3 created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            2, 1, true
        )
        var result = service.listRoomCategories(hotelId1)
        assertThat(result).hasSize(2)
    }

    @Test
    fun testEnableDisable() {
        var rcId = service.createRoomCategory(
            hotelId1, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        ).id
        assertThat(service.listRoomCategories(hotelId1, includeDisabled = false)).hasSize(1)
        service.disableRoomCategory(hotelId1, rcId)
        assertThat(service.listRoomCategories(hotelId1, includeDisabled = false)).hasSize(0)
        assertThat(service.listRoomCategories(hotelId1, includeDisabled = true)).hasSize(1)
        assertThat(service.listRoomCategories(hotelId1)).hasSize(1)
        assertThat(service.getRoomCategory(hotelId1, rcId)).isNotNull
        assertDoesNotThrow { service.disableRoomCategory(hotelId1, rcId) }
        assertDoesNotThrow { service.disableRoomCategory(hotelId1, rcId) }
        service.enableRoomCategory(hotelId1, rcId)
        assertThat(service.listRoomCategories(hotelId1, includeDisabled = false)).hasSize(1)
        assertThat(service.listRoomCategories(hotelId1, includeDisabled = true)).hasSize(1)
        assertDoesNotThrow { service.enableRoomCategory(hotelId1, rcId) }
        assertDoesNotThrow { service.enableRoomCategory(hotelId1, rcId) }
    }

    @Test
    fun testHideShow() {
        var rcId = service.createRoomCategory(
            hotelId1, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        ).id
        assertThat(service.listRoomCategories(hotelId1, includeHidden = false)).hasSize(1)
        service.hideRoomCategory(hotelId1, rcId)
        assertThat(service.listRoomCategories(hotelId1, includeHidden = false)).hasSize(0)
        assertThat(service.listRoomCategories(hotelId1, includeHidden = true)).hasSize(1)
        assertThat(service.listRoomCategories(hotelId1)).hasSize(1)
        assertThat(service.getRoomCategory(hotelId1, rcId)).isNotNull
        assertDoesNotThrow { service.hideRoomCategory(hotelId1, rcId) }
        assertDoesNotThrow { service.hideRoomCategory(hotelId1, rcId) }
        service.showRoomCategory(hotelId1, rcId)
        assertThat(service.listRoomCategories(hotelId1, includeHidden = false)).hasSize(1)
        assertThat(service.listRoomCategories(hotelId1, includeHidden = true)).hasSize(1)
        assertDoesNotThrow { service.showRoomCategory(hotelId1, rcId) }
        assertDoesNotThrow { service.showRoomCategory(hotelId1, rcId) }
    }

    @Test
    @Transactional
    fun testSoftDelete() {
        var rcId = service.createRoomCategory(
            hotelId1, "test room", "room created for testing",
            RoomCategoryType.ROOM_CATEGORY_TYPE_HOTEL_ROOM,
            5, 2, true
        ).id
        service.deleteRoomCategory(hotelId1, rcId)
        assertThrows<EmptyResultDataAccessException> { service.getRoomCategory(hotelId1, rcId) }
        assertThrows<EmptyResultDataAccessException> { service.deleteRoomCategory(hotelId1, rcId) }
        assertThrows<EmptyResultDataAccessException> { service.enableRoomCategory(hotelId1, rcId) }
        assertThrows<EmptyResultDataAccessException> { service.disableRoomCategory(hotelId1, rcId) }
        assertThrows<EmptyResultDataAccessException> { service.showRoomCategory(hotelId1, rcId) }
        assertThrows<EmptyResultDataAccessException> { service.hideRoomCategory(hotelId1, rcId) }
        assertThat(service.listRoomCategories(hotelId1)).isEmpty()
        assertThat(roomCategoryRepository.getOne(rcId)).isNotNull.matches { rc -> rc.deleted }
    }
}
