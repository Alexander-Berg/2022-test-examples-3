package ru.yandex.direct.jobs.telephony

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
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
import ru.yandex.direct.common.db.PpcPropertyName
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.db.PpcPropertyNames.TELEPHONY_CALC_LAST_SHOW_TIME_JOB_LAST_TIME
import ru.yandex.direct.common.db.PpcPropertyType
import ru.yandex.direct.core.entity.clientphone.ClientPhoneService
import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils
import ru.yandex.direct.core.entity.clientphone.TelephonyPhoneService
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber
import ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TextBannerInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.time.LocalDateTime

@JobsTest
@ExtendWith(SpringExtension::class)
class TelephonyDetacherJobTest {

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

    private lateinit var job: TelephonyDetacherJob

    @BeforeEach
    fun setUp() {
        val enabledProperty = mock(PpcProperty::class.java)
        telephonyPhoneService = spy(telephonyPhoneService);
        doReturn(true).`when`(enabledProperty).getOrDefault(any())
        doReturn(enabledProperty).`when`(ppcPropertiesSupport).get(PpcPropertyNames.TELEPHONY_DETACHING_ENABLED)

        val daysProperty = mock(PpcProperty::class.java)
        doReturn(10).`when`(daysProperty).getOrDefault(any())
        doReturn(daysProperty).`when`(ppcPropertiesSupport)
                .get(PpcPropertyNames.MAX_DAYS_WITHOUT_SHOWS_FOR_ADV_TELEPHONY_PHONE)

        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId ?: throw RuntimeException("Cannot create client")
        shard = clientInfo.shard

        job = TelephonyDetacherJob(shard, ppcPropertiesSupport, clientPhoneRepository, telephonyPhoneService)
    }

    @Test
    fun execute_success() {
        val calcLastShowTimeJobLastTimeProperty = mock(PpcProperty::class.java)
        doReturn(LocalDateTime.now().minusHours(12)).`when`(calcLastShowTimeJobLastTimeProperty).get()
        doReturn(calcLastShowTimeJobLastTimeProperty).`when`(ppcPropertiesSupport)
            .get(eq(TELEPHONY_CALC_LAST_SHOW_TIME_JOB_LAST_TIME))

        val property = mock(PpcProperty::class.java)
        doReturn(LocalDateTime.now().minusHours(25)).`when`(property).get()
        doReturn(property).`when`(ppcPropertiesSupport)
                .get(eq(PpcPropertyName(
                    TELEPHONY_DETACHER_JOB_LAST_TIME_PROPERTY_PREFIX + shard,
                    PpcPropertyType.LOCAL_DATE_TIME)))

        val disabledPhone = addLinkedPhoneAndCampaign(null)
        val inactivePhone = addLinkedPhoneAndCampaign(20)
        val activePhone = addLinkedPhoneAndCampaign(4)
        val activeBannerPhone = addLinkedPhoneAndBanner(4)
        val inactiveBannerPhone = addLinkedPhoneAndBanner(12)

        // not adv telephony phone
        addTelephonyPhone(null)

        job.execute()

        val captor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
        verify(telephonyPhoneService, times(2))
            .detachTelephony(captor.capture(), eq(false))

        val tryDetachPhoneIds = captor.allValues

        val phoneIds = listOf(
            disabledPhone.id,
            inactivePhone.id,
            activePhone.id,
            activeBannerPhone.id,
            inactiveBannerPhone.id)

        val phonesById = clientPhoneService.getByPhoneIds(clientId, phoneIds).map { it.id to it }.toMap()
        SoftAssertions.assertSoftly {
            it.assertThat(phonesById[disabledPhone.id]?.telephonyPhone).isNotNull
            it.assertThat(phonesById[disabledPhone.id]?.telephonyServiceId).isNotNull

            it.assertThat(phonesById[inactivePhone.id]?.telephonyPhone).isNull()
            it.assertThat(phonesById[inactivePhone.id]?.telephonyServiceId).isNull()
            it.assertThat(tryDetachPhoneIds).contains(inactivePhone.telephonyServiceId)

            it.assertThat(phonesById[activePhone.id]?.telephonyPhone).isNotNull
            it.assertThat(phonesById[activePhone.id]?.telephonyServiceId).isNotNull

            it.assertThat(phonesById[activeBannerPhone.id]?.telephonyPhone).isNotNull
            it.assertThat(phonesById[activeBannerPhone.id]?.telephonyServiceId).isNotNull

            it.assertThat(phonesById[inactiveBannerPhone.id]?.telephonyPhone).isNull()
            it.assertThat(phonesById[inactiveBannerPhone.id]?.telephonyServiceId).isNull()
            it.assertThat(tryDetachPhoneIds).contains(inactiveBannerPhone.telephonyServiceId)
        }
    }

