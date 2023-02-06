package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign
import ru.yandex.direct.core.testing.data.campaign.TestMcBannerCampaigns.fullMcBannerCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class McBannerCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: McBannerCampaign = fullMcBannerCampaign()
) : CampaignInfo<McBannerCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
