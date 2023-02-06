package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns.fullSmartCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class SmartCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: SmartCampaign = fullSmartCampaign()
) : CampaignInfo<SmartCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)

