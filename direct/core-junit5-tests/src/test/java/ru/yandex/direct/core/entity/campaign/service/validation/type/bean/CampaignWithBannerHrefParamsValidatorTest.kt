package ru.yandex.direct.core.entity.campaign.service.validation.type.bean

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBannerHrefParams
import ru.yandex.direct.core.entity.hrefparams.validation.defects.HrefParamsDefects
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.field

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CampaignWIthBannerHrefParamsValidatorTest {

    companion object {
        private const val HREF_PARAMS_MAX_LENGTH = 1024
        private const val CORRECT_PARAMS = "utm_medium=cpc&utm_source=yandex&utm_term={campaign_id}"
    }

    private lateinit var validator: CampaignWithBannerHrefParamsValidator

    @BeforeAll
    fun setUp() {
        validator = CampaignWithBannerHrefParamsValidator()
    }

    @Test
    @DisplayName("Empty href parameters, no defects")
    fun validateEmptyParams_noDefects() {
        val campaign = createCampaign()
        val validationResult = validator.apply(campaign)

        assertThat(validationResult).`is`(matchedBy(Matchers.hasNoDefectsDefinitions<Defect<Any>>()))
    }

    @Test
    @DisplayName("Valid non-empty href parameters, no defects")
    fun validateParams_noDefects() {
        val campaign = createCampaign(CORRECT_PARAMS)
        val validationResult = validator.apply(campaign)

        assertThat(validationResult).`is`(matchedBy(Matchers.hasNoDefectsDefinitions<Defect<Any>>()))
    }

    @Test
    @DisplayName("Href parameters too long, with defects")
    fun validateParamsWithMaxLength_defects() {
        val campaign = createCampaign("A".repeat(HREF_PARAMS_MAX_LENGTH + 1))
        val validationResult = validator.apply(campaign)

        val expectedError = validationError(
            PathHelper.path(field(CampaignWithBannerHrefParams.BANNER_HREF_PARAMS)),
            HrefParamsDefects.hrefParamsTooLong()
        )
        assertThat(validationResult).`is`(
            matchedBy(Matchers.hasDefectWithDefinition<Any>(expectedError))
        )
    }

    @ParameterizedTest(name = "Href parameters \"{0}\" make url invalid, with defects")
    @ValueSource(
        strings = [
            "some incorrect value", // мешают пробелы
            "\u0000", // валидная ссылка не содержит нулевой код
        ]
    )
    fun validateBrokenParams_defects(hrefParams: String) {
        val campaign = createCampaign(hrefParams)
        val validationResult = validator.apply(campaign)

        val expectedError = validationError(
            PathHelper.path(field(CampaignWithBannerHrefParams.BANNER_HREF_PARAMS)),
            HrefParamsDefects.hrefWithParamsInvalid()
        )
        assertThat(validationResult).`is`(
            matchedBy(Matchers.hasDefectWithDefinition<Any>(expectedError))
        )
    }

    private fun createCampaign(
        hrefParams: String? = null
    ) = TestCampaigns.defaultTextCampaignWithSystemFields().apply {
        bannerHrefParams = hrefParams
    }
}
