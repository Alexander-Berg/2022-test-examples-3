package ru.yandex.direct.oneshot.oneshots.change_permalink_to_head

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository
import ru.yandex.direct.core.entity.organization.model.Organization
import ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.change_permalink_to_head.repository.BannerPermalinksOneshotRepository
import ru.yandex.direct.oneshot.oneshots.change_permalink_to_head.repository.CampaignPermalinksOneshotRepository
import ru.yandex.direct.oneshot.oneshots.change_permalink_to_head.repository.ClientPhoneOneshotRepository
import ru.yandex.direct.organizations.swagger.OrganizationsClient
import ru.yandex.direct.organizations.swagger.model.CompanyChain
import ru.yandex.direct.organizations.swagger.model.MetrikaData
import ru.yandex.direct.organizations.swagger.model.PubApiCompanyData
import ru.yandex.direct.telephony.client.TelephonyClient
import ru.yandex.direct.telephony.client.TelephonyClientException
import ru.yandex.direct.telephony.client.model.TelephonyPhoneRequest
import ru.yandex.direct.test.utils.assertj.Conditions

@OneshotTest
@RunWith(SpringRunner::class)
class ChangePermalinkToHeadOneshotTest {

    companion object {
        private const val PERMALINK_ID = 3L
        private const val HEAD_PERMALINK_ID = 5L
        private const val COUNTER_ID = 15L
        private const val HEAD_COUNTER_ID = 15L
        private const val CHAIN_ID = 77L
        private const val TELEPHONY_SERVICE_ID = "0A"
    }

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var clientPhoneOneshotRepository: ClientPhoneOneshotRepository

    @Autowired
    private lateinit var clientPhoneRepository: ClientPhoneRepository

    @Autowired
    private lateinit var campaignPermalinksOneshotRepository: CampaignPermalinksOneshotRepository

    @Autowired
    private lateinit var testOrganizationRepository: TestOrganizationRepository

    @Autowired
    private lateinit var bannerPermalinksOneshotRepository: BannerPermalinksOneshotRepository

    @Mock
    private lateinit var organizationsClient: OrganizationsClient

    @Mock
    private lateinit var telephonyClient: TelephonyClient

    private lateinit var changePermalinkToHeadOneshot: ChangePermalinkToHeadOneshot

