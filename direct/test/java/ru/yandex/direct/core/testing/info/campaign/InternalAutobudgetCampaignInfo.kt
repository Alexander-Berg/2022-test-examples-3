package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.testing.data.campaign.TestInternalAutobudgetCampaigns.fullInternalAutobudgetCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class InternalAutobudgetCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: InternalAutobudgetCampaign = fullInternalAutobudgetCampaign()
) : CampaignInfo<InternalAutobudgetCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
