package ru.yandex.travel.hotels.extranet.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import ru.yandex.travel.hotels.extranet.entities.AnnulationPenaltyType
import ru.yandex.travel.hotels.extranet.entities.AnnulationPolicy
import ru.yandex.travel.hotels.extranet.entities.AnnulationRule
import ru.yandex.travel.hotels.extranet.entities.Hotel
import ru.yandex.travel.hotels.extranet.entities.Organization
import ru.yandex.travel.hotels.extranet.entities.RatePlan
import ru.yandex.travel.hotels.extranet.entities.RoomCategory
import ru.yandex.travel.hotels.extranet.repository.AnnulationPolicyRepository
import ru.yandex.travel.hotels.extranet.repository.HotelRepository
import ru.yandex.travel.hotels.extranet.repository.OrganizationRepository
import ru.yandex.travel.hotels.extranet.repository.RatePlanRepository
import ru.yandex.travel.hotels.extranet.repository.RoomCategoryRepository

@DataJpaTest
class RatePlanRepositoryTest {
    @Autowired
    lateinit var ratePlanRepository: RatePlanRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var roomCategoryRepository: RoomCategoryRepository

    @Autowired
    lateinit var annulationPolicyRepository: AnnulationPolicyRepository

    lateinit var hotel: Hotel

    lateinit var roomCats: Set<RoomCategory>

    lateinit var policies: List<AnnulationPolicy>

    private val numRoomCats = 5;
    private val numPolicies = 3;

    @BeforeEach
    fun setUp() {
        val organization = organizationRepository.saveAndFlush(Organization("test"))
        hotel = hotelRepository.saveAndFlush(
            Hotel(
                organization,
                "test hotel"
            )
        )
        roomCats =
            (1..numRoomCats).map { roomCategoryRepository.save(RoomCategory(hotel, "room $it", maxPrimaryPlaces = 2)) }
                .toSet()
                .also { roomCategoryRepository.flush() }
        policies =
            (1..numPolicies).map {
                annulationPolicyRepository.save(
                    AnnulationPolicy(
                        organization,
                        "policy $it", listOf(AnnulationRule(AnnulationPenaltyType.NONE))
                    )
                )
            }
    }

    @Test
    fun testCreateAndUnbind() {
        val ratePlan = ratePlanRepository.saveAndFlush(RatePlan(hotel, policies[0], "test plan", null, roomCats))
        assertThat(ratePlan.id).isNotNull
        assertThat(ratePlan.roomCategories).hasSize(numRoomCats)
        ratePlan.roomCategories = emptySet()
        ratePlanRepository.flush()
        val fetched = ratePlanRepository.getOne(ratePlan.id!!)
        assertThat(fetched.roomCategories).isEmpty()
    }
}
