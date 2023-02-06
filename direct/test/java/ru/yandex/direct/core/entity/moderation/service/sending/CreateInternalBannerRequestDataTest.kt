package ru.yandex.direct.core.entity.moderation.service.sending

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.entity.banner.model.BannerWithInternalAdModerationInfo
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo
import ru.yandex.direct.core.entity.banner.model.TemplateVariable
import ru.yandex.direct.core.entity.image.model.BannerImageFormat
import ru.yandex.direct.core.entity.image.service.ImageUtils
import ru.yandex.direct.core.entity.internalads.Constants.BANNER_ON_404_TEMPLATE_ID
import ru.yandex.direct.core.entity.internalads.Constants.DESKTOP_BANNER_ON_MORDA_TEMPLATE_ID
import ru.yandex.direct.core.entity.internalads.Constants.MEDIA_BANNER_TEMPLATE_ID
import ru.yandex.direct.core.entity.internalads.Constants.TEASER_INLINE_BROWSER_TEMPLATE_ID
import ru.yandex.direct.core.entity.internalads.Constants.TEASER_INLINE_TEMPLATE_ID
import ru.yandex.direct.core.entity.internalads.Constants.TEASER_TEMPLATE_ID
import ru.yandex.direct.core.entity.internalads.Constants.TEASER_WITH_TIME_TEMPLATE_ID
import ru.yandex.direct.core.entity.moderation.model.AspectRatio
import ru.yandex.direct.core.entity.moderation.model.InternalBannerRequestData
import ru.yandex.direct.core.entity.moderation.service.sending.internalad.InternalBannerModerationHelper
import ru.yandex.direct.core.testing.data.TestBannerImageFormat.createBannerImageFormat
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy

@RunWith(JUnitParamsRunner::class)
class CreateInternalBannerRequestDataTest {

    companion object {
        val IMAGE_ASPECTS = AspectRatio(120, 240)
        val IMAGE_RETINA_ASPECTS = AspectRatio(240, 480)

        private val IMAGE_FORMAT = createBannerImageFormat(IMAGE_ASPECTS.width, IMAGE_ASPECTS.height)
        private val IMAGE_RETINA_FORMAT = createBannerImageFormat(IMAGE_RETINA_ASPECTS.width, IMAGE_RETINA_ASPECTS.height)

        val IMAGE_HASH = IMAGE_FORMAT.imageHash
        val IMAGE_URL = ImageUtils.generateSecureOrigImageUrl(IMAGE_FORMAT)
        val IMAGE_RETINA_HASH = IMAGE_RETINA_FORMAT.imageHash
        val IMAGE_RETINA_URL = ImageUtils.generateSecureOrigImageUrl(IMAGE_RETINA_FORMAT)

        val IMAGE_FORMAT_BY_HASH: Map<String, BannerImageFormat> = mapOf(
                IMAGE_HASH to IMAGE_FORMAT,
                IMAGE_RETINA_HASH to IMAGE_RETINA_FORMAT
        )

        const val IMAGE_ALT = "some imageAlt"
        const val LINK = "https://redirect.appmetrica.yandex.com/serve/1035324056045369788?click_id={TRACKID}&ios_ifa_sha1&phrase_id={PHRASE_EXPORT_ID}"

        const val TITLE1 = "some title1"
        const val TITLE2 = "some title2"
        const val TEXT1 = "some text1"
        const val TEXT2 = "some text2"
        const val PRIVACY_TEXT = "some privacyText"
        const val LINK_TYPE = "some linkType"

        const val MODERATION_COUNTRIES = Region.RUSSIA_REGION_ID.toString()
    }

    fun testData() = listOf(
            listOf("861 MediaBanner", getModerationInfo(MEDIA_BANNER_TEMPLATE_ID)),
            listOf("516 Тизер с текущим временем (только png, прозрачный)", getModerationInfo(TEASER_WITH_TIME_TEMPLATE_ID)),
            listOf("631 Тизер 2013 (json)", getModerationInfo(TEASER_TEMPLATE_ID)),
            listOf("866 Teaser_inline", getModerationInfo(TEASER_INLINE_TEMPLATE_ID)),
            listOf("1001 Teaser_inline для прямого скачивания Браузера - морда", getModerationInfo(TEASER_INLINE_BROWSER_TEMPLATE_ID)),
            listOf("3340 Десктопный баннер на Морде (Internal)", getModerationInfo(DESKTOP_BANNER_ON_MORDA_TEMPLATE_ID)),
            listOf("3343 Баннер на 404", getModerationInfo(BANNER_ON_404_TEMPLATE_ID)),
    )


