package ru.yandex.direct.oneshot.oneshots.calltracking

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.calltrackingsettings.service.CalltrackingSettingsService
import ru.yandex.direct.core.entity.domain.model.Domain
import ru.yandex.direct.core.entity.domain.service.DomainService
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest

@OneshotTest
@RunWith(Parameterized::class)
class CalltrackingOnSiteWwwDomainOneshotParameterizedTest(
    private val wwwDomain: String,
    private val domain: String
) {

    companion object {
        @get:ClassRule
        val springClassRule = SpringClassRule()

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf("4nax.ru", "4nax.ru"),
            arrayOf("wwww.classitaly.ru", "wwww.classitaly.ru"),
            arrayOf("www.sigma-tech.ru", "sigma-tech.ru"),
            arrayOf("www.xn--80adrjmfcpo.ru.com", "xn--80adrjmfcpo.ru.com"),
            arrayOf("www.медтехника.online", "медтехника.online"),
            arrayOf("xn--80abhrbahgwjkbsffe.xn--p1ai", "xn--80abhrbahgwjkbsffe.xn--p1ai"),
            arrayOf("юрист.сайт", "юрист.сайт"),
        )
    }

    @get:Rule
    var springMethodRule = SpringMethodRule()

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

    @Before
    fun before() {
        oneshot = CalltrackingOnSiteWwwDomainOneshot(dslContextProvider, domainService)

        val clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        clientId = clientInfo.clientId ?: throw RuntimeException("Cannot get client")
    }

    @After
    fun after() {
        steps.calltrackingSettingsSteps().deleteAll(shard)
        steps.domainSteps().delete(shard, setOf(wwwDomain, domain))
    }

    @Test
    fun test_wwwDomain_replace_domain_from_db() {
        val wwwDomainId = steps.domainSteps().createDomain(shard, Domain().withDomain(wwwDomain)).domainId
        val domainId = if (wwwDomain == domain) {
            wwwDomainId
        } else {
            steps.domainSteps().createDomain(shard, Domain().withDomain(domain)).domainId
        }
        val settingsId = steps.calltrackingSettingsSteps().add(clientId, wwwDomainId)

        oneshot.execute(null, null, shard)

        val actualDomainId = calltrackingSettingsService.getByIds(shard, clientId, listOf(settingsId))[0].domainId
        assertThat(actualDomainId).isEqualTo(domainId)
    }

    @Test
    fun test_wwwDomain_replace_domain_not_from_db() {
        val wwwDomainId = steps.domainSteps().createDomain(shard, Domain().withDomain(wwwDomain)).domainId
        val settingsId = steps.calltrackingSettingsSteps().add(clientId, wwwDomainId)

        oneshot.execute(null, null, shard)

        val actualDomainId = calltrackingSettingsService.getByIds(shard, clientId, listOf(settingsId))[0].domainId

        val domainId = steps.domainSteps().getDomainIdByDomain(shard, listOf(domain))[domain]
        assertThat(actualDomainId).isEqualTo(domainId)
    }
}
