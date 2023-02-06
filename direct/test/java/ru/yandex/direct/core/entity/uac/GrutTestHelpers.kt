package ru.yandex.direct.core.entity.uac

import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.grut.api.BriefAdGroup
import ru.yandex.direct.core.grut.api.BriefBanner
import ru.yandex.grut.objects.proto.AdGroup
import ru.yandex.grut.objects.proto.Banner
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.client.Schema

object GrutTestHelpers {

    fun buildCreateCampaignRequest(
        clientId: Long,
        campaignId: Long,
        campaignType: Campaign.ECampaignTypeOld = Campaign.ECampaignTypeOld.CTO_TEXT,
        campaignSpec: Campaign.TCampaignSpec? = null
    ): Schema.TCampaign {
        return Schema.TCampaign.newBuilder()
            .apply {
                meta = Schema.TCampaignMeta.newBuilder().apply {
                    id = campaignId
                    this.clientId = clientId
                    this.campaignType = campaignType
                }.build()
                if (campaignSpec != null) {
                    spec = campaignSpec
                }
            }
            .build()
    }

    fun buildCreateAdGroupRequest(
        campaignId: Long,
        adGroupId: Long,
        directAdGroupStatus: AdGroup.TAdGroupSpec.EDirectAdGroupStatus = AdGroup.TAdGroupSpec.EDirectAdGroupStatus.DAGS_CREATED
    ): BriefAdGroup {
        return BriefAdGroup(
            id = adGroupId,
            briefId = campaignId,
            status = directAdGroupStatus
        )
    }

    fun buildCreateBannerRequest(
        adGroupId: Long,
        campaignId: Long,
        bannerId: Long,
        status: Banner.TBannerSpec.EBannerStatus = Banner.TBannerSpec.EBannerStatus.BSS_CREATED,
        assetIds: Collection<String> = emptyList(),
        assetLinkIds: Collection<String> = emptyList(),
    ): BriefBanner {
        return BriefBanner(
            id = bannerId,
            adGroupId = adGroupId,
            briefId = campaignId,
            source = Banner.EBannerSource.BS_DIRECT,
            status = status,
            assetIds = assetIds.map { it.toIdLong() },
            assetLinksIds = assetLinkIds.map { it.toIdLong() },
        )
    }
}
