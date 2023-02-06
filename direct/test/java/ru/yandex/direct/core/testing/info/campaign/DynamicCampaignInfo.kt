package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns.fullDynamicCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class DynamicCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: DynamicCampaign = fullDynamicCampaign()
) : CampaignInfo<DynamicCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
