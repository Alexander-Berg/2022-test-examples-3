package ru.yandex.market.partner.campaign.model;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;

import ru.yandex.market.core.campaign.model.CampaignType;

/**
 * @author fbokovikov
 */
@XmlRootElement(name = "campaign-info")
@XmlAccessorType(XmlAccessType.NONE)
public class CampaignInfoDTO {
    @XmlElement(name = "type")
    private CampaignType campaignType;

    @XmlElement(name = "client-id")
    private Long clientId;

    @XmlElement(name = "datasource-id", nillable = true)
    private Long datasourceId;

    @XmlElement(name = "tariff-id", nillable = true)
    private Long tariffId;

    @XmlElement(name = "id")
    private Long campaignId;

    public CampaignInfoDTO() {
    }

    public CampaignInfoDTO(CampaignType campaignType, Long clientId, Long datasourceId, Long tariffId, Long campaignId) {
        this.campaignType = campaignType;
        this.clientId = clientId;
        this.datasourceId = datasourceId;
        this.tariffId = tariffId;
        this.campaignId = campaignId;
    }

    public CampaignType getCampaignType() {
        return campaignType;
    }

    public void setCampaignType(CampaignType campaignType) {
        this.campaignType = campaignType;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Long datasourceId) {
        this.datasourceId = datasourceId;
    }

    public Long getTariffId() {
        return tariffId;
    }

    public void setTariffId(Long tariffId) {
        this.tariffId = tariffId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CampaignInfoDTO that = (CampaignInfoDTO) o;
        return campaignType == that.campaignType &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(datasourceId, that.datasourceId) &&
                Objects.equals(tariffId, that.tariffId) &&
                Objects.equals(campaignId, that.campaignId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(campaignType, clientId, datasourceId, tariffId, campaignId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("campaignType", campaignType)
                .add("clientId", clientId)
                .add("datasourceId", datasourceId)
                .add("tariffId", tariffId)
                .add("campaignId", campaignId)
                .toString();
    }
}
