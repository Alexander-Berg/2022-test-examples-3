package ru.yandex.autotests.reporting.api.steps;

import com.hazelcast.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBubbleChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBubbleSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDoughnutChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumVal;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrVal;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.stat.util.FileReaderUtils;
import ru.yandex.autotests.reporting.api.beans.CpaSlide;
import ru.yandex.autotests.reporting.api.beans.CpaSlide1;
import ru.yandex.autotests.reporting.api.beans.CpcSlide;
import ru.yandex.autotests.reporting.api.beans.CpcSlide1;
import ru.yandex.autotests.reporting.api.beans.CpcSlide2;
import ru.yandex.autotests.reporting.api.beans.ReportFileInfo;
import ru.yandex.autotests.reporting.api.beans.ReportingSlide;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * Created by kateleb on 06.03.17.
 */
public class ReportingApiPptxSteps {

    private SlideAnalyzerSteps slideReader = new SlideAnalyzerSteps();

    public static final String SUMMARY = "Выводы";
    public static final String CPC = "CPC";
    public static final String CPA = "CPA";
    public static final String PPTX = "pptx";

    @Step
    public <T extends ReportingSlide> List<T> parseSlides(ReportingApiParams testType, List<ReportFileInfo> files, Collection<String> categories, Collection<String> regions) {
        List<T> parsedSlides = new ArrayList<>();
        if (testType.buildCpc()) parsedSlides.addAll((List<T>) readCpcSlides(files, regions, categories));
        if (testType.buildCpa()) parsedSlides.addAll((List<T>) readCpaSlides(files, regions, categories));
        return parsedSlides;
    }

    @Step
    public <T extends ReportingSlide> void checkCpcSlides(List<ReportingSlide> slides, List<CpcSlide> clickHouseData,
                                                          Map<String, String> categories, Map<String, String> regions) {
        for (String category : categories.values()) {
            for (String region : regions.values()) {
                List<T> regCategorySlides = getSlidesForRegionAndCategory((List<T>) slides, category, region);
                List<T> clickHouseSlides = getSlidesForRegionAndCategory((List<T>) clickHouseData, category, region);
                checkCpc1Slide(category, region, regCategorySlides, clickHouseSlides);
                checkCpc2Slide(category, region, regCategorySlides, clickHouseSlides);
            }
        }
    }

    @Step
    public <T extends ReportingSlide> void checkCpaSlides(List<ReportingSlide> slides, List<CpaSlide> clickHouseData,
                                                          Map<String, String> categories, Map<String, String> regions) {
        for (String category : categories.values()) {
            for (String region : regions.values()) {
                List<T> regCategorySlides = getSlidesForRegionAndCategory((List<T>) slides, category, region);
                List<T> clickHouseSlides = getSlidesForRegionAndCategory((List<T>) clickHouseData, category, region);
                checkCpa1Slide(category, region, regCategorySlides, clickHouseSlides);
            }
        }
    }

    @Step
    private <T extends ReportingSlide> void checkCpc2Slide(String category, String region, List<T> regCategorySlides, List<T> clickHouseSlides) {
        CpcSlide2 chSlide = (CpcSlide2) getSlideFor(clickHouseSlides, CpcSlide.CPC_SLIDE_2);
        CpcSlide2 slide = slideReader.readCpc2Slide(getSlideFor(regCategorySlides, CpcSlide.CPC_SLIDE_2));
        checkFooter(category, region, slide);
        compareSlideValues(slide, chSlide);

    }

    @Step
    private <T extends ReportingSlide> void checkCpc1Slide(String category, String region, List<T> regCategorySlides, List<T> clickHouseSlides) {
        CpcSlide1 chSlide = (CpcSlide1) getSlideFor(clickHouseSlides, CpcSlide.CPC_SLIDE_1);
        CpcSlide1 slide = slideReader.readCpc1Slide(getSlideFor(regCategorySlides, CpcSlide.CPC_SLIDE_1));
        checkFooter(category, region, slide);
        compareSlideValues(slide, chSlide);
    }

    @Step
    private <T extends ReportingSlide> void compareSlideValues(T slide, T chSlide) {
        Attacher.attachExpected(chSlide);
        Attacher.attachActual(slide);
        //  assertThat("Values mismatch!", Arrays.asList(slide), hasNoDifferenceFrom(Arrays.asList(chSlide), "lines", "charts", "footer"));
    }

    @Step
    private <T extends ReportingSlide> void checkCpa1Slide(String category, String region, List<T> regCategorySlides, List<T> clickHouseSlides) {
        CpaSlide1 chSlide = (CpaSlide1) getSlideFor(clickHouseSlides, CpaSlide.CPA_SLIDE_1);
        CpaSlide1 slide = slideReader.readCpaSlide(getSlideFor(regCategorySlides, CpaSlide.CPA_SLIDE_1));
        checkFooter(category, region, slide);
        compareSlideValues(slide, chSlide);
    }

