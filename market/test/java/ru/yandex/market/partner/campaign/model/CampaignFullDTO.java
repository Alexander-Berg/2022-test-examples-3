package ru.yandex.market.partner.campaign.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;

@XmlRootElement(name = "campaign")
@XmlAccessorType(XmlAccessType.FIELD)
public class CampaignFullDTO {

    @XmlElement(name = "campaign-info")
    private CampaignInfoDTO campaignInfoDTO;

    @XmlElement(name = "datasource-info")
    private DatasourceInfoDTO datasourceInfoDTO;

    @XmlElement(name = "can-limit-budget")
    private boolean canLimitBudget;

    @XmlElement(name = "param-value")
    private Set<ParamValueDTO> paramValue;

    @XmlElement(name = "partner-placement-program-type")
    @XmlElementWrapper(name = "placement-types")
    private List<PartnerPlacementProgramType> placementTypes;

    public List<PartnerPlacementProgramType> getPlacementTypes() {
        return placementTypes;
    }

    public void setPlacementTypes(List<PartnerPlacementProgramType> placementTypes) {
        this.placementTypes = placementTypes;
    }

    public CampaignFullDTO() {
    }

    public CampaignFullDTO(CampaignInfoDTO campaignInfoDTO, DatasourceInfoDTO datasourceInfoDTO,
                           boolean canLimitBudget, Set<ParamValueDTO> paramValue) {
        this.campaignInfoDTO = campaignInfoDTO;
        this.datasourceInfoDTO = datasourceInfoDTO;
        this.canLimitBudget = canLimitBudget;
        this.paramValue = paramValue;
    }

    public CampaignInfoDTO getCampaignInfoDTO() {
        return campaignInfoDTO;
    }

    public void setCampaignInfoDTO(CampaignInfoDTO campaignInfoDTO) {
        this.campaignInfoDTO = campaignInfoDTO;
    }

    public DatasourceInfoDTO getDatasourceInfoDTO() {
        return datasourceInfoDTO;
    }

    public void setDatasourceInfoDTO(DatasourceInfoDTO datasourceInfoDTO) {
        this.datasourceInfoDTO = datasourceInfoDTO;
    }

    public boolean isCanLimitBudget() {
        return canLimitBudget;
    }

    public void setCanLimitBudget(boolean canLimitBudget) {
        this.canLimitBudget = canLimitBudget;
    }

    public Set<ParamValueDTO> getParamValue() {
        return paramValue;
    }

    public void setParamValue(Set<ParamValueDTO> paramValue) {
        this.paramValue = paramValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CampaignFullDTO that = (CampaignFullDTO) o;
        return canLimitBudget == that.canLimitBudget && Objects.equals(campaignInfoDTO, that.campaignInfoDTO) && Objects.equals(datasourceInfoDTO, that.datasourceInfoDTO) && Objects.equals(paramValue, that.paramValue) && Objects.equals(placementTypes, that.placementTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(campaignInfoDTO, datasourceInfoDTO, canLimitBudget, paramValue, placementTypes);
    }

    @Override
    public String toString() {
        return "CampaignFullDTO{" +
                "campaignInfoDTO=" + campaignInfoDTO +
                ", datasourceInfoDTO=" + datasourceInfoDTO +
                ", canLimitBudget=" + canLimitBudget +
                ", paramValue=" + paramValue +
                ", placementTypes=" + placementTypes +
                '}';
    }
}
