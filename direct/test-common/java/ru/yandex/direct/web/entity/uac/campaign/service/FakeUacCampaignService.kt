package ru.yandex.direct.web.entity.uac.campaign.service

import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.IUacCampaignService

class FakeUacCampaignService(private val data: Map<String, UacYdbCampaign>) : IUacCampaignService {
    override fun getCampaignById(id: String): UacYdbCampaign? = data[id]
}