    private <T extends ReportingSlide> void checkFooter(String category, String region, T slide) {
        Attacher.attach(String.format("slide for %s, %s", category, region), slide);
        if (slide instanceof CpcSlide1) {
            assertThat("Wrong category on slide!", ((CpcSlide1) slide).getFooter(), containsString(category));
            assertThat("Wrong region on slide!", ((CpcSlide1) slide).getFooter(), containsString(region));
        } else if (slide instanceof CpcSlide2) {
            assertThat("Wrong category on slide!", ((CpcSlide2) slide).getFooter(), containsString(category));
            assertThat("Wrong region on slide!", ((CpcSlide2) slide).getFooter(), containsString(region));
        } else if (slide instanceof CpaSlide1) {
            assertThat("Wrong category on slide!", ((CpaSlide1) slide).getFooter(), containsString(category));
            assertThat("Wrong region on slide!", ((CpaSlide1) slide).getFooter(), containsString(region));
        }
    }

    private <T extends ReportingSlide> T getSlideFor(List<T> slides, String type) {
        return slides.stream().filter(s -> s.getType().equals(type)).findAny()
                .orElseThrow(() -> new AssertionError(String.format("No %s slides found", type)));
    }

    private <T extends ReportingSlide> List<T> getSlidesForRegionAndCategory(List<T> slides, String category, String region) {
        List<T> targetSlides = slides.stream().filter(s -> s.getCategory().equals(category) && s.getRegion().equals(region)).collect(toList());
        assertThat(String.format("No slides for region %s and category %s found!", region, category), targetSlides.size(), greaterThan(0));
        return targetSlides;
    }

    @Step
    private List<CpaSlide> readCpaSlides(List<ReportFileInfo> files, Collection<String> regions, Collection<String> categories) {
        XMLSlideShow xmlSlideShow = readPresentation(getTargetFile(files, CPA).getUrl());
        return readSlides(xmlSlideShow, regions, categories, true);
    }

    @Step
    private List<CpcSlide> readCpcSlides(List<ReportFileInfo> files, Collection<String> regions, Collection<String> categories) {
        XMLSlideShow xmlSlideShow = readPresentation(getTargetFile(files, CPC).getUrl());
        return readSlides(xmlSlideShow, regions, categories, false);
    }

    @Step
    private ReportFileInfo getTargetFile(List<ReportFileInfo> files, String marker) {
        ReportFileInfo fileForCase = files.stream().filter(f -> f.getName().contains(marker) && f.getName().endsWith("." + PPTX)).findAny().orElseThrow(() -> new AssertionError("No " + PPTX + " file found for " + marker + "!"));
        Attacher.attach(PPTX + " file for " + marker, fileForCase);
        return fileForCase;
    }

