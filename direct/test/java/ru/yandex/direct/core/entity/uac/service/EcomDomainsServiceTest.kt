package ru.yandex.direct.core.entity.uac.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.uac.model.EcomDomain
import ru.yandex.direct.core.entity.uac.repository.mysql.EcomDomainsRepository
import ru.yandex.direct.gemini.GeminiClient

class EcomDomainsServiceTest {

    @Mock
    lateinit var ecomDomainsRepository: EcomDomainsRepository

    @Mock
    lateinit var geminiClient: GeminiClient

    @Mock
    lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    lateinit var ecomDomainsService: EcomDomainsService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        ecomDomainsService = EcomDomainsService(ecomDomainsRepository, geminiClient, ppcPropertiesSupport)
    }

    @Test
    fun getEcomDomainsByUrls_success() {
        val mainMirrors = mapOf(
            "http://domain1.com" to "https://domain1.com/",
            "https://domain2.ru" to "https://mirror.domain2.org/",
            "https://domain3.com" to "http://mirror.ru/"
        )

        val urls = mainMirrors.keys

        val domains = setOf("domain1.com", "mirror.domain2.org", "mirror.ru")

        whenever(geminiClient.getMainMirrors(any()))
            .doReturn(mainMirrors)

        whenever(ecomDomainsRepository.getByDomains(eq(domains)))
            .doAnswer {
                it.getArgument<Set<String>>(0).map { domain ->
                    EcomDomain().withDomain(domain)
                }
            }

        val result = ecomDomainsService.getEcomDomainsByUrls(urls)
        val resultDomains = result.values.map {
            it.domain
        }.toSet()

        assertThat(resultDomains, equalTo(domains))
    }

    @Test
    fun getEcomDomainsByUrls_sameMainMirrors_success() {
        val mainMirrors = mapOf(
            "http://domain1.com" to "https://domain1.com/",
            "https://domain2.ru" to "https://domain1.com/",
            "https://domain3.com" to "http://mirror.ru/"
        )

        val urls = mainMirrors.keys

        val domains = setOf("domain1.com", "mirror.ru")

        whenever(geminiClient.getMainMirrors(any()))
            .doReturn(mainMirrors)

        whenever(ecomDomainsRepository.getByDomains(eq(domains)))
            .doAnswer {
                it.getArgument<Set<String>>(0).map { domain ->
                    EcomDomain().withDomain(domain)
                }
            }

        val result = ecomDomainsService.getEcomDomainsByUrls(urls)
        val resultDomains = result.values.map {
            it.domain
        }.toSet()

        assertThat(resultDomains, equalTo(domains))
    }

    @Test
    fun getEcomDomainsByUrls_emptyInput_success() {
        val urls = emptyList<String>()

        whenever(geminiClient.getMainMirrors(any()))
            .doReturn(emptyMap())

        whenever(ecomDomainsRepository.getByDomains(eq(emptySet())))
            .doReturn(emptyList())

        val result = ecomDomainsService.getEcomDomainsByUrls(urls)

        assertThat(result.size, equalTo(0))
    }

    @Test
    fun getEcomDomainsByUrls_withoutSchema_success() {
        val mainMirrors = mapOf(
            "http://domain1.com" to "https://mirror.com/"
        )

        val urls = mainMirrors.keys + setOf("domain2.ru", "domain3.com")

        val domains = setOf("mirror.com", "domain2.ru", "domain3.com")

        whenever(geminiClient.getMainMirrors(any()))
            .doReturn(mainMirrors)

        whenever(ecomDomainsRepository.getByDomains(eq(domains)))
            .doAnswer {
                it.getArgument<Set<String>>(0).map { domain ->
                    EcomDomain().withDomain(domain)
                }
            }

        val result = ecomDomainsService.getEcomDomainsByUrls(urls)
        val resultDomains = result.values.map {
            it.domain
        }.toSet()

        assertThat(resultDomains, equalTo(domains))
    }

    @Test
    fun getEcomDomainsByUrls_notFound_success() {
        val mainMirrors = mapOf(
            "http://domain1.com" to "https://domain1.com/",
            "https://domain2.ru" to "https://mirror.domain2.org/",
            "https://domain3.com" to "http://mirror.ru/"
        )

        val urls = mainMirrors.keys

        val existingDomains = setOf("domain1.com")

        whenever(geminiClient.getMainMirrors(any()))
            .doReturn(mainMirrors)

        whenever(ecomDomainsRepository.getByDomains(any()))
            .doAnswer {
                it.getArgument<Set<String>>(0)
                    .filter { domain -> existingDomains.contains(domain) }
                    .map { domain -> EcomDomain().withDomain(domain) }
            }

        val result = ecomDomainsService.getEcomDomainsByUrls(urls)
        val resultDomains = result.values.map {
            it.domain
        }.toSet()

        assertThat(resultDomains, equalTo(existingDomains))
    }
}
