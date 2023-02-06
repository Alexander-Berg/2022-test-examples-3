package ru.yandex.market.partner.campaign.model;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;

/**
 * @author fbokovikov
 */
@XmlRootElement(name = "datasource-info")
@XmlAccessorType(XmlAccessType.FIELD)
public class DatasourceInfoDTO {

    @XmlElement(name = "internal-name")
    private String internalName;

    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "manager-id")
    private Long managerId;

    public DatasourceInfoDTO(String internalName, Long id, Long managerId) {
        this.internalName = internalName;
        this.id = id;
        this.managerId = managerId;
    }

    public DatasourceInfoDTO() {
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatasourceInfoDTO that = (DatasourceInfoDTO) o;
        return Objects.equals(internalName, that.internalName) &&
                Objects.equals(id, that.id) &&
                Objects.equals(managerId, that.managerId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(internalName, id, managerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("internalName", internalName)
                .add("id", id)
                .add("managerId", managerId)
                .toString();
    }
}
