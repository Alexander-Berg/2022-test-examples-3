package ru.yandex.autotests.reporting.api.beans;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by kateleb on 13.03.17.
 */
public class CpcSlide extends ReportingSlide {

    public static final String CPC_SLIDE_1 = "Cpc-слайд 1: Динамика";
    public static final String CPC_SLIDE_2 = "Карта конкуренции";

    public CpcSlide(String region, String category, List<String> unparsedLines, Map<String, Map<String, String>> charts) {
        super(region, category, unparsedLines, charts);
    }

    public CpcSlide(ReportingSlide slide) {
        super(slide);
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
        if (firstLine.contains(CpcSlide.DYNAMICS)) {
            return CpcSlide.CPC_SLIDE_1;
        }
        return Stream.of(CpcSlide.METHODS, CpcSlide.SUMMARY, CpcSlide.CPC_SLIDE_2)
                .filter(firstLine::contains).findFirst()
                .orElse(firstLine.replace(currentRegion, "").replace(currentCategory, ""));
    }
}
