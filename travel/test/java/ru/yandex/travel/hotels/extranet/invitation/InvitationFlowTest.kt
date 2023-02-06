package ru.yandex.travel.hotels.extranet.invitation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import ru.yandex.travel.credentials.UserCredentials
import ru.yandex.travel.hotels.extranet.EApproveAuthStatus
import ru.yandex.travel.hotels.extranet.ERequestAuthStatus
import ru.yandex.travel.hotels.extranet.IDMRoleType
import ru.yandex.travel.hotels.extranet.TApproveAuthReq
import ru.yandex.travel.hotels.extranet.TRequestAuthReq
import ru.yandex.travel.hotels.extranet.cache.HotelConnection
import ru.yandex.travel.hotels.extranet.cache.IHotelConnectionService
import ru.yandex.travel.hotels.extranet.entities.HotelIdentifier
import ru.yandex.travel.hotels.extranet.entities.Permission
import ru.yandex.travel.hotels.extranet.errors.AuthorizationException
import ru.yandex.travel.hotels.extranet.repository.InvitationRepository
import ru.yandex.travel.hotels.extranet.service.blackbox.BlackBoxService
import ru.yandex.travel.hotels.extranet.service.hotels.HotelInfoService
import ru.yandex.travel.hotels.extranet.service.invitations.InvitationService
import ru.yandex.travel.hotels.extranet.service.notification.MailSenderService
import ru.yandex.travel.hotels.extranet.service.roles.IdmService
import ru.yandex.travel.hotels.extranet.service.roles.UserRoleServiceImpl
import ru.yandex.travel.hotels.extranet.withCredentials
import ru.yandex.travel.hotels.proto.EPartnerId
import ru.yandex.travel.hotels.proto.THotelId
import javax.transaction.Transactional

@SpringBootTest
@ActiveProfiles("test")
open class InvitationFlowTest {
    @Autowired
    lateinit var authPermissionsRepository: InvitationRepository

    @Autowired
    lateinit var userRoleService: UserRoleServiceImpl

    @MockBean
    lateinit var hotelConnectionService: IHotelConnectionService

    @MockBean
    lateinit var mailSenderService: MailSenderService

    @MockBean
    lateinit var blackBoxService: BlackBoxService

    @Autowired
    lateinit var invitationService: InvitationService

    @Autowired
    lateinit var idmService: IdmService

    @Autowired
    lateinit var hotelInfoService: HotelInfoService

    @Test
    fun contextLoads() {
    }

