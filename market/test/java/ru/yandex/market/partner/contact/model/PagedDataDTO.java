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
public class PagedDataDTO {

    @XmlElement(name = "data")
    private DataDTO data;

    public DataDTO getData() {
        return data;
    }
}
