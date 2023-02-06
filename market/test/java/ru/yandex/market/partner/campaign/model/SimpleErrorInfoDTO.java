package ru.yandex.market.partner.campaign.model;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;

/**
 * @author fbokovikov
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "simple-error-info")
public class SimpleErrorInfoDTO {

    @XmlElement(name = "message-code")
    private String messageCode;

    public SimpleErrorInfoDTO(String messageCode) {
        this.messageCode = messageCode;
    }

    public SimpleErrorInfoDTO() {
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("messageCode", messageCode)
                .toString();
    }
}
