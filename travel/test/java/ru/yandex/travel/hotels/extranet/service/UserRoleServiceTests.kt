package ru.yandex.travel.hotels.extranet.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.travel.credentials.UserCredentials
import ru.yandex.travel.hotels.extranet.entities.Hotel
import ru.yandex.travel.hotels.extranet.entities.HotelIdentifier
import ru.yandex.travel.hotels.extranet.entities.Organization
import ru.yandex.travel.hotels.extranet.entities.Permission
import ru.yandex.travel.hotels.extranet.entities.Role
import ru.yandex.travel.hotels.extranet.entities.User
import ru.yandex.travel.hotels.extranet.entities.UserRoleBinding
import ru.yandex.travel.hotels.extranet.errors.AuthorizationException
import ru.yandex.travel.hotels.extranet.errors.IllegalOperationException
import ru.yandex.travel.hotels.extranet.repository.OrganizationRepository
import ru.yandex.travel.hotels.extranet.repository.UserRepository
import ru.yandex.travel.hotels.extranet.service.roles.UserRoleServiceImpl
import ru.yandex.travel.hotels.extranet.withCredentials
import ru.yandex.travel.hotels.proto.EPartnerId
import java.util.UUID
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@SpringBootTest
@RunWith(SpringRunner::class)
@ActiveProfiles("test")
open class UserRoleServiceTests {
    @Autowired
    lateinit var service: UserRoleServiceImpl

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var userRepository: UserRepository

    private var hotelId1: Long? = null
    private var hotelId2: Long? = null
    private var orgId1: UUID? = null
    private var orgId2: UUID? = null

    private val root = UserCredentials.builder().login("root").passportId("1").build()
    private val user = UserCredentials.builder().login("test-user").passportId("123").build()
    private val developer = UserCredentials.builder().login("idm-developer").passportId("777").build()

    @Before
    @Transactional
    fun setUp() {
        userRepository.saveAll(
            listOf(
                User(123, "test-user"),
            )
        )

        val idmUser = User(777, "idm-developer")
        val idmBinding = UserRoleBinding(idmUser, Role.IDM_DEVELOPER, idmLogin = "yndx-idm-developer")
        idmUser.roles.add(idmBinding)
        userRepository.save(idmUser)

        val orgOwner = userRepository.save(User(1, "root"))
        val org = Organization("test org 1")
        org.hotels.addAll(
            listOf(
                Hotel(org, "Test Hotel 1", partnerHotelId = HotelIdentifier(EPartnerId.PI_TRAVELLINE, "100")),
                Hotel(org, "Test Hotel 2")
            )
        )
        val binding = UserRoleBinding(orgOwner, Role.OWNER, organization = org)
        org.users.add(binding)
        orgOwner.roles.add(binding)

        organizationRepository.save(org).also {
            hotelId1 = it.hotels[0].id
            hotelId2 = it.hotels[1].id
            orgId1 = it.id
        }

        organizationRepository.save(Organization("test org 2")).also { orgId2 = it.id }
    }

