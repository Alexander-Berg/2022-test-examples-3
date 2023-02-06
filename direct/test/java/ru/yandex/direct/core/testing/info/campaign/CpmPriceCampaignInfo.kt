package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCpmPriceCampaigns.fullCpmPriceCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.PricePackageInfo
import ru.yandex.direct.core.testing.info.UserInfo

class CpmPriceCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: CpmPriceCampaign = fullCpmPriceCampaign(null),
    val pricePackageInfo: PricePackageInfo = PricePackageInfo(),
) : CampaignInfo<CpmPriceCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign)