    @Step
    private XMLSlideShow readPresentation(String filepath) {
        HttpURLConnection httpURLConnection = FileReaderUtils.getHttpURLConnection(filepath);
        InputStream inputStream;
        try {
            inputStream = httpURLConnection.getInputStream();
            return new XMLSlideShow(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read pptx file!", e);
        }
    }

    @Step
    private static <T extends ReportingSlide> List<T> readSlides(XMLSlideShow ppt, Collection<String> regions, Collection<String> categories, boolean isCpa) {
        List<T> slides = new ArrayList<>();
        String currentRegion = "";
        String currentCategory;
        int slideCounter = 1;
        for (XSLFSlide slide : ppt.getSlides()) {
            Attacher.attachAction("Starting slide..." + slideCounter);
            Map<String, Map<String, String>> charts = readChartsFromSlide(slide);
            List<String> slideTextLines = readTextFromSlide(slide);
            String slideTitle = slideTextLines.isEmpty() ? "" : slideTextLines.get(0);
            currentRegion = getTargetValue(regions, currentRegion, slideTitle);
            currentCategory = getTargetValue(categories, "", slideTitle);
            slides.add(isCpa ? (T) new CpaSlide(currentRegion, currentCategory, slideTextLines, charts) :
                    (T) new CpcSlide(currentRegion, currentCategory, slideTextLines, charts));
            slideCounter++;
        }
        return slides;
    }

    @Step
    private static List<String> readTextFromSlide(XSLFSlide slide) {
        return Arrays.stream(slide.getShapes()).filter(shape ->
                (shape instanceof XSLFTextShape)).map(sha -> ((XSLFTextShape) sha).getText().trim()).filter(text ->
                !StringUtil.isNullOrEmpty(text)).collect(toList());
    }

    @Step
    private static Map<String, Map<String, String>> readChartsFromSlide(XSLFSlide slide) {
        List<CTPlotArea> plots = slide.getRelations().stream().filter(rel -> rel instanceof XSLFChart).map(re -> ((XSLFChart) re).getCTChart().getPlotArea()).collect(toList());
        List<CTDoughnutChart> donuts = plots.stream().map(CTPlotArea::getDoughnutChartList).flatMap(Collection::stream).collect(Collectors.toList());
        List<CTLineChart> lines = plots.stream().map(CTPlotArea::getLineChartList).flatMap(Collection::stream).collect(Collectors.toList());
        List<CTBubbleChart> bubbles = plots.stream().map(CTPlotArea::getBubbleChartList).flatMap(Collection::stream).collect(Collectors.toList());
        Map<String, Map<String, String>> charts = new HashMap<>();
        charts.putAll(collectChartValuesAsMaps(donuts, CTDoughnutChart.class));
        charts.putAll(collectChartValuesAsMaps(lines, CTLineChart.class));
        charts.putAll(collectChartValuesAsMaps(bubbles, CTBubbleChart.class));
        return charts;

    }

    @Step("Collect all charts to map")
    private static <T extends XmlObject> Map<String, Map<String, String>> collectChartValuesAsMaps(List<T> chartList, Class<T> clazz) {
        Map<String, Map<String, String>> charts = new HashMap<>();
        List<Map<String, String>> chartMaps = chartList.stream().map(ReportingApiPptxSteps::getChartValuesAsMap).collect(toList());
        if (CollectionUtils.isNotEmpty(chartList)) {
            IntStream.range(0, chartMaps.size()).forEach(i -> charts.put(clazz.getSimpleName() + "_" + i, chartMaps.get(i)));
        }
        return charts;
    }

    @Step("Collect values from chart to map")
    private static <V extends XmlObject> Map<String, String> getChartValuesAsMap(V chart) {
        Attacher.attachAction(chart.getClass().getCanonicalName());
        if (chart instanceof CTDoughnutChart) {
            return readValuesFromDoughnutChart((CTDoughnutChart) chart);
        } else if (chart instanceof CTLineChart) {
            return readValuesFromLineChart((CTLineChart) chart);
        } else if (chart instanceof CTBubbleChart) {
            return readValuesFromLineChart((CTBubbleChart) chart);
        }
        return new HashMap<>();
    }

    @Step
    private static Map<String, String> readValuesFromLineChart(CTBubbleChart chart) {
        Map<String, String> chartValues = new HashMap<>();
        CTBubbleSer targetShopData = chart.getSerList().get(1);
        String name = targetShopData.getTx().getV();
        String x = targetShopData.getXVal().getNumRef().getNumCache().getPtList().stream().map(CTNumVal::getV).findAny().orElse("0");
        String y = targetShopData.getYVal().getNumRef().getNumCache().getPtList().stream().map(CTNumVal::getV).findAny().orElse("0");
        String radius = targetShopData.getBubbleSize().getNumRef().getNumCache().getPtList().stream().map(CTNumVal::getV).findAny().orElse("0");
        chartValues.put(name, x + ":" + y + ":" + radius);
        Attacher.attach("Chart Values", chartValues);
        return chartValues;
    }

    @Step
    private static Map<String, String> readValuesFromLineChart(CTLineChart chart) {
        Map<String, String> chartValues = new HashMap<>();
        List<CTStrVal> names = chart.getSerList().get(0).getCat().getStrRef().getStrCache().getPtList();
        List<CTNumVal> values = chart.getSerList().get(0).getVal().getNumRef().getNumCache().getPtList();
        IntStream.range(0, names.size()).forEach(num -> chartValues.put(names.get(num).getV(), values.get(num).getV()));
        Attacher.attach("Chart Values", chartValues);
        return chartValues;
    }

    @Step
    private static Map<String, String> readValuesFromDoughnutChart(CTDoughnutChart chart) {
        Map<String, String> chartValues = new HashMap<>();
        List<CTStrVal> names = chart.getSerList().get(0).getCat().getStrRef().getStrCache().getPtList();
        List<CTNumVal> values = chart.getSerList().get(0).getVal().getNumRef().getNumCache().getPtList();
        IntStream.range(0, names.size()).forEach(num -> chartValues.put(names.get(num).getV(), values.get(num).getV()));
        Attacher.attach("Chart Values", chartValues);
        return chartValues;
    }


    @Step("Detect slide current region and category by first line")
    private static String getTargetValue(Collection<String> regions, String currentRegion, String text) {
        List<String> targetReg = regions.stream().filter(text::contains).collect(toList());
        if (targetReg.size() == 1) {
            return targetReg.get(0);
        } else if (targetReg.size() > 1) {
            String pretendent = targetReg.stream().sorted(Comparator.comparingInt(p -> p.toString().length()).reversed()).findFirst().orElse(currentRegion);
            targetReg.remove(pretendent);
            return targetReg.stream().filter(reg -> !pretendent.contains(reg)).count() > 0 ? currentRegion : pretendent;
        }
        if (text.contains(SUMMARY)) return "";
        return currentRegion;
    }
}
