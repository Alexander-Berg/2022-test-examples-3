package ru.yandex.market.partner.contact.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DataDTO {
    @XmlElement(name = "contact")
    private List<FullContactDTO> contacts;

    public List<FullContactDTO> getContacts() {
        return contacts;
    }
}
