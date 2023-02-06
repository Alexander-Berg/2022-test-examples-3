package ru.yandex.market.partner.campaign.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
@XmlAccessorType(XmlAccessType.FIELD)
public class CampaignFullInfoDTO {
    @XmlElement(name = "pager-info")
    private PagerInfoDTO pagerInfo;
    @XmlElement(name = "campaign")
    private CampaignFullDTO campaign;
    @XmlElement(name = "isSearch")
    private boolean isSearch;


    public PagerInfoDTO getPagerInfo() {
        return pagerInfo;
    }

    public void setPagerInfo(PagerInfoDTO pagerInfo) {
        this.pagerInfo = pagerInfo;
    }

    public boolean isSearch() {
        return isSearch;
    }

    public void setSearch(boolean search) {
        isSearch = search;
    }

    public CampaignFullDTO getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignFullDTO campaign) {
        this.campaign = campaign;
    }
}
