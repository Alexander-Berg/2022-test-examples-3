package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns.fullCpmBannerCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class CpmBannerCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: CpmBannerCampaign = fullCpmBannerCampaign()
) : CampaignInfo<CpmBannerCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