    @Test
    @Parameters(method = "testData")
    @TestCaseName("checkCreateInternalBannerRequestData for templateId= {0}")
    fun checkCreateInternalBannerRequestData(description: String,
                                             moderationInfo: BannerWithInternalAdModerationInfo) {
        val requestData = InternalBannerModerationHelper
                .createInternalBannerRequestData(moderationInfo, true, IMAGE_FORMAT_BY_HASH, MODERATION_COUNTRIES)
        val expectedRequestData = getExpectedRequestData(moderationInfo)

        assertThat(requestData)
                .`is`(matchedBy(beanDiffer(expectedRequestData)))
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("checkCreateInternalBannerRequestData without imageFormats and moderationCountries for templateId= {0}")
    fun checkCreateInternalBannerRequestData_WithoutImageFormatsAndModerationCountries(description: String,
                                                                                       moderationInfo: BannerWithInternalAdModerationInfo) {
        val requestData = InternalBannerModerationHelper.createInternalBannerRequestData(moderationInfo, false)
        val expectedRequestData = getExpectedRequestData(moderationInfo, false, "")

        assertThat(requestData)
                .`is`(matchedBy(beanDiffer(expectedRequestData)))
    }


    private fun getModerationInfo(templateId: Long): BannerWithInternalAdModerationInfo {
        return BannerWithInternalAdModerationInfo()
                .withTemplateId(templateId)
                .withDescription("InternalBanner description ${RandomStringUtils.random(7)}")
                .withTemplateVariables(getTemplateVariables(templateId))
                .withModerationInfo(InternalModerationInfo()
                        .withStatusShowAfterModeration(true)
                        .withCustomComment("Custom comment ${RandomStringUtils.random(7)}")
                        .withIsSecretAd(false)
                        .withTicketUrl("https://st.yandex-team.ru/LEGAL-115")
                )
    }

    private fun getTemplateVariables(templateId: Long): List<TemplateVariable> {
        return when (templateId) {
            MEDIA_BANNER_TEMPLATE_ID -> listOf(
                    TemplateVariable()
                            .withTemplateResourceId(3337)
                            .withInternalValue(IMAGE_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(3338)
                            .withInternalValue(IMAGE_ALT),
                    TemplateVariable()
                            .withTemplateResourceId(3339)
                            .withInternalValue(LINK))
            TEASER_WITH_TIME_TEMPLATE_ID -> listOf(
                    TemplateVariable()
                            .withTemplateResourceId(2333)
                            .withInternalValue(LINK),
                    TemplateVariable()
                            .withTemplateResourceId(2334)
                            .withInternalValue(IMAGE_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(2335)
                            .withInternalValue(TEXT1))
            TEASER_TEMPLATE_ID -> listOf(
                    TemplateVariable()
                            .withTemplateResourceId(3278)
                            .withInternalValue(LINK),
                    TemplateVariable()
                            .withTemplateResourceId(3279)
                            .withInternalValue(IMAGE_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(3280)
                            .withInternalValue(TITLE1),
                    TemplateVariable()
                            .withTemplateResourceId(3281)
                            .withInternalValue(TITLE2),
                    TemplateVariable()
                            .withTemplateResourceId(3282)
                            .withInternalValue(TEXT1),
                    TemplateVariable()
                            .withTemplateResourceId(3283)
                            .withInternalValue(TEXT2),
                    TemplateVariable()
                            .withTemplateResourceId(3284)
                            .withInternalValue(IMAGE_ALT),
                    TemplateVariable()
                            .withTemplateResourceId(3741)
                            .withInternalValue(PRIVACY_TEXT),
                    TemplateVariable()
                            .withTemplateResourceId(3755)
                            .withInternalValue(IMAGE_RETINA_HASH))
            TEASER_INLINE_TEMPLATE_ID -> listOf(
                    TemplateVariable()
                            .withTemplateResourceId(3342)
                            .withInternalValue(LINK),
                    TemplateVariable()
                            .withTemplateResourceId(3343)
                            .withInternalValue(IMAGE_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(3344)
                            .withInternalValue(TITLE1),
                    TemplateVariable()
                            .withTemplateResourceId(3345)
                            .withInternalValue(TITLE2),
                    TemplateVariable()
                            .withTemplateResourceId(3346)
                            .withInternalValue(TEXT1),
                    TemplateVariable()
                            .withTemplateResourceId(3347)
                            .withInternalValue(TEXT2),
                    TemplateVariable()
                            .withTemplateResourceId(3348)
                            .withInternalValue(IMAGE_ALT),
                    TemplateVariable()
                            .withTemplateResourceId(3349)
                            .withInternalValue(LINK_TYPE),
                    TemplateVariable()
                            .withTemplateResourceId(3518)
                            .withInternalValue(PRIVACY_TEXT),
                    TemplateVariable()
                            .withTemplateResourceId(3756)
                            .withInternalValue(IMAGE_RETINA_HASH))
            TEASER_INLINE_BROWSER_TEMPLATE_ID -> listOf(
                    TemplateVariable()
                            .withTemplateResourceId(3673)
                            .withInternalValue(LINK),
                    TemplateVariable()
                            .withTemplateResourceId(3674)
                            .withInternalValue(IMAGE_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(3675)
                            .withInternalValue(TITLE1),
                    TemplateVariable()
                            .withTemplateResourceId(3676)
                            .withInternalValue(TITLE2),
                    TemplateVariable()
                            .withTemplateResourceId(3677)
                            .withInternalValue(TEXT1),
                    TemplateVariable()
                            .withTemplateResourceId(3678)
                            .withInternalValue(TEXT2),
                    TemplateVariable()
                            .withTemplateResourceId(3679)
                            .withInternalValue(IMAGE_ALT),
                    TemplateVariable()
                            .withTemplateResourceId(3680)
                            .withInternalValue(LINK_TYPE),
                    TemplateVariable()
                            .withTemplateResourceId(3683)
                            .withInternalValue(PRIVACY_TEXT),
                    TemplateVariable()
                            .withTemplateResourceId(4143)
                            .withInternalValue(IMAGE_RETINA_HASH))
            DESKTOP_BANNER_ON_MORDA_TEMPLATE_ID -> listOf(
                    TemplateVariable()
                            .withTemplateResourceId(5247)
                            .withInternalValue(IMAGE_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(5248)
                            .withInternalValue(IMAGE_ALT),
                    TemplateVariable()
                            .withTemplateResourceId(5249)
                            .withInternalValue(IMAGE_RETINA_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(5250)
                            .withInternalValue(LINK))
            BANNER_ON_404_TEMPLATE_ID -> listOf(
                    TemplateVariable()
                            .withTemplateResourceId(5730)
                            .withInternalValue(IMAGE_ALT),
                    TemplateVariable()
                            .withTemplateResourceId(5731)
                            .withInternalValue(IMAGE_HASH),
                    TemplateVariable()
                            .withTemplateResourceId(5732)
                            .withInternalValue(LINK))
            else -> throw IllegalStateException("unexpected templateId $templateId")
        }
    }

    private fun getExpectedRequestData(moderationInfo: BannerWithInternalAdModerationInfo,
                                       needAddDataForImages: Boolean = true,
                                       moderationCountries: String = MODERATION_COUNTRIES): InternalBannerRequestData {
        val internalModerationInfo = moderationInfo.moderationInfo
        val requestData = InternalBannerRequestData()
                .withTemplateId(moderationInfo.templateId)
                .withGeo(moderationCountries)

                .withObjectName(moderationInfo.description)
                .withTicketUrl(internalModerationInfo.ticketUrl)
                .withAdditionalInformation(internalModerationInfo.customComment)
                .withIsSecretAd(internalModerationInfo.isSecretAd)

                .withImage(IMAGE_HASH)
                .withImageAlt(IMAGE_ALT)
                .withLink(LINK)

        when (moderationInfo.templateId) {
            TEASER_WITH_TIME_TEMPLATE_ID -> requestData
                    .withText1(TEXT1)
                    .withImageAlt(null)
            in listOf(TEASER_TEMPLATE_ID, TEASER_INLINE_TEMPLATE_ID, TEASER_INLINE_BROWSER_TEMPLATE_ID) -> {
                requestData
                        .withTitle1(TITLE1)
                        .withTitle2(TITLE2)
                        .withText1(TEXT1)
                        .withText2(TEXT2)
                        .withPrivacyText(PRIVACY_TEXT)
                        .withImageRetina(IMAGE_RETINA_HASH)

                if (moderationInfo.templateId != TEASER_TEMPLATE_ID) {
                    requestData.withLinkType(LINK_TYPE)
                }
            }
            DESKTOP_BANNER_ON_MORDA_TEMPLATE_ID -> requestData
                    .withImageRetina(IMAGE_RETINA_HASH)
            else -> {
            }
        }

        if (needAddDataForImages) {
            requestData
                    .withImage(IMAGE_URL)
                    .withImageAspects(IMAGE_ASPECTS)

            if (requestData.imageRetina != null) {
                requestData
                        .withImageRetina(IMAGE_RETINA_URL)
                        .withImageRetinaAspects(IMAGE_RETINA_ASPECTS)
            }
        }

        return requestData
    }

}
