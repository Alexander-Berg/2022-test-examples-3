package ru.yandex.travel.hotels.extranet.service.content

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.test.context.ActiveProfiles
import ru.yandex.travel.hotels.extranet.entities.AnnulationPenaltyType
import ru.yandex.travel.hotels.extranet.entities.AnnulationPolicy
import ru.yandex.travel.hotels.extranet.entities.AnnulationRule
import ru.yandex.travel.hotels.extranet.entities.Hotel
import ru.yandex.travel.hotels.extranet.entities.HotelManagementSource
import ru.yandex.travel.hotels.extranet.entities.Organization
import ru.yandex.travel.hotels.extranet.entities.RoomCategory
import ru.yandex.travel.hotels.extranet.entities.User
import ru.yandex.travel.hotels.extranet.repository.AnnulationPolicyRepository
import ru.yandex.travel.hotels.extranet.repository.HotelRepository
import ru.yandex.travel.hotels.extranet.repository.OrganizationRepository
import ru.yandex.travel.hotels.extranet.repository.RatePlanRepository
import ru.yandex.travel.hotels.extranet.repository.RoomCategoryRepository
import ru.yandex.travel.hotels.extranet.service.content.rateplans.RatePlanService
import ru.yandex.travel.hotels.extranet.service.roles.UserRoleService
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RatePlanServiceTest {
    @Autowired
    lateinit var ratePlanService: RatePlanService

    @Autowired
    lateinit var ratePlanRepository: RatePlanRepository

    @Autowired
    lateinit var roomCategoryRepository: RoomCategoryRepository

    @Autowired
    lateinit var annulationPolicyRepository: AnnulationPolicyRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @MockBean
    lateinit var userRoleService: UserRoleService

    lateinit var hotel: Hotel
    lateinit var policy: AnnulationPolicy
    lateinit var cat1: RoomCategory
    lateinit var cat2: RoomCategory
    lateinit var cat3: RoomCategory

    @BeforeEach
    fun setup() {
        var org = Organization("test");
        organizationRepository.saveAndFlush(org)
        hotel = hotelRepository.saveAndFlush(Hotel(org, "test hotel", HotelManagementSource.YANDEX))
        policy = annulationPolicyRepository.saveAndFlush(
            AnnulationPolicy(
                org,
                "foo",
                listOf(AnnulationRule(AnnulationPenaltyType.NONE))
            )
        )
        cat1 = roomCategoryRepository.saveAndFlush(RoomCategory(hotel, "category 1", maxPrimaryPlaces = 2))
        cat2 = roomCategoryRepository.saveAndFlush(RoomCategory(hotel, "category 2", maxPrimaryPlaces = 2))
        cat3 = roomCategoryRepository.saveAndFlush(RoomCategory(hotel, "category 3", maxPrimaryPlaces = 2))
        given(userRoleService.checkPermission(any(), any(), any(), any())).willReturn(User(1, "test"))
    }

    @AfterEach
    fun tearDown() {
        ratePlanRepository.deleteAll()
        annulationPolicyRepository.deleteAll()
        roomCategoryRepository.deleteAll()
        hotelRepository.deleteAll()
        organizationRepository.deleteAll()

        ratePlanRepository.flush()
        annulationPolicyRepository.flush()
        roomCategoryRepository.flush()
        hotelRepository.flush()
        organizationRepository.flush()
    }

    @Test
    fun testNoRatePlans() {
        assertThat(ratePlanService.listRatePlans(hotel.id!!, includeDisabled = true, includeHidden = true))
            .isEmpty()
    }

    @Test
    fun testCreateRatePlanWithNoRoomCats() {
        val dto = ratePlanService.addRatePlan(hotel.id!!, "test plan", policy.id!!, "some description")
        assertThat(dto.id).isNotZero
        assertThat(dto.name).isEqualTo("test plan")
        assertThat(dto.description).isEqualTo("some description")
        assertThat(dto.annulationPolicy.id).isEqualTo(policy.id)
        assertThat(dto.annulationPolicy.name).isEqualTo("foo")
        assertThat(dto.roomCategoriesCount).isEqualTo(0)
    }

    @Test
    fun testCreateRatePlanWithSomeRoomCats() {
        val dto = ratePlanService.addRatePlan(
            hotel.id!!, "test plan", policy.id!!, "some description",
            setOf(cat1.id!!, cat2.id!!)
        )
        assertThat(dto.id).isNotZero
        assertThat(dto.name).isEqualTo("test plan")
        assertThat(dto.description).isEqualTo("some description")
        assertThat(dto.annulationPolicy.id).isEqualTo(policy.id)
        assertThat(dto.annulationPolicy.name).isEqualTo("foo")
        assertThat(dto.roomCategoriesCount).isEqualTo(2)
        assertThat(dto.roomCategoriesList).extracting("id").containsExactlyInAnyOrder(cat1.id, cat2.id)
        assertThat(dto.roomCategoriesList).extracting("name").containsExactlyInAnyOrder(cat1.name, cat2.name)
    }

    @Test
    fun testUnableToCreateWrongHotel() {
        assertThrows<EntityNotFoundException> {
            ratePlanService.addRatePlan(
                -1,
                "test plan",
                policy.id!!,
                "some description"
            )
        }
    }

    @Test
    fun testUnableToCreateWrongPolicy() {
        assertThrows<EmptyResultDataAccessException> {
            ratePlanService.addRatePlan(
                hotel.id!!,
                "test plan",
                -1,
                "some description"
            )
        }
    }

    @Test
    fun testUnableToCreateSomeMissingRooms() {
        val th = assertThrows<EntityNotFoundException> {
            ratePlanService.addRatePlan(
                hotel.id!!,
                "test plan",
                policy.id!!,
                "some description",
                setOf(cat1.id!!, cat2.id!!, -1)
            )
        }
        assertThat(th.message).isEqualTo("Category with id(s) [-1] is (are) not found")
    }

    @Test
    fun testUpdateRatePlanRoomsAndGet() {
        val id = ratePlanService.addRatePlan(hotel.id!!, "test plan", policy.id!!, "some description").id
        val updateDto = ratePlanService.updateRatePlan(hotel.id!!, id, roomCategories = setOf(cat1.id!!))
        assertThat(updateDto.name).isEqualTo("test plan")
        assertThat(updateDto.description).isEqualTo("some description")
        assertThat(updateDto.annulationPolicy.id).isEqualTo(policy.id)
        assertThat(updateDto.roomCategoriesCount).isEqualTo(1)
        assertThat(updateDto.roomCategoriesList).extracting("id").containsExactlyInAnyOrder(cat1.id)
        assertThat(updateDto.roomCategoriesList).extracting("name").containsExactlyInAnyOrder(cat1.name)
        val getDto = ratePlanService.getRatePlan(hotel.id!!, id)
        assertThat(getDto).isEqualTo(updateDto)
    }
}
