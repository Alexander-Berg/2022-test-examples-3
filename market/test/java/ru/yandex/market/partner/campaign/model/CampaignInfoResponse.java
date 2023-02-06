package ru.yandex.market.partner.campaign.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;

/**
 * @author fbokovikov
 */
@XmlRootElement(name = "data")
@XmlAccessorType(XmlAccessType.NONE)
public class CampaignInfoResponse {

    @XmlElement(name = "campaign-info")
    private CampaignInfoDTO campaignInfoDTO;

    public CampaignInfoResponse() {
    }

    public CampaignInfoResponse(CampaignInfoDTO campaignInfoDTO) {
        this.campaignInfoDTO = campaignInfoDTO;
    }

    public CampaignInfoDTO getCampaignInfoDTO() {
        return campaignInfoDTO;
    }

    public void setCampaignInfoDTO(CampaignInfoDTO campaignInfoDTO) {
        this.campaignInfoDTO = campaignInfoDTO;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("campaignInfoDTO", campaignInfoDTO)
                .toString();
    }
}
