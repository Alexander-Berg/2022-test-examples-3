package ru.yandex.direct.grid.processing.service.userphone

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames.PHONE_VERIFICATION_MAX_CALLS_COUNT_PER_DAY
import ru.yandex.direct.core.service.integration.passport.PassportService
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.container.agency.GdAgencyInfo.PHONE
import ru.yandex.direct.grid.processing.model.api.GdValidationResult
import ru.yandex.direct.grid.processing.model.userphone.GdConfirmAndBindPhoneCommitContainer
import ru.yandex.direct.grid.processing.model.userphone.GdConfirmAndBindPhoneCommitPayload
import ru.yandex.direct.grid.processing.model.userphone.GdConfirmAndBindPhoneSubmitPayload
import ru.yandex.direct.grid.processing.model.userphone.GdPassportPhone
import ru.yandex.direct.grid.processing.model.userphone.GdUserPhonesPayload
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.alreadyBindAndSecure
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.confirmationsLimitExceeded
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.emptyPhone
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.internalPassportError
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.invalidCode
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.invalidPhone
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.smsLimitExceeded
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.tooManyRequests
import ru.yandex.direct.grid.processing.service.userphone.validation.UserPhoneDefects.unexpectedError
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.inside.passport.internal.api.models.phone.PhoneSubmitResponse
import ru.yandex.inside.passport.internal.api.models.validation.ValidatePhoneNumberResponse

private const val ANOTHER_PASSPORT_USER_PHONE = "+71112221133"
private const val PASSPORT_MASKED_USER_PHONE = "+7111*****22"