    @After
    @Transactional
    fun tearDown() {
        organizationRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `test grant operator role globally`() {
        withCredentials(root) {
            service.grantRole(123, orgId1!!, Role.OPERATOR)
        }
        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId2) }
            assertThrows<AuthorizationException> { service.checkPermission(Permission.VIEW_HOTELS, orgId2) }
            assertThrows<AuthorizationException> { service.checkPermission(Permission.MANAGE_USERS, orgId1) }
        }
    }

    @Test
    fun `test grant operator role for hotel`() {
        withCredentials(root) {
            service.grantRole(123, orgId1!!, Role.OPERATOR, hotelId1)
        }
        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertThrows<AuthorizationException> { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId2) }
            assertThrows<AuthorizationException> { service.checkPermission(Permission.VIEW_HOTELS, orgId2) }
            assertThrows<AuthorizationException> { service.checkPermission(Permission.MANAGE_USERS, orgId1) }
        }
    }

    @Test
    fun `test grant operator role for hotel and then globally`() {
        withCredentials(root) {
            service.grantRole(123, orgId1!!, Role.OPERATOR, hotelId1)
        }
        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertThrows<AuthorizationException> { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId2) }
        }
        withCredentials(root) {
            service.grantRole(123, orgId1!!, Role.OPERATOR)
        }
        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId2) }
        }
    }

    @Test
    fun `test grant operator role globally and then for hotel`() {
        withCredentials(root) {
            service.grantRole(123, orgId1!!, Role.OPERATOR)
        }
        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId2) }
        }
        withCredentials(root) {
            assertThrows<IllegalOperationException> {
                service.grantRole(123, orgId1!!, Role.OPERATOR, hotelId1)
            }
        }
        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId2) }
        }
    }

    @Test
    fun `test unable to grant role several times`() {
        withCredentials(root) {
            assertThat(service.grantRole(123, orgId1!!, Role.OPERATOR))
            assertThrows<IllegalOperationException> {
                service.grantRole(123, orgId1!!, Role.OPERATOR)
            }
        }
        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId2) }
        }
    }

    @Test
    fun `test assign role then revoke`() {
        var permId: UUID? = null
        withCredentials(root) {
            permId = service.grantRole(123, orgId1!!, Role.OPERATOR, hotelId1)
        }

        withCredentials(user) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
        }
        withCredentials(root) {
            service.revokeRole(permId!!)
        }
        withCredentials(user) {
            assertThrows<AuthorizationException> { service.checkPermission(Permission.VIEW_HOTELS, orgId1, hotelId1) }
        }
    }

    @Test
    fun `test unable to revoke own role`() {
        withCredentials(root) {
            val permId = service.listOrgUsers(orgId1!!).find { it.login == "root" }!!.id.let { UUID.fromString(it) }
            assertThrows<IllegalOperationException> { service.revokeRole(permId) }
            assertDoesNotThrow { service.checkPermission(Permission.MANAGE_USERS, orgId1) }
        }
    }

    @Test
    fun `test unable to revoke twice`() {
        withCredentials(root) {
            val permId = service.grantRole(123, orgId1!!, Role.OPERATOR)
            service.revokeRole(permId)
            assertThrows<EntityNotFoundException> { service.revokeRole(permId) }
        }
    }

    @Test
    fun `test permissions`() {
        withCredentials(root) {
            assertDoesNotThrow { service.listOrgUsers(orgId1!!) }
        }
        withCredentials(user) {
            assertThrows<AuthorizationException> { service.listOrgUsers(orgId1!!) }
        }
        withCredentials(developer) {
            assertThat(service.listOrgUsers(orgId1!!)).extracting("login").doesNotContain("idm-developer")
        }
        withCredentials(root) {
            assertDoesNotThrow { service.checkPermission(Permission.VIEW_HOTELS, hotelId = hotelId1) }
            assertDoesNotThrow {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    hotelId = hotelId1,
                    hotelPartnerId = HotelIdentifier(EPartnerId.PI_TRAVELLINE, "100")
                )
            }
            assertDoesNotThrow {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    organizationId = orgId1,
                    hotelId = hotelId1,
                    hotelPartnerId = HotelIdentifier(EPartnerId.PI_TRAVELLINE, "100")
                )
            }
            assertDoesNotThrow {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    hotelPartnerId = HotelIdentifier(EPartnerId.PI_TRAVELLINE, "100")
                )
            }
            assertDoesNotThrow {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    organizationId = orgId1
                )
            }
            assertThrows<AuthorizationException> {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    organizationId = UUID.randomUUID()
                )
            }
            assertThrows<AuthorizationException> {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    hotelId = 42,
                )
            }
            assertThrows<AuthorizationException> {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    hotelPartnerId = HotelIdentifier(EPartnerId.PI_TRAVELLINE, "missing")
                )
            }
            assertThrows<AuthorizationException> {
                service.checkPermission(
                    Permission.VIEW_HOTELS,
                    hotelId = hotelId2,
                    hotelPartnerId = HotelIdentifier(EPartnerId.PI_TRAVELLINE, "100")
                )
            }
        }
    }

    @Test
    fun `test list roles`() {
        withCredentials(root) {
            service.grantRole(123, orgId1!!, Role.OPERATOR, hotelId = hotelId1)
            val users = service.listOrgUsers(orgId1!!)
            assertThat(users).hasSize(2)
            assertThat(users).extracting("login", "role", "hotelName").containsExactlyInAnyOrder(
                tuple("root", "Владелец", ""),
                tuple("test-user", "Оператор", "Test Hotel 1")
            )
        }
    }
}
