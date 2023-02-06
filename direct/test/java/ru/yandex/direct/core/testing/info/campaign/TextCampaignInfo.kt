package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

class TextCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: TextCampaign = fullTextCampaign()
) : CampaignInfo<TextCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
