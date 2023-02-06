package ru.yandex.direct.web.entity.uac.campaign.service

import ru.yandex.direct.core.entity.uac.repository.DirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign

class FakeDirectCampaignRepository(private val data: Map<Long, UacYdbDirectCampaign>) : DirectCampaignRepository {
    override fun getDirectCampaignByDirectCampaignId(directCampaignId: Long): UacYdbDirectCampaign? = data[directCampaignId]
}
