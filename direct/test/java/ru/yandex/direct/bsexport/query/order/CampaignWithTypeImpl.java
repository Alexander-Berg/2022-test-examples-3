package ru.yandex.direct.bsexport.query.order;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCampaignType;

class CampaignWithTypeImpl implements CampaignWithCampaignType {
    private Long id;
    private CampaignType campaignType;

    @Override
    public CampaignType getType() {
        return campaignType;
    }

    @Override
    public void setType(CampaignType type) {
        campaignType = type;
    }

    @Override
    public CampaignWithCampaignType withType(CampaignType type) {
        setType(type);
        return this;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public CampaignWithCampaignType withId(Long id) {
        setId(id);
        return this;
    }
}
