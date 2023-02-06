package ru.yandex.autotests.reporting.api.beans;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.*;

/**
 * Created by kateleb on 20.03.17.
 */
public class CpcSlide2 extends CpcSlide {

    private String targetShop;
    private Map<String, String> competitionMapLeftDiagram;
    private Map<String, String> competitionMapRightDiagram;

    private List<String> periods;
    private String footer;

    public CpcSlide2(ReportingSlide slide) {
        super(slide);
    }

    public CpcSlide2(String region, String category, List<String> strings, HashMap<String, Map<String,String>> stringMapHashMap) {
        super(region, category, strings, stringMapHashMap);
    }

    public String getTargetShop() {
        return targetShop;
    }

    public void setTargetShop(String targetShop) {
        this.targetShop = targetShop;
    }


    public List<String> getPeriods() {
        return periods;
    }

    public void setPeriods(List<String> periods) {
        this.periods = periods;
    }

    public Map<String, String> getCompetitionMapLeftDiagram() {
        return competitionMapLeftDiagram;
    }

    public void setCompetitionMapLeftDiagram(Map<String, String> competitionMapLeftDiagram) {
        this.competitionMapLeftDiagram = competitionMapLeftDiagram;
    }

    public Map<String, String> getCompetitionMapRightDiagram() {
        return competitionMapRightDiagram;
    }

    public void setCompetitionMapRightDiagram(Map<String, String> competitionMapRightDiagram) {
        this.competitionMapRightDiagram = competitionMapRightDiagram;
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
        return CPC_SLIDE_2;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }
}
