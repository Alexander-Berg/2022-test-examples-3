package ru.yandex.direct.jobs.telephony

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.initMocks
import org.mockito.Spy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.clientphone.ClientPhoneService
import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils
import ru.yandex.direct.core.entity.clientphone.TelephonyPhoneService
import ru.yandex.direct.core.entity.clientphone.TelephonyPhoneValue
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber
import ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.time.LocalDateTime

@JobsTest
@ExtendWith(SpringExtension::class)
class TelephonyAttacherJobTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientPhoneService: ClientPhoneService

    @Autowired
    private lateinit var clientPhoneRepository: ClientPhoneRepository

    @Autowired
    private lateinit var telephonyPhoneService: TelephonyPhoneService

    @Mock
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard = 0

    private lateinit var job: TelephonyAttacherJob

    @BeforeEach
    fun setUp() {
        val enabledProperty = mock(PpcProperty::class.java)
        telephonyPhoneService = spy(telephonyPhoneService);
        doReturn(true).`when`(enabledProperty).getOrDefault(any())
        doReturn(enabledProperty).`when`(ppcPropertiesSupport).get(PpcPropertyNames.TELEPHONY_ATTACHING_ENABLED)

        val daysProperty = mock(PpcProperty::class.java)
        doReturn(10).`when`(daysProperty).getOrDefault(any())
        doReturn(daysProperty).`when`(ppcPropertiesSupport)
                .get(PpcPropertyNames.MAX_DAYS_WITHOUT_SHOWS_FOR_ADV_TELEPHONY_PHONE)

        doReturn(
            TelephonyPhoneValue(RandomNumberUtils.nextPositiveInteger().toString(), ClientPhoneTestUtils.getUniqPhone())
        ).`when`(telephonyPhoneService).attachTelephony(any(), anyBoolean())

        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId ?: throw RuntimeException("Cannot create client")
        shard = clientInfo.shard

        job = TelephonyAttacherJob(shard, ppcPropertiesSupport, clientPhoneRepository, telephonyPhoneService)
    }

    @Test
    fun execute() {
        val calcLastShowTimeJobLastTimeProperty = mock(PpcProperty::class.java)
        doReturn(LocalDateTime.now().minusHours(12)).`when`(calcLastShowTimeJobLastTimeProperty).get()
        doReturn(calcLastShowTimeJobLastTimeProperty).`when`(ppcPropertiesSupport)
            .get(eq(PpcPropertyNames.TELEPHONY_CALC_LAST_SHOW_TIME_JOB_LAST_TIME))

        val inactivePhoneId = addLinkedPhoneAndCampaign(20)
        val activePhoneId = addLinkedPhoneAndCampaign(null)
        val activePhoneId2 = addLinkedPhoneAndCampaign(3)
        val activePhoneId3 = addLinkedPhoneAndCampaign(5)
        val activePhoneId4 = addTelephonyPhone(null).id

        job.execute()

        val captor: ArgumentCaptor<ClientPhone> = ArgumentCaptor.forClass(ClientPhone::class.java)
        verify(telephonyPhoneService, times(4)).attachTelephony(captor.capture(), eq(false))

        val tryAttachPhoneIds = captor.allValues.map {it.id}
        val phoneIds = listOf(inactivePhoneId, activePhoneId, activePhoneId2, activePhoneId3, activePhoneId4)
        val phonesById = clientPhoneService.getByPhoneIds(clientId, phoneIds).map { it.id to it }.toMap()
        SoftAssertions.assertSoftly {
            it.assertThat(phonesById[inactivePhoneId]?.telephonyPhone).isNull()
            it.assertThat(phonesById[inactivePhoneId]?.telephonyServiceId).isNull()

            it.assertThat(phonesById[activePhoneId]?.telephonyPhone).isNotNull
            it.assertThat(phonesById[activePhoneId]?.telephonyServiceId).isNotNull
            it.assertThat(tryAttachPhoneIds).contains(activePhoneId)

            it.assertThat(phonesById[activePhoneId2]?.telephonyPhone).isNotNull
            it.assertThat(phonesById[activePhoneId2]?.telephonyServiceId).isNotNull
            it.assertThat(tryAttachPhoneIds).contains(activePhoneId2)

            it.assertThat(phonesById[activePhoneId3]?.telephonyPhone).isNotNull
            it.assertThat(phonesById[activePhoneId3]?.telephonyServiceId).isNotNull
            it.assertThat(tryAttachPhoneIds).contains(activePhoneId3)

            it.assertThat(phonesById[activePhoneId4]?.telephonyPhone).isNotNull
            it.assertThat(phonesById[activePhoneId4]?.telephonyServiceId).isNotNull
            it.assertThat(tryAttachPhoneIds).contains(activePhoneId4)
        }
    }

    /**
     * Создать телефон, привязанный к кампании c открутками в БК, которые были [daysAgo] дней назад
     * Если [daysAgo] == `null`, то откруток не было
     */
    private fun addLinkedPhoneAndCampaign(daysAgo: Long?): Long {
        val phone = addTelephonyPhone(daysAgo)
        addCampaignAndLinkPhone(phone, daysAgo)
        return phone.id
    }

    private fun addCampaignAndLinkPhone(phone: ClientPhone, day: Long?) {
        val campaign = activeTextCampaign(null, null).apply {
            this.lastShowTime = day?.let { LocalDateTime.now().minusDays(it) }
        }
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignInfo.campaignId, phone.id)
    }

    private fun addTelephonyPhone(daysAgo: Long?): ClientPhone {
        val permalink = RandomNumberUtils.nextPositiveLong()
        return addTelephonyPhone(permalink, daysAgo)
    }

    private fun addTelephonyPhone(permalink: Long?, daysAgo: Long?): ClientPhone {
        val clientPhone = ClientPhone().also {
            it.clientId = clientId
            it.phoneType = ClientPhoneType.TELEPHONY
            it.phoneNumber = PhoneNumber().withPhone(ClientPhoneTestUtils.getUniqPhone())
            it.permalinkId = permalink
            it.counterId = RandomNumberUtils.nextPositiveLong()
            it.isDeleted = false
            it.lastShowTime = daysAgo?.let { LocalDateTime.now().minusDays(it) }
        }
        return steps.clientPhoneSteps().addPhone(clientId, clientPhone)
    }

}