    private lateinit var bannerInfo: NewTextBannerInfo
    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private var shard: Int = 0
    private lateinit var organization: Organization

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)

        changePermalinkToHeadOneshot = ChangePermalinkToHeadOneshot(
            organizationRepository,
            shardHelper,
            telephonyClient,
            organizationsClient,
            clientPhoneOneshotRepository,
            bannerPermalinksOneshotRepository,
            campaignPermalinksOneshotRepository,
            dslContextProvider,
            clientPhoneRepository,
            1,
        )

        bannerInfo = steps.textBannerSteps().createDefaultTextBanner()
        clientInfo = bannerInfo.clientInfo
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard

        val company = PubApiCompanyData()
            .id(PERMALINK_ID)
            .headPermalink(HEAD_PERMALINK_ID)
        val headCompany = PubApiCompanyData()
            .id(HEAD_PERMALINK_ID)
            .chain(CompanyChain().id(CHAIN_ID))
            .metrikaData(MetrikaData().counter(HEAD_COUNTER_ID.toString()))

        Mockito.doReturn(company).`when`(organizationsClient)
            .getSingleOrganizationInfo(
                eq(PERMALINK_ID),
                anyCollection(),
                anyString(),
                eq(null)
            )
        Mockito.doReturn(headCompany).`when`(organizationsClient)
            .getSingleOrganizationInfo(
                eq(HEAD_PERMALINK_ID),
                anyCollection(),
                anyString(),
                eq(null)
            )

        organization = Organization()
            .withPermalinkId(PERMALINK_ID)
            .withChainId(0L)
            .withClientId(clientId)
            .withStatusPublish(OrganizationStatusPublish.PUBLISHED)
        organizationRepository.addOrUpdateOrganizations(shard, listOf(organization))
    }

    /**
     * Проверяем что при выполнении ваншота создается новая организация
     */
    @Test
    fun checkCreateNewOrganization() {
        changePermalinkToHeadOneshot.execute(InputData(
            PERMALINK_ID,
            listOf(clientId.asLong())
        ), null)

        val actualOrganizations = organizationRepository.getAllClientOrganizations(shard, clientId)

        val expectOrganization = Organization()
            .withPermalinkId(HEAD_PERMALINK_ID)
            .withChainId(CHAIN_ID)
            .withClientId(clientId)
            .withStatusPublish(OrganizationStatusPublish.PUBLISHED)

        val soft = SoftAssertions()
        soft.assertThat(actualOrganizations)
            .`as`("Организации клиента")
            .hasSize(2)
        soft.assertThat(actualOrganizations.first { o -> o.permalinkId == HEAD_PERMALINK_ID })
            .`as`("Организация с новым пермалинком")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectOrganization)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertThat(actualOrganizations.first { o -> o.permalinkId == PERMALINK_ID })
            .`as`("Организация со старым пермалинком")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(organization)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertAll()
    }


    /**
     * Проверяем что при выполнении ваншота не создается новая организация, если она уже есть
     */
    @Test
    fun doNotCreateNewOrganizationIfItExists() {
        val headOrganization = Organization()
            .withPermalinkId(HEAD_PERMALINK_ID)
            .withChainId(99L)
            .withClientId(clientId)
            .withStatusPublish(OrganizationStatusPublish.PUBLISHED)

        organizationRepository.addOrUpdateOrganizations(shard, listOf(headOrganization))

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        val actualOrganizations = organizationRepository.getAllClientOrganizations(shard, clientId)

        val soft = SoftAssertions()
        soft.assertThat(actualOrganizations)
            .`as`("Организации клиента")
            .hasSize(2)
        soft.assertThat(actualOrganizations.first { o -> o.permalinkId == HEAD_PERMALINK_ID })
            .`as`("Организация с новым пермалинком")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(headOrganization)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertThat(actualOrganizations.first { o -> o.permalinkId == PERMALINK_ID })
            .`as`("Организация со старым пермалинком")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(organization)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertAll()
    }

    /**
     * Проверяем изменение пермалинка в таблице client_phone
     */
    @Test
    fun checkClientPhonePermalinkChange() {
        val clientPhone = ClientPhone()
            .withClientId(clientId)
            .withPermalinkId(PERMALINK_ID)
            .withPhoneType(ClientPhoneType.MANUAL)
            .withPhoneNumber(PhoneNumber()
                .withPhone("+1612345001"))
            .withCounterId(COUNTER_ID)
            .withComment("")
            .withTelephonyServiceId(TELEPHONY_SERVICE_ID)

        clientPhoneRepository.add(clientId, listOf(clientPhone))

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        val actualClientPhones = clientPhoneRepository.getByClientId(clientId)

        val expectClientPhone = ClientPhone()
            .withClientId(clientId)
            .withPermalinkId(HEAD_PERMALINK_ID)
            .withPhoneType(ClientPhoneType.MANUAL)
            .withPhoneNumber(clientPhone.phoneNumber)
            .withCounterId(HEAD_COUNTER_ID)
            .withComment(clientPhone.comment)

        val soft = SoftAssertions()
        soft.assertThat(actualClientPhones)
            .`as`("Номера телефонов клиента")
            .hasSize(1)
        soft.assertThat(actualClientPhones.firstOrNull())
            .`as`("Данные номера телефона изменились")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectClientPhone)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertAll()
    }

    /**
     * Проверяем изменение пермалинка в таблице campaign_permalink
     */
    @Test
    fun checkCampaignPermalinkChange() {
        testOrganizationRepository.linkDefaultOrganizationToCampaign(
            shard,
            PERMALINK_ID,
            bannerInfo.campaignId
        )

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        val campaignIdToPermalinkId = organizationRepository
            .getDefaultPermalinkIdsByCampaignId(shard, setOf(bannerInfo.campaignId))

        val soft = SoftAssertions()
        soft.assertThat(campaignIdToPermalinkId)
            .`as`("Связь кампаний с пермалинками")
            .hasSize(1)
            .containsEntry(bannerInfo.campaignId, HEAD_PERMALINK_ID)
        soft.assertAll()
    }

    /**
     * Проверяем изменение пермалинка в таблице banner_permalink
     */
    @Test
    fun checkBannerPermalinkChange() {
        testOrganizationRepository.addAutoPermalink(
            shard,
            bannerInfo.bannerId,
            PERMALINK_ID
        )

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        val bannerIdToPermalinkId = organizationRepository
            .getBannerPermalinkByBannerIds(shard, setOf(bannerInfo.bannerId))
            .mapValues { it.value.map { b -> b.permalinkId }.toSet() }

        val soft = SoftAssertions()
        soft.assertThat(bannerIdToPermalinkId)
            .`as`("Связь баннеров с пермалинками")
            .hasSize(1)
            .containsEntry(bannerInfo.bannerId, setOf(HEAD_PERMALINK_ID))
        soft.assertAll()
    }

    /**
     * Проверяем изменение пермалинка у нескольких баннеров, в большем количестве чем лимит (=1) в запросе их получения
     */
    @Test
    fun checkBannerPermalinkChangeForTwoBanners() {
        val secondBannerInfo = steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withClientInfo(clientInfo))

        testOrganizationRepository.addAutoPermalink(
            shard,
            bannerInfo.bannerId,
            PERMALINK_ID
        )
        testOrganizationRepository.addAutoPermalink(
            shard,
            secondBannerInfo.bannerId,
            PERMALINK_ID
        )

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        val bannerIdToPermalinkId = organizationRepository
            .getBannerPermalinkByBannerIds(shard, setOf(bannerInfo.bannerId, secondBannerInfo.bannerId))
            .mapValues { it.value.map { b -> b.permalinkId }.toSet() }

        val soft = SoftAssertions()
        soft.assertThat(bannerIdToPermalinkId)
            .`as`("Связь баннеров с пермалинками")
            .hasSize(2)
            .containsEntry(bannerInfo.bannerId, setOf(HEAD_PERMALINK_ID))
            .containsEntry(secondBannerInfo.bannerId, setOf(HEAD_PERMALINK_ID))
        soft.assertAll()
    }

    /**
     * Если включен фильтр по клиенту -> у других клиентов пермалинк/организации не изменяются
     */
    @Test
    fun checkFilterByClient() {
        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID, listOf(clientId.asLong() + 1)), null)

        val actualOrganizations = organizationRepository.getAllClientOrganizations(shard, clientId)

        val soft = SoftAssertions()
        soft.assertThat(actualOrganizations)
            .`as`("Организации клиента")
            .hasSize(1)
        soft.assertThat(actualOrganizations.firstOrNull())
            .`as`("Организация у клиента не изменилась")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(organization)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertAll()
    }

    /**
     * Проверяем что при изменении данных в client_phone отправляем их в телефонию
     */
    @Test
    fun checkSendNewDataToTelephony() {
        val clientPhone = ClientPhone()
            .withClientId(clientId)
            .withPermalinkId(PERMALINK_ID)
            .withPhoneType(ClientPhoneType.TELEPHONY)
            .withPhoneNumber(PhoneNumber()
                .withPhone("+1612345001"))
            .withCounterId(COUNTER_ID)
            .withComment("")
            .withTelephonyServiceId(TELEPHONY_SERVICE_ID)
        clientPhoneRepository.add(clientId, listOf(clientPhone))

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        verify(telephonyClient).linkServiceNumber(clientId.asLong(), TelephonyPhoneRequest()
            .withCounterId(HEAD_COUNTER_ID)
            .withPermalinkId(HEAD_PERMALINK_ID)
            .withRedirectPhone(clientPhone.phoneNumber?.phone)
            .withTelephonyServiceId(clientPhone.telephonyServiceId))
    }

    /**
     * Проверяем что при изменении данных в client_phone c типом не telephony -> не отправляем их в телефонию
     */
    @Test
    fun checkDoNotSendNewDataToTelephonyForManualType() {
        val clientPhone = ClientPhone()
            .withClientId(clientId)
            .withPermalinkId(PERMALINK_ID)
            .withPhoneType(ClientPhoneType.MANUAL)
            .withPhoneNumber(PhoneNumber()
                .withPhone("+1612345001"))
            .withCounterId(COUNTER_ID)
            .withComment("")
            .withTelephonyServiceId(TELEPHONY_SERVICE_ID)
        clientPhoneRepository.add(clientId, listOf(clientPhone))

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        verify(telephonyClient, Mockito.never()).linkServiceNumber(anyLong(), any())
    }

    /**
     * Проверяем что при ошибке отправки в телефонию все изменения откатываются обратно
     */
    @Test
    fun checkWhenExceptionAllChangesRollback() {

        given(telephonyClient.linkServiceNumber(anyLong(), any())).willThrow(TelephonyClientException("exception"))

        val clientPhone = ClientPhone()
            .withClientId(clientId)
            .withPermalinkId(PERMALINK_ID)
            .withPhoneType(ClientPhoneType.TELEPHONY)
            .withPhoneNumber(PhoneNumber()
                .withPhone("+1612345001"))
            .withCounterId(COUNTER_ID)
            .withComment("")
            .withTelephonyServiceId(TELEPHONY_SERVICE_ID)
        clientPhoneRepository.add(clientId, listOf(clientPhone))

        testOrganizationRepository.linkDefaultOrganizationToCampaign(
            shard,
            PERMALINK_ID,
            bannerInfo.campaignId
        )

        testOrganizationRepository.addAutoPermalink(
            shard,
            bannerInfo.bannerId,
            PERMALINK_ID
        )

        changePermalinkToHeadOneshot.execute(InputData(PERMALINK_ID), null)

        val actualOrganizations = organizationRepository.getAllClientOrganizations(shard, clientId)
        val actualClientPhones = clientPhoneRepository.getByClientId(clientId)
        val campaignIdToPermalinkId = organizationRepository
            .getDefaultPermalinkIdsByCampaignId(shard, setOf(bannerInfo.campaignId))
        val bannerIdToPermalinkId = organizationRepository
            .getBannerPermalinkByBannerIds(shard, setOf(bannerInfo.bannerId))
            .mapValues { it.value.map { b -> b.permalinkId }.toSet() }

        val soft = SoftAssertions()
        soft.assertThat(actualOrganizations)
            .`as`("Организации клиента")
            .hasSize(1)
        soft.assertThat(actualOrganizations.firstOrNull())
            .`as`("Организация не изменилась")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(organization)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertThat(actualClientPhones)
            .`as`("Номера телефонов клиента")
            .hasSize(1)
        soft.assertThat(actualClientPhones.firstOrNull())
            .`as`("Данные номера телефона не изменились")
            .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(clientPhone)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
        soft.assertThat(campaignIdToPermalinkId)
            .`as`("Связь кампаний с пермалинками")
            .hasSize(1)
            .containsEntry(bannerInfo.campaignId, PERMALINK_ID)
        soft.assertThat(bannerIdToPermalinkId)
            .`as`("Связь баннеров с пермалинками")
            .hasSize(1)
            .containsEntry(bannerInfo.bannerId, setOf(PERMALINK_ID))
        soft.assertAll()
    }
}
