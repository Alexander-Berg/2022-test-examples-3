package ru.yandex.autotests.reporting.api.beans;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kateleb on 20.03.17.
 */
public class CpcSlide1 extends CpcSlide {

    private String clicksShare;
    private Integer clicksCount;
    private Map<String, String> clicksShareDiagram;
    private Map<String, String> mobileDesktopDiagram;
    private Map<String, String> categoryDynamicDiagram;
    private Map<String, String> clicksShareDynamicsDiagram;
    private List<String> periods;
    private List<String> shopRating;
    private String footer;

    public CpcSlide1(ReportingSlide slide) {
        super(slide);
    }

    public CpcSlide1(String region, String category, List<String> strings, HashMap<String, Map<String,String>> stringMapHashMap) {
        super(region, category, strings, stringMapHashMap);
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
        return CPC_SLIDE_1;
    }

    public String getClicksShare() {
        return clicksShare;
    }

    public void setClicksShare(String clicksShare) {
        this.clicksShare = clicksShare;
    }

    public Integer getClicksCount() {
        return clicksCount;
    }

    public void setClicksCount(Integer clicksCount) {
        this.clicksCount = clicksCount;
    }

    public Map<String, String> getClicksShareDiagram() {
        return clicksShareDiagram;
    }

    public void setClicksShareDiagram(Map<String, String> clicksShareDiagram) {
        this.clicksShareDiagram = clicksShareDiagram;
    }

    public Map<String, String> getMobileDesktopDiagram() {
        return mobileDesktopDiagram;
    }

    public void setMobileDesktopDiagram(Map<String, String> mobileDesktopDiagram) {
        this.mobileDesktopDiagram = mobileDesktopDiagram;
    }

    public Map<String, String> getCategoryDynamicDiagram() {
        return categoryDynamicDiagram;
    }

    public void setCategoryDynamicDiagram(Map<String, String> categoryDynamicDiagram) {
        this.categoryDynamicDiagram = categoryDynamicDiagram;
    }

    public Map<String, String> getClicksShareDynamicsDiagram() {
        return clicksShareDynamicsDiagram;
    }

    public void setClicksShareDynamicsDiagram(Map<String, String> clicksShareDynamicsDiagram) {
        this.clicksShareDynamicsDiagram = clicksShareDynamicsDiagram;
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
}
