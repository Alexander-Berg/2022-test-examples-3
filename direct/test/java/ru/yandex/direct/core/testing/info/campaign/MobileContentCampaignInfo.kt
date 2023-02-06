package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns.fullMobileContentCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.MobileAppInfo
import ru.yandex.direct.core.testing.info.UserInfo

class MobileContentCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: MobileContentCampaign = fullMobileContentCampaign(null),
    var mobileAppInfo: MobileAppInfo = MobileAppInfo()
) : CampaignInfo<MobileContentCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign) {

    fun withMobileAppInfo(mobileAppInfo: MobileAppInfo) = apply { this.mobileAppInfo = mobileAppInfo }
}
