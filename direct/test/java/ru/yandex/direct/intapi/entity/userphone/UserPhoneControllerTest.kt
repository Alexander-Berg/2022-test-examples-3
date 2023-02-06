package ru.yandex.direct.intapi.entity.userphone

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.direct.blackbox.client.BlackboxClient
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.intapi.configuration.IntApiTest
import ru.yandex.direct.intapi.entity.userphone.model.CheckVerifiedRequest
import ru.yandex.direct.intapi.entity.userphone.model.CheckVerifiedResponse
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils.fromJson
import ru.yandex.direct.utils.JsonUtils.toJson
import ru.yandex.inside.passport.PassportUid
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxPhone
import ru.yandex.misc.ip.IpAddress
import java.util.*

private const val PASSPORT_MASKED_USER_PHONE = "+7111*****22"
private const val PASSPORT_PHONE_ID = 1L
private const val NONEXISTENT_PASSPORT_PHONE_ID = 2L
private const val NONEXISTENT_UID = 123456L

@IntApiTest
@RunWith(SpringJUnit4ClassRunner::class)
class UserPhoneControllerTest {

    @Autowired
    private lateinit var controller: UserPhoneController

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    protected lateinit var blackboxClient: BlackboxClient

    private lateinit var mockMvc: MockMvc

    private lateinit var userInfo: UserInfo
    private lateinit var blackboxCorrectResponse: BlackboxCorrectResponse
    private lateinit var blackboxPhone: BlackboxPhone

    @Before
    fun setUp() {
        userInfo = steps.userSteps().createDefaultUser()

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
        `when`(blackboxPhone.maskedE164Number).thenReturn(Option.of(PASSPORT_MASKED_USER_PHONE))
        `when`(blackboxPhone.isConfirmedNumber).thenReturn(Option.of(true))
        `when`(blackboxPhone.isSecureNumber).thenReturn(Option.of(true))

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun checkVerified_VerifiedPhoneReturned() {
        mockMvc.doRequest(userInfo.uid, PASSPORT_PHONE_ID)
            .checkEquals(CheckVerifiedResponse().apply {
                uid = userInfo.uid
                verified = true
                phone = PASSPORT_MASKED_USER_PHONE
            })
    }

    @Test
    fun checkVerified_NonexistentPhone_NoPhoneReturned() {
        mockMvc.doRequest(userInfo.uid, NONEXISTENT_PASSPORT_PHONE_ID)
            .checkEquals(CheckVerifiedResponse().apply {
                uid = userInfo.uid
                verified = false
            })
    }

    @Test
    fun checkVerified_NonexistentUser_NoPhoneReturned() {
        mockMvc.doRequest(NONEXISTENT_UID, PASSPORT_PHONE_ID)
            .checkEquals(CheckVerifiedResponse().apply {
                uid = NONEXISTENT_UID
                verified = false
            })
    }
}

private fun MockMvc.doRequest(uid: Long, phoneId: Long): CheckVerifiedResponse {
    val requestBuilder = MockMvcRequestBuilders.post("/user_phones/check_verified")
        .contentType(MediaType.APPLICATION_JSON)
        .content(toJson(CheckVerifiedRequest().apply {
            this.uid = uid
            this.phoneId = phoneId
        }))

    val responseRaw = perform(requestBuilder)
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andReturn().response.contentAsString

    return fromJson(responseRaw, CheckVerifiedResponse::class.java)
}