    @Test
    fun execute_failure() {
        val calcLastShowTimeJobLastTimeProperty = mock(PpcProperty::class.java)
        doReturn(LocalDateTime.now().minusHours(12)).`when`(calcLastShowTimeJobLastTimeProperty).get()
        doReturn(calcLastShowTimeJobLastTimeProperty).`when`(ppcPropertiesSupport)
            .get(eq(TELEPHONY_CALC_LAST_SHOW_TIME_JOB_LAST_TIME))
        val property = mock(PpcProperty::class.java)
        doReturn(LocalDateTime.now().minusHours(23)).`when`(property).get()
        doReturn(property).`when`(ppcPropertiesSupport)
                .get(eq(PpcPropertyName(
                    TELEPHONY_DETACHER_JOB_LAST_TIME_PROPERTY_PREFIX + shard,
                    PpcPropertyType.LOCAL_DATE_TIME)))

        verify(spy(job), times(0)).execute()
    }


    @Test
    fun execute_failure_actual_last_show_time_not_calculated() {
        val property = mock(PpcProperty::class.java)
        doReturn(LocalDateTime.now().minusHours(23)).`when`(property).get()
        doReturn(property).`when`(ppcPropertiesSupport)
            .get(eq(PpcPropertyName(
                TELEPHONY_DETACHER_JOB_LAST_TIME_PROPERTY_PREFIX + shard,
                PpcPropertyType.LOCAL_DATE_TIME)))

        verify(spy(job), times(0)).execute()
        verify(spy(property), times(0)).get()
    }

    /**
     * Создать телефон Телефонии, привязанный к кампании c открутками в БК, которые были [daysAgo] дней назад
     * Если [daysAgo] == `null`, то откруток не было
     */
    private fun addLinkedPhoneAndCampaign(daysAgo: Long?): ClientPhone {
        val phone = addTelephonyPhone(daysAgo)
        val campaignInfo = addCampaign(daysAgo)
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignInfo.campaignId, phone.id)
        return phone
    }

    /**
     * Создать телефон Телефонии, привязанный к баннеру, привязанному к кампании, c открутками в БК,
     * которые были [daysAgo] дней назад
     * Если [daysAgo] == `null`, то откруток не было
     */
    private fun addLinkedPhoneAndBanner(daysAgo: Long?): ClientPhone {
        val phone = addTelephonyPhone(daysAgo)
        val bannerInfo = addBanner(daysAgo)
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bannerInfo.bannerId, phone.id)
        return phone
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
            it.telephonyPhone = PhoneNumber().withPhone(ClientPhoneTestUtils.getUniqPhone())
            it.telephonyServiceId = RandomNumberUtils.nextPositiveInteger().toString()
            it.isDeleted = false
            it.lastShowTime = daysAgo?.let { LocalDateTime.now().minusDays(it) }
        }
        return steps.clientPhoneSteps().addPhone(clientId, clientPhone)
    }

    private fun addCampaign(daysAgo: Long?): CampaignInfo {
        val campaign = activeTextCampaign(null, null).apply {
            this.lastShowTime = daysAgo?.let { LocalDateTime.now().minusDays(it) }
        }
        return steps.campaignSteps().createCampaign(campaign, clientInfo)
    }

    private fun addBanner(daysAgo: Long?): TextBannerInfo {
        val campaignInfo = addCampaign(daysAgo)
        val adGroup = steps.adGroupSteps().createDefaultAdGroup(CampaignInfo(clientInfo, campaignInfo.campaign))
        steps.bannerSteps().createDefaultBanner(adGroup)
        return steps.bannerSteps().createDefaultBanner(adGroup)
    }

}
