package ru.yandex.autotests.reporting.api.beans;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Map;

/**
 * Created by kateleb on 20.03.17.
 */
public class CpaSlide1 extends CpaSlide {
    private String cancelledOrdersCategoryShare;
    private String ordersShare;
    private Integer ordersCount;

    private Map<String, String> ordersShareDiagram;
    private Map<String, String> completedCancelledDiagram;
    private Map<String, String> categoryDynamicDiagram;
    private Map<String, String> ordersShareDynamicDiagram;
    private List<String> periods;
    private List<String> shopRating;
    private String footer;

    public CpaSlide1(ReportingSlide slide) {
        super(slide);
    }

    public CpaSlide1(String region, String category, List<String> unparsedLines, Map<String, Map<String, String>> charts) {
        super(region, category, unparsedLines, charts);
    }

    public String getCancelledOrdersCategoryShare() {
        return cancelledOrdersCategoryShare;
    }

    public void setCancelledOrdersCategoryShare(String cancelledOrdersCategoryShare) {
        this.cancelledOrdersCategoryShare = cancelledOrdersCategoryShare;
    }

    public String getOrdersShare() {
        return ordersShare;
    }

    public void setOrdersShare(String ordersShare) {
        this.ordersShare = ordersShare;
    }

    public Integer getOrdersCount() {
        return ordersCount;
    }

    public void setOrdersCount(Integer ordersCount) {
        this.ordersCount = ordersCount;
    }

    public Map<String, String> getOrdersShareDiagram() {
        return ordersShareDiagram;
    }

    public void setOrdersShareDiagram(Map<String, String> ordersShareDiagram) {
        this.ordersShareDiagram = ordersShareDiagram;
    }

    public Map<String, String> getCompletedCancelledDiagram() {
        return completedCancelledDiagram;
    }

    public void setCompletedCancelledDiagram(Map<String, String> completedCancelledDiagram) {
        this.completedCancelledDiagram = completedCancelledDiagram;
    }

    public Map<String, String> getCategoryDynamicDiagram() {
        return categoryDynamicDiagram;
    }

    public void setCategoryDynamicDiagram(Map<String, String> categoryDynamicDiagram) {
        this.categoryDynamicDiagram = categoryDynamicDiagram;
    }

    public Map<String, String> getOrdersShareDynamicDiagram() {
        return ordersShareDynamicDiagram;
    }

    public void setOrdersShareDynamicDiagram(Map<String, String> ordersShareDynamicDiagram) {
        this.ordersShareDynamicDiagram = ordersShareDynamicDiagram;
    }

    public List<String> getPeriods() {
        return periods;
    }

    public void setPeriods(List<String> periods) {
        this.periods = periods;
    }

    public List<String> getShopRating() {
        return shopRating;
    }

    public void setShopRating(List<String> shopRating) {
        this.shopRating = shopRating;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, false);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    @Override
    protected String getType(String currentRegion, String currentCategory, String firstLine) {
        return CpaSlide1.CPA_SLIDE_1;
    }
}
