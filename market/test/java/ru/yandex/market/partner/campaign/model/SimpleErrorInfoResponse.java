package ru.yandex.market.partner.campaign.model;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;

/**
 * @author fbokovikov
 */
@XmlRootElement(name = "data")
@XmlAccessorType(XmlAccessType.FIELD)
public class SimpleErrorInfoResponse {

    @XmlElementWrapper(name = "errors")
    @XmlElement(name = "simple-error-info")
    private Collection<SimpleErrorInfoDTO> errors;

    public SimpleErrorInfoResponse(Collection<SimpleErrorInfoDTO> errors) {
        this.errors = errors;
    }

    public SimpleErrorInfoResponse() {
    }

    public Collection<SimpleErrorInfoDTO> getErrors() {
        return errors;
    }

    public void setErrors(Collection<SimpleErrorInfoDTO> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("errors", errors)
                .toString();
    }
}
