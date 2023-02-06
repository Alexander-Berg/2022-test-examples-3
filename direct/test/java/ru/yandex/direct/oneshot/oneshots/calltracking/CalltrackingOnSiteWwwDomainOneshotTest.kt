package ru.yandex.direct.oneshot.oneshots.calltracking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.direct.core.entity.calltracking.model.SettingsPhone
import ru.yandex.direct.core.entity.calltrackingsettings.service.CalltrackingSettingsService
import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone
import ru.yandex.direct.core.entity.domain.model.Domain
import ru.yandex.direct.core.entity.domain.service.DomainService
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions
import java.time.LocalDate

@OneshotTest
@RunWith(SpringJUnit4ClassRunner::class)
class CalltrackingOnSiteWwwDomainOneshotTest {

    companion object {
        private val DOMAINS = listOf(
            "4nax.ru",
            "wwww.classitaly.ru",
            "www.sigma-tech.ru",
            "www.xn--80adrjmfcpo.ru.com",
            "www.медтехника.online",
            "медтехника2.online",
            "xn--80abhrbahgwjkbsffe.xn--p1ai",
            "юрист.сайт",
        )

        private val COUNTER_ID = RandomNumberUtils.nextPositiveLong()
        private val DEFAULT_PHONE = getUniqPhone()

        private const val WWW_DOMAIN = "www.piter-pro.com"
        private const val DOMAIN = "piter-pro.com"
    }

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var domainService: DomainService

    @Autowired
    lateinit var calltrackingSettingsService: CalltrackingSettingsService

    @Autowired
    lateinit var oneshot: CalltrackingOnSiteWwwDomainOneshot

    private var shard: Int = 0
    private lateinit var clientId: ClientId
    private lateinit var domainToWwwDomainIdAndDomainId: Map<String, Pair<Long, Long>>

    private var wwwDomainId: Long = 0L
    private var domainId: Long = 0L

    @Before
    fun before() {
        oneshot = CalltrackingOnSiteWwwDomainOneshot(dslContextProvider, domainService)

        val clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        clientId = clientInfo.clientId ?: throw RuntimeException("Cannot get client")
        domainToWwwDomainIdAndDomainId = DOMAINS
            .map {
                val wwwDomainId = steps.domainSteps().createDomain(shard, Domain().withDomain(it)).domainId
                val domainId = if (it.startsWith("www.")) {
                    steps.domainSteps().createDomain(shard, Domain().withDomain(it.substring(4))).domainId
                } else {
                    wwwDomainId
                }
                it to (wwwDomainId to domainId)
            }
            .toMap()

        wwwDomainId = steps.domainSteps().createDomain(shard, Domain().withDomain(WWW_DOMAIN)).domainId
        domainId = steps.domainSteps().createDomain(shard, Domain().withDomain(DOMAIN)).domainId
    }

    @After
    fun after() {
        steps.calltrackingSettingsSteps().deleteAll(shard)
        val domains = DOMAINS + WWW_DOMAIN + DOMAIN + DOMAINS.filter { it.startsWith("www.") }.map { it.substring(4) }
        steps.domainSteps().delete(shard, domains)
    }

    @Test
    fun test_wwwDomain_replace() {
        val domainIdsAfterOneshot = domainToWwwDomainIdAndDomainId
            .mapKeys {
                val wwwDomainId = it.value.first
                steps.calltrackingSettingsSteps().add(clientId, wwwDomainId)
            }
            .also { oneshot.execute(null, null, shard) }
            .map { calltrackingSettingsService.getByIds(shard, clientId, listOf(it.key))[0].domainId }
            .toList()

        assertThat(domainIdsAfterOneshot).isEqualTo(domainToWwwDomainIdAndDomainId.map { it.value.second })
    }

    @Test
    fun test_wwwDomain_sameAllSettings() {
        val wwwTime = LocalDate.of(2020, 10, 10).atTime(0, 0)
        val wwwPhones = listOf(SettingsPhone().withPhone(DEFAULT_PHONE).withCreateTime(wwwTime))
        val wwwSettingId = steps.calltrackingSettingsSteps().add(clientId, wwwDomainId, COUNTER_ID, wwwPhones, true)

        val cutWwwTime = wwwTime.minusDays(1)
        val cutWwwPhones = listOf(SettingsPhone().withPhone(DEFAULT_PHONE).withCreateTime(cutWwwTime))
        val cutWwwSettingId = steps.calltrackingSettingsSteps().add(clientId, domainId, COUNTER_ID, cutWwwPhones, true)


        oneshot.execute(null, null, shard)

        val settings = calltrackingSettingsService.getByIds(shard, clientId, listOf(wwwSettingId, cutWwwSettingId))
        SoftAssertions.assertSoftly {
            it.assertThat(settings).hasSize(1)
            it.assertThat(settings[0]).`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(wwwSettingId)))
        }
    }

    @Test
    fun test_wwwDomain_sameNotAllSettings() {
        val firstTime = LocalDate.of(2020, 10, 10).atTime(0, 0)
        val wwwSettingId = steps.calltrackingSettingsSteps()
            .add(clientId, wwwDomainId, COUNTER_ID, listOf(DEFAULT_PHONE), true, firstTime)

        val secondTime = firstTime.minusDays(1)
        val cutWwwSettingId = steps.calltrackingSettingsSteps()
            .add(clientId, domainId, COUNTER_ID, listOf(DEFAULT_PHONE, getUniqPhone()), true, secondTime)

        oneshot.execute(null, null, shard)

        val settings = calltrackingSettingsService.getByIds(shard, clientId, listOf(wwwSettingId, cutWwwSettingId))
        assertThat(settings).hasSize(2)
    }

}
