package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign
import ru.yandex.direct.core.testing.data.campaign.TestInternalDistribCampaigns.fullInternalDistribCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class InternalDistribCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: InternalDistribCampaign = fullInternalDistribCampaign()
) : CampaignInfo<InternalDistribCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
