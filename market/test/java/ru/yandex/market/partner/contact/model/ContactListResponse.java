package ru.yandex.market.partner.contact.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@XmlRootElement(name = "data")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContactListResponse {

    @XmlElement(name = "paged-data")
    private PagedDataDTO pagedData;

    public PagedDataDTO getPagedData() {
        return pagedData;
    }
}
