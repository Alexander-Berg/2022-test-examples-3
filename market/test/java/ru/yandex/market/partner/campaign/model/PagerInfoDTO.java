package ru.yandex.market.partner.campaign.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "pager-info")
@XmlAccessorType(XmlAccessType.FIELD)
public class PagerInfoDTO {
    @XmlElement(name = "current-page")
    private int currentPage;

    @XmlElement(name = "total-count")
    private int totalCount;

    @XmlElement(name = "perpage-number")
    private int perpageNumber;

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPerpageNumber() {
        return perpageNumber;
    }

    public void setPerpageNumber(int perpageNumber) {
        this.perpageNumber = perpageNumber;
    }
}
