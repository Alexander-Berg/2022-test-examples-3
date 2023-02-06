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
public class FullContactDTO {

    @XmlElement(name = "super-admin")
    private boolean superAdmin;

    @XmlElement(name = "contact")
    private ContactDTO contact;

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public ContactDTO getContact() {
        return contact;
    }
}
