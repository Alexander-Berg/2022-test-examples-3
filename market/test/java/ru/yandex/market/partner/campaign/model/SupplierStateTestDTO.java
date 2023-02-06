package ru.yandex.market.partner.campaign.model;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
@XmlRootElement(name = "supplier-info")
@XmlAccessorType(XmlAccessType.NONE)
public class SupplierStateTestDTO {

    @XmlElement(name = "campaign-id")
    private Long campaignId;

    @XmlElement(name = "domain")
    private String domain;

    @XmlElement(name = "name")
    private String name;

    public SupplierStateTestDTO() {
    }

    public SupplierStateTestDTO(long campaignId, String domain, String name) {
        this.campaignId = campaignId;
        this.domain = domain;
        this.name = name;
    }

    @Nonnull
    @Override
    public String toString() {
        return "SupplierStateDTO{" +
                "campaignId=" + campaignId +
                ", domain='" + domain + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SupplierStateTestDTO that = (SupplierStateTestDTO) o;
        return Objects.equals(campaignId, that.campaignId) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(campaignId, domain, name);
    }

    @Nullable
    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    @Nullable
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