    @Test
    @Rollback
    @Transactional
    open fun `Test whole flow with most corner cases`() {
        val existingHotelIdentifier = THotelId.newBuilder()
            .setPartnerId(EPartnerId.PI_BOOKING)
            .setOriginalId("0xDEADBEEF")
            .build()
        val hotel = HotelConnection(
            permalink = 100_000L,
            hotelIdentifier = existingHotelIdentifier,
            hotelName = "SampleHotel 5*",
            accountantEmail = "hotel@example.com,hotel2@example.com",
            contractPersonEmail = "hotel3@example.com,hotel4@example.com"
        )
        given(
            this.hotelConnectionService.getByHotelIdKey(
                eq(HotelIdentifier.fromProto(existingHotelIdentifier)),
                anyOrNull()
            )
        ).willReturn(hotel)

        given(
            this.blackBoxService.getUidByLogin(
                eq("yndx-account-manager"), anyOrNull()
            )
        ).willReturn(424242)


        withCredentials(UserCredentials.builder().passportId("12345").login("iamhotelowner").build()) {
            var available = hotelInfoService.listAvailableHotels()
            assertThat(available).isEmpty()

            // check no auth for existing hotel before requesting
            assertThrows<AuthorizationException> {
                userRoleService.checkPermission(
                    Permission.VIEW_PAYMENTS,
                    hotelPartnerId = HotelIdentifier.fromProto(hotel.hotelIdentifier)
                )
            }
            // Request auth with non-existing hotel
            var requestAuthResponse = invitationService.requestInvitation(
                TRequestAuthReq.newBuilder()
                    .setHotelId(hotel.hotelIdentifier.toBuilder().setOriginalId("NO MATCH"))
                    .setEmail("invalid@example.com")
                    .build()
            )
            assertThat(requestAuthResponse.status).isEqualTo(ERequestAuthStatus.RAS_HOTEL_NOT_FOUND)
            // Request auth with invalid email
            requestAuthResponse = invitationService.requestInvitation(
                TRequestAuthReq.newBuilder()
                    .setHotelId(hotel.hotelIdentifier)
                    .setEmail("invalid@example.com")
                    .build()
            )
            assertThat(requestAuthResponse.status).isEqualTo(ERequestAuthStatus.RAS_INVALID_EMAIL)
            // Request Authorization
            verifyZeroInteractions(mailSenderService)
            requestAuthResponse = invitationService.requestInvitation(
                TRequestAuthReq.newBuilder()
                    .setHotelId(hotel.hotelIdentifier)
                    .setEmail("hotel@example.com")
                    .build()
            )
            assertThat(requestAuthResponse.status).isEqualTo(ERequestAuthStatus.RAS_OK)
            verify(mailSenderService, times(1)).sendEmailSync(any(), any(), any(), any())
            // RE-request Authorization should return "ALREADY_REQUESTED"
            requestAuthResponse = invitationService.requestInvitation(
                TRequestAuthReq.newBuilder()
                    .setHotelId(hotel.hotelIdentifier)
                    .setEmail("hotel@example.com")
                    .build()
            )
            assertThat(requestAuthResponse.status).isEqualTo(ERequestAuthStatus.RAS_ALREADY_REQUESTED)
            // Check that nothing is authorized before we approve it
            available = hotelInfoService.listAvailableHotels()
            assertThat(available).isEmpty()
            // Try to approve with invalid token
            var approveResponse =
                invitationService.acceptInvitation(TApproveAuthReq.newBuilder().setToken("invalidtoken").build())
            assertThat(approveResponse.status).isEqualTo(EApproveAuthStatus.AAS_INVALID_TOKEN)
            // Check that nothing is approved
            available = hotelInfoService.listAvailableHotels()
            assertThat(available).isEmpty()
            // Find token in database
            val requestToken = authPermissionsRepository.findAll().first().requestToken
            // Approving with real token
            approveResponse =
                invitationService.acceptInvitation(TApproveAuthReq.newBuilder().setToken(requestToken).build())
            assertThat(approveResponse.status).isEqualTo(EApproveAuthStatus.AAS_OK)
            // Check that we are authorized
            available = hotelInfoService.listAvailableHotels()
            assertThat(available).hasSize(1)
            assertThat(available[0].hotelId.partnerId).isEqualTo(hotel.hotelIdentifier.partnerId)
            assertThat(available[0].hotelId.originalId).isEqualTo(hotel.hotelIdentifier.originalId)
            assertThat(available[0].title).isEqualTo(hotel.getTitle())
            // Check repeated approve returns "already approved"
            approveResponse =
                invitationService.acceptInvitation(
                    TApproveAuthReq.newBuilder().setToken(requestToken).build()
                )
            assertThat(approveResponse.status).isEqualTo(EApproveAuthStatus.AAS_ALREADY_APPROVED)
            available = hotelInfoService.listAvailableHotels()
            assertThat(available).hasSize(1)
            // Check request after approval return "already approved"
            requestAuthResponse = invitationService.requestInvitation(
                TRequestAuthReq.newBuilder()
                    .setHotelId(hotel.hotelIdentifier)
                    .setEmail("hotel@example.com")
                    .build()
            )
            assertThat(requestAuthResponse.status).isEqualTo(ERequestAuthStatus.RAS_ALREADY_APPROVED)

            // Check that in the end we've sent only one email
            verifyZeroInteractions(mailSenderService)

            // Check that we have access to hotel
            userRoleService.checkPermission(
                Permission.VIEW_PAYMENTS,
                hotelPartnerId = HotelIdentifier.fromProto(hotel.hotelIdentifier)
            )
        }
        // Check that account manager has no access to hotel before granting access
        val accountManagerCredentials =
            UserCredentials.builder().passportId("424242").login("yndx-account-manager").build()
        withCredentials(accountManagerCredentials) {
            assertThrows<AuthorizationException> {
                userRoleService.checkPermission(
                    Permission.VIEW_PAYMENTS,
                    hotelPartnerId = HotelIdentifier.fromProto(hotel.hotelIdentifier)
                )
            }
        }

        // grant role
        assertThat(
            idmService.grantIdmRole(
                IDMRoleType.IDM_ROLE_TYPE_ACCOUNT_MANAGER,
                "account-manager@yandex-team.ru",
                "yndx-account-manager"
            ).code
        ).isEqualTo(0)

        // subsequent grant returns false
        assertThat(
            idmService.grantIdmRole(
                IDMRoleType.IDM_ROLE_TYPE_ACCOUNT_MANAGER,
                "account-manager@yandex-team.ru",
                "yndx-account-manager"
            ).code
        ).isEqualTo(1)

        withCredentials(accountManagerCredentials) {
            userRoleService.checkPermission(
                Permission.VIEW_PAYMENTS,
                hotelPartnerId = HotelIdentifier.fromProto(hotel.hotelIdentifier)
            )
        }

        // revoke idm role
        assertThat(
            idmService.removeIdmRole(
                IDMRoleType.IDM_ROLE_TYPE_ACCOUNT_MANAGER,
                "account-manager@yandex-team.ru",
            ).code
        ).isEqualTo(0)
        // subsequent revocation returns false
        assertThat(
            idmService.removeIdmRole(
                IDMRoleType.IDM_ROLE_TYPE_ACCOUNT_MANAGER,
                "account-manager@yandex-team.ru",
            ).code
        ).isEqualTo(0)
        assertThat(
            idmService.removeIdmRole(
                IDMRoleType.IDM_ROLE_TYPE_ACCOUNT_MANAGER,
                "account-manager@yandex-team.ru",
            ).warning
        ).isNotEmpty()
        withCredentials(accountManagerCredentials) {
            assertThrows<AuthorizationException> {
                userRoleService.checkPermission(
                    Permission.VIEW_PAYMENTS,
                    hotelPartnerId = HotelIdentifier.fromProto(hotel.hotelIdentifier)
                )
            }
        }
    }
}

