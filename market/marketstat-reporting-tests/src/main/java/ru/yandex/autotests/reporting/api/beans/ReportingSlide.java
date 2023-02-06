package ru.yandex.autotests.reporting.api.beans;

import ru.yandex.autotests.market.common.differ.WithId;

import java.util.List;
import java.util.Map;

/**
 * Created by kateleb on 16.03.17.
 */
public abstract class ReportingSlide implements WithId {

    public static final String DYNAMICS = "Динамика";
    public static final String METHODS = "Методология";
    public static final String SUMMARY = "Выводы";

    protected String region;
    protected String category;
    protected String type;
    protected List<String> lines;
    protected Map<String, Map<String, String>> charts;

    public ReportingSlide(String region, String category, List<String> unparsedLines, Map<String, Map<String, String>> charts) {
        this.region = region;
        this.category = category;
        this.type = getType(region, category, unparsedLines.remove(0));
        this.lines = unparsedLines;
        this.charts = charts;
    }

    public ReportingSlide(ReportingSlide slide) {
        this(slide.getRegion(), slide.getCategory(), slide.getLines(), slide.getCharts());
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    protected abstract String getType(String currentRegion, String currentCategory, String firstLine);

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public Map<String, Map<String, String>> getCharts() {
        return charts;
    }

    public void setCharts(Map<String, Map<String, String>> charts) {
        this.charts = charts;
    }

    @Override
    public String id() {
        return getType();
    }
}