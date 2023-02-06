package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign
import ru.yandex.direct.core.testing.data.campaign.TestInternalFreeCampaigns.fullInternalFreeCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class InternalFreeCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: InternalFreeCampaign = fullInternalFreeCampaign()
) : CampaignInfo<InternalFreeCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
