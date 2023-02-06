package ru.yandex.direct.core.entity.campaign.service.type

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBannerHrefParams
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.model.ModelChanges

/**
 * Проверяем, что ссылка при сохранении меняется.
 * Логика изменения из [BannerHrefParamsUtils.sanitizeHrefParams] проверяется в [BannerHrefParamsUtilsTest]
 */
@CoreTest
@ExtendWith(SpringExtension::class)
class CampaignWithBannerHrefParamsSanitizeBannerHrefTest {

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var campaignOperationService: CampaignOperationService

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Test
    internal fun `hrefParams are sanitized on text campaign adding`() {
        val clientInfo = steps.clientSteps().createDefaultClient()

        val ucCampaign = TestCampaigns.defaultTextCampaign()
            .withBannerHrefParams("?&&utm_source=direct&&utm_campaign=my_camp&&")

        val result = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(ucCampaign),
            clientInfo.uid, UidAndClientId.of(clientInfo.uid, clientInfo.clientId!!), CampaignOptions()
        ).prepareAndApply()

        assertThat(result.successfulCount)
            .`as`("campaign added successfully")
            .isEqualTo(1)

        val campaignId = result.result.first().result
        val campaign = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, listOf(campaignId)).first()
        assertThat((campaign as TextCampaign).bannerHrefParams)
            .describedAs("bannerHrefParams saved")
            .isEqualTo("utm_source=direct&&utm_campaign=my_camp")
    }

    @Test
    internal fun `hrefParams are sanitized on text campaign updating`() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaign()
        val clientInfo = campaignInfo.clientInfo
        val campaignId = campaignInfo.campaignId

        val element: ModelChanges<TextCampaign>? =
            ModelChanges.build(
                campaignId, TextCampaign::class.java,
                CampaignWithBannerHrefParams.BANNER_HREF_PARAMS,
                "?&&utm_source=direct&&utm_campaign=my_camp&&"
            )
        val result2 = campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(element),
            clientInfo.uid, UidAndClientId.of(clientInfo.uid, clientInfo.clientId!!), CampaignOptions()
        ).apply()

        assertThat(result2.successfulCount)
            .`as`("campaign updated successfully")
            .isEqualTo(1)

        val campaign = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, listOf(campaignId)).first()
        assertThat((campaign as TextCampaign).bannerHrefParams)
            .describedAs("bannerHrefParams saved")
            .isEqualTo("utm_source=direct&&utm_campaign=my_camp")
    }
}
