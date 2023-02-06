package ru.yandex.direct.internaltools.tools.ecom

import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.uac.model.EcomDomain
import ru.yandex.direct.core.entity.uac.repository.mysql.EcomDomainsRepository
import ru.yandex.direct.core.entity.uac.service.EcomDomainsService
import ru.yandex.direct.gemini.GeminiClient
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult
import ru.yandex.direct.internaltools.tools.ecom.model.AddEcomDomainParam
import ru.yandex.direct.internaltools.tools.ecom.model.EcomDomainInfo
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.*
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.StringDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path

class EcomDomainAddToolTest {

    private lateinit var ecomDomainAddTool: EcomDomainAddTool

    @Mock
    private lateinit var ecomDomainsService: EcomDomainsService

    @Mock
    private lateinit var ecomDomainsRepository: EcomDomainsRepository

    @Mock
    private lateinit var geminiClient: GeminiClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        ecomDomainAddTool = EcomDomainAddTool(ecomDomainsService, ecomDomainsRepository, geminiClient)
    }

    @Test
    fun validate_withEmptyUrl() {
        val input = AddEcomDomainParam()

        val validationResult = ecomDomainAddTool.validate(input)

        assertThat(validationResult)
            .`is`(matchedBy(hasDefectDefinitionWith<AddEcomDomainParam>(validationError(
                path(field("Домен")), StringDefects.notEmptyString()
            ))))
    }

    @Test
    fun validate_withInvalidUrl() {
        val input = AddEcomDomainParam()
        input.url = "invalid url"

        val validationResult = ecomDomainAddTool.validate(input)

        assertThat(validationResult)
            .`is`(matchedBy(hasDefectDefinitionWith<AddEcomDomainParam>(validationError(
                path(field("Домен")), invalidValue()
            ))))
    }

    @Test
    fun validate_withoutSchema() {
        val input = AddEcomDomainParam()
        input.url = "yandex.ru"

        val validationResult = ecomDomainAddTool.validate(input)

        assertThat(validationResult)
            .`is`(matchedBy(hasDefectDefinitionWith<AddEcomDomainParam>(validationError(
                path(field("Домен")), invalidValue()
            ))))
    }

    @Test
    fun process_addDomain() {
        val url = "https://yandex.ru"
        val input = AddEcomDomainParam()
        input.url = url
        input.offersCount = 55
        input.permanent = true

        val validationResult = ecomDomainAddTool.validate(input)

        assertThat(validationResult)
            .`is`(matchedBy(hasNoDefectsDefinitions<AddEcomDomainParam>()))

        whenever(ecomDomainsService.getEcomDomainsByUrls(any()))
            .doReturn(mapOf())

        whenever(geminiClient.getMainMirrors(any()))
            .doReturn(mapOf(url to url))

        whenever(ecomDomainsRepository.insert(any()))
            .doReturn(1)

        val result = ecomDomainAddTool.process(input)

        verify(ecomDomainsRepository).insert(eq(listOf(
            EcomDomain()
                .withDomain("yandex.ru")
                .withSchema("https")
                .withOffersCount(55)
                .withIsPermanent(true)
        )))

        assertThat(result).isNotNull
        assertThat(result).isOfAnyClassIn(InternalToolMassResult::class.java)

        @Suppress("unchecked_cast")
        val resultData = (result as InternalToolMassResult<EcomDomainInfo>).data
        assertThat(resultData).hasSize(1)
        assertThat(resultData[0]).isEqualTo(EcomDomainInfo("ADDED", "https", "yandex.ru", 55, true))
    }

    @Test
    fun process_addDomainWithMainMirror() {
        val url = "https://yandex.ru"
        val mirror = "http://yandex2.ru"
        val input = AddEcomDomainParam()
        input.url = url
        input.offersCount = 0
        input.permanent = false

        val validationResult = ecomDomainAddTool.validate(input)

        assertThat(validationResult)
            .`is`(matchedBy(hasNoDefectsDefinitions<AddEcomDomainParam>()))

        whenever(ecomDomainsService.getEcomDomainsByUrls(any()))
            .doReturn(mapOf())

        whenever(geminiClient.getMainMirrors(any()))
            .doReturn(mapOf(url to mirror))

        whenever(ecomDomainsRepository.insert(any()))
            .doReturn(1)

        val result = ecomDomainAddTool.process(input)

        verify(ecomDomainsRepository).insert(eq(listOf(
            EcomDomain()
                .withDomain("yandex2.ru")
                .withSchema("http")
                .withOffersCount(0)
                .withIsPermanent(false)
        )))

        assertThat(result).isNotNull
        assertThat(result).isOfAnyClassIn(InternalToolMassResult::class.java)

        @Suppress("unchecked_cast")
        val resultData = (result as InternalToolMassResult<EcomDomainInfo>).data
        assertThat(resultData).hasSize(1)
        assertThat(resultData[0]).isEqualTo(EcomDomainInfo("ADDED", "http", "yandex2.ru", 0, false))
    }

    @Test
    fun process_updateDomain() {
        val input = AddEcomDomainParam()
        input.url = "https://yandex.ru"
        input.offersCount = 55
        input.permanent = true

        val validationResult = ecomDomainAddTool.validate(input)

        assertThat(validationResult)
            .`is`(matchedBy(hasNoDefectsDefinitions<AddEcomDomainParam>()))

        whenever(ecomDomainsService.getEcomDomainsByUrls(any()))
            .doReturn(mapOf(input.url to
                EcomDomain()
                    .withId(1)
                    .withSchema("http")
                    .withDomain("yandex2.ru")
                    .withOffersCount(40)
                    .withIsPermanent(false)
            ))

        whenever(ecomDomainsRepository.update(any()))
            .doReturn(1)

        val result = ecomDomainAddTool.process(input)

        assertThat(result).isNotNull
        assertThat(result).isOfAnyClassIn(InternalToolMassResult::class.java)

        @Suppress("unchecked_cast")
        val resultData = (result as InternalToolMassResult<EcomDomainInfo>).data
        assertThat(resultData).hasSize(1)
        assertThat(resultData[0]).isEqualTo(EcomDomainInfo("UPDATED", "http", "yandex2.ru", 40, true))
    }
}
