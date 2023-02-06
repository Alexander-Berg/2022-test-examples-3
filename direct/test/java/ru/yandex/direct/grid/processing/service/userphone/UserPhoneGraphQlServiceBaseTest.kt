package ru.yandex.direct.grid.processing.service.userphone

import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.Option // IGNORE-BAD-STYLE DIRECT-125005
import ru.yandex.bolts.collection.impl.ArrayListF // IGNORE-BAD-STYLE DIRECT-125005
import ru.yandex.direct.blackbox.client.BlackboxClient
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.TestUsers.generateNewUser
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.model.api.GdApiResponse
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.TestAuthHelper.setDirectAuthentication
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith
import ru.yandex.inside.passport.PassportUid
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxPhone
import ru.yandex.misc.ip.IpAddress
import java.util.Optional

const val OLD_USER_PHONE = "22222"
const val PASSPORT_USER_PHONE = "+7111*****22"
const val PASSPORT_PHONE_ID = 1L
const val NONEXISTENT_PASSPORT_PHONE_ID = 111L

open class UserPhoneGraphQlServiceBaseTest {

    @Autowired
    protected lateinit var gridContextProvider: GridContextProvider

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var blackboxClient: BlackboxClient

    @Autowired
    protected lateinit var userRepository: UserRepository

    protected lateinit var userInfo: UserInfo
    protected lateinit var blackboxCorrectResponse: BlackboxCorrectResponse
    protected lateinit var blackboxPhone: BlackboxPhone

    @Before
    open fun setUp() {
        userInfo = steps.userSteps().createUser(generateNewUser().withPhone(OLD_USER_PHONE))

        mockPassportResponse(userInfo)
        setDirectAuthentication(userInfo.user!!)
    }

    @After
    open fun tearDown() {
        reset(blackboxClient)
    }

    private fun mockPassportResponse(userInfo: UserInfo) {
        blackboxCorrectResponse = mock(BlackboxCorrectResponse::class.java)
        `when`(blackboxClient.userInfoBulk(
            any(IpAddress::class.java),
            anyList(),
            isNull(),
            eq(Optional.empty()),
            eq(Optional.empty()),
            anyBoolean(),
            any(),
            anyString())).thenReturn(mapOf(PassportUid(userInfo.uid) to blackboxCorrectResponse))
        blackboxPhone = mock(BlackboxPhone::class.java)

        `when`(blackboxCorrectResponse.o).thenReturn(Option.of(blackboxCorrectResponse))
        `when`(blackboxCorrectResponse.phones).thenReturn(ArrayListF(listOf(blackboxPhone)))
        `when`(blackboxPhone.phoneId).thenReturn(PASSPORT_PHONE_ID)
        `when`(blackboxPhone.maskedE164Number).thenReturn(Option.of(PASSPORT_USER_PHONE))
        `when`(blackboxPhone.isConfirmedNumber).thenReturn(Option.of(true))
        `when`(blackboxPhone.isSecureNumber).thenReturn(Option.of(true))
    }
}

fun <T : GdApiResponse> T.hasError(matcher: Matcher<GdDefect>) = assertThat(validationResult, hasErrorsWith(matcher))