private const val CODE = "123456"
private const val TRACK_ID = "rqwfrqw42134234"
private const val ANOTHER_TRACK_ID = "424323weghergh34"

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class UserPhonePublicGraphQlServiceTest : UserPhoneGraphQlServiceBaseTest() {

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var passportService: PassportService

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Before
    override fun setUp() {
        super.setUp()
        ppcPropertiesSupport.remove(PHONE_VERIFICATION_MAX_CALLS_COUNT_PER_DAY.name)
    }

    @After
    override fun tearDown() {
        super.tearDown()
        reset(passportService)
    }

    @Test
    fun userPhones_PassportPhonesReturned() {
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .getUserPhonesPublic()
            .checkEquals(GdUserPhonesPayload().apply {
                passportPhones = listOf(
                    GdPassportPhone().apply {
                        isConfirmed = true
                        phone = PASSPORT_MASKED_USER_PHONE
                        phoneId = PASSPORT_PHONE_ID
                    })
            })
    }

    @Test
    fun confirmAndBindPhoneSubmit_Success() {
        `when`(passportService.confirmAndBindSubmit(anyString(), anyString()))
            .thenReturn(PhoneSubmitResponse().apply {
                trackId = TRACK_ID
                errors = emptyList()
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(PASSPORT_USER_PHONE)
            .checkEquals(GdConfirmAndBindPhoneSubmitPayload().apply {
                trackId = TRACK_ID
                validationResult = GdValidationResult().apply {
                    errors = emptyList()
                }
            })
    }

    @Test
    fun confirmAndBindPhoneSubmit_NoSmsFlow_Success() {
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit("+78005553535")
            .checkEquals(GdConfirmAndBindPhoneSubmitPayload().apply {
                trackId = "test_track_id"
            })
    }

    @Test
    fun confirmAndBindPhoneSubmit_InvalidNumberError() {
        `when`(passportService.confirmAndBindSubmit(anyString(), anyString()))
            .thenReturn(PhoneSubmitResponse().apply {
                errors = listOf("number.invalid")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(PASSPORT_USER_PHONE)
            .hasError(gridDefect(path(field(PHONE)), invalidPhone()))
    }

    @Test
    fun confirmAndBindPhoneSubmit_EmptyNumberError() {
        `when`(passportService.confirmAndBindSubmit(anyString(), anyString()))
            .thenReturn(PhoneSubmitResponse().apply {
                errors = listOf("number.empty")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(PASSPORT_USER_PHONE)
            .hasError(gridDefect(path(field(PHONE)), emptyPhone()))
    }

    @Test
    fun confirmAndBindPhoneSubmit_SmsLimitExceededError() {
        `when`(passportService.confirmAndBindSubmit(anyString(), anyString()))
            .thenReturn(PhoneSubmitResponse().apply {
                errors = listOf("sms_limit.exceeded")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(PASSPORT_USER_PHONE)
            .hasError(gridDefect(path(field(PHONE)), smsLimitExceeded()))
    }

    @Test
    fun confirmAndBindPhoneSubmit_AlreadyBindAndSecureError() {
        `when`(passportService.confirmAndBindSubmit(anyString(), anyString()))
            .thenReturn(PhoneSubmitResponse().apply {
                errors = listOf("phone_secure.bound_and_confirmed")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(PASSPORT_USER_PHONE)
            .hasError(gridDefect(path(field(PHONE)), alreadyBindAndSecure(PASSPORT_PHONE_ID)))
    }

    @Test
    fun confirmAndBindPhoneSubmit_UnexpectedError() {
        `when`(passportService.confirmAndBindSubmit(anyString(), anyString()))
            .thenReturn(PhoneSubmitResponse().apply {
                errors = listOf("unexpected")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(PASSPORT_USER_PHONE)
            .hasError(gridDefect(path(field(PHONE)), unexpectedError()))
    }

    @Test
    fun confirmAndBindPhoneSubmit_PassportIsDown_PassportInternalErrorReturned() {
        `when`(passportService.confirmAndBindSubmit(anyString(), eq(ANOTHER_PASSPORT_USER_PHONE)))
            .thenReturn(null)

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(ANOTHER_PASSPORT_USER_PHONE)
            .hasError(gridDefect(path(), internalPassportError()))
    }

    @Test
    fun confirmAndBindPhoneSubmit_TooManyRequestsError() {
        ppcPropertiesSupport.set(PHONE_VERIFICATION_MAX_CALLS_COUNT_PER_DAY.name, "0")

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneSubmit(PASSPORT_USER_PHONE)
            .hasError(gridDefect(path(), tooManyRequests()))
    }

    @Test
    fun confirmAndBindPhoneCommit_Success() {
        `when`(passportService.confirmAndBindCommit(anyString(), anyString(), anyString()))
            .thenReturn(ValidatePhoneNumberResponse().apply {
                phoneId = PASSPORT_PHONE_ID
                errors = emptyList()
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = TRACK_ID)
            .checkEquals(GdConfirmAndBindPhoneCommitPayload().apply {
                phoneId = PASSPORT_PHONE_ID
                validationResult = GdValidationResult().apply {
                    errors = emptyList()
                }
            })
    }

    @Test
    fun confirmAndBindPhoneCommit_NoPhoneId_UnexpectedError() {
        `when`(passportService.confirmAndBindCommit(anyString(), anyString(), anyString()))
            .thenReturn(ValidatePhoneNumberResponse().apply {
                errors = emptyList()
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = TRACK_ID)
            .hasError(gridDefect(path(), unexpectedError()))
    }

    @Test
    fun confirmAndBindPhoneCommit_NoSmsFlow_Success() {
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = "test_track_id")
            .checkEquals(GdConfirmAndBindPhoneCommitPayload().apply {
                phoneId = PASSPORT_PHONE_ID
            })
    }

    @Test
    fun confirmAndBindPhoneCommit_InvalidCodeError() {
        `when`(passportService.confirmAndBindCommit(anyString(), anyString(), anyString()))
            .thenReturn(ValidatePhoneNumberResponse().apply {
                errors = listOf("code.invalid")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = TRACK_ID)
            .hasError(gridDefect(path(field(GdConfirmAndBindPhoneCommitContainer.CODE)), invalidCode()))
    }

    @Test
    fun confirmAndBindPhoneCommit_ConfirmationsLimitExceededError() {
        `when`(passportService.confirmAndBindCommit(anyString(), anyString(), anyString()))
            .thenReturn(ValidatePhoneNumberResponse().apply {
                errors = listOf("confirmations_limit.exceeded")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = TRACK_ID)
            .hasError(gridDefect(path(field(GdConfirmAndBindPhoneCommitContainer.CODE)), confirmationsLimitExceeded()))
    }

    @Test
    fun confirmAndBindPhoneCommit_UnexpectedError() {
        `when`(passportService.confirmAndBindCommit(anyString(), anyString(), anyString()))
            .thenReturn(ValidatePhoneNumberResponse().apply {
                errors = listOf("unexpected")
            })

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = TRACK_ID)
            .hasError(gridDefect(path(), unexpectedError()))
    }

    @Test
    fun confirmAndBindPhoneCommit_PassportIsDown_PassportInternalErrorReturned() {
        `when`(passportService.confirmAndBindCommit(anyString(), eq(ANOTHER_TRACK_ID), anyString()))
            .thenReturn(null)

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = ANOTHER_TRACK_ID)
            .hasError(gridDefect(path(), internalPassportError()))
    }

    @Test
    fun confirmAndBindPhoneCommit_TooManyRequestsError() {
        ppcPropertiesSupport.set(PHONE_VERIFICATION_MAX_CALLS_COUNT_PER_DAY.name, "0")

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .confirmAndBindPhoneCommit(code = CODE, trackId = TRACK_ID)
            .hasError(gridDefect(path(), tooManyRequests()))
    }
}
