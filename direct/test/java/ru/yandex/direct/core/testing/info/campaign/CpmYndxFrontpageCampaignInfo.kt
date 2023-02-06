package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns.fullCpmYndxFrontpageCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class CpmYndxFrontpageCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: CpmYndxFrontpageCampaign = fullCpmYndxFrontpageCampaign(),
) : CampaignInfo<CpmYndxFrontpageCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)

