package ru.yandex.market.partner.contact.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ContactDTO {
    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "user-id")
    private Long userId;

    @XmlElement(name = "super-admin")
    private boolean superAdmin;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }
}
