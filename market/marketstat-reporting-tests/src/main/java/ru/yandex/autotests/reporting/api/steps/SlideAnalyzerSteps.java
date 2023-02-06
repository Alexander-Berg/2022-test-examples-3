package ru.yandex.autotests.reporting.api.steps;

import org.apache.commons.collections.CollectionUtils;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBubbleChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDoughnutChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import ru.yandex.autotests.market.stat.matchers.collections.CollectionsMatchers;
import ru.yandex.autotests.reporting.api.beans.*;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by kateleb on 20.03.17.
 */
public class SlideAnalyzerSteps {

    public static final Pattern URL_PATTERN = Pattern.compile("^\\w{1,20}(\\.\\w{1,20}){0,3}\\.\\w{2,4}$");
    public static final Pattern PERIOD_PATTERN = Pattern.compile("^\\(?[А-Яа-я]{3,8}\\s?\\d{4}(\\s?-\\s?[А-Яа-я]{3,8}.\\s?\\d{4}){0,1}\\)?$");
    //slide titles
    public static final String RATINGS = "Рейтинг:";
    //cpa slide headers
    public static final String SHOPS_RATIO_BY_ORDERS = "Доли магазинов по заказанным товарам";
    public static final String DYNAMICS_IN_ORDERS_RATIO = "Динамика доли заказанных товаров";
    public static final String DELIVERY_STATUSES = "Статусы переданных в доставку товаров";
    public static final String CATEGORY_DYNAMICS = "Общая динамика категории";
    public static final Pattern CANCELLED_RATIO_REGEX = Pattern.compile("Доля\\s?\\n?отменённых\\s?\\n?в категории:");
    //cpa slide varnames
    public static final String DELVERED = "Доставлены";
    public static final String START_LEVEL = "1.0";
    public static final String REGION_HEADER = "Регион:";
    //cpc 1 slide headers
    public static final String CLICKS_BY_SHOPS_PRCNT = "Доли магазинов по кликам";
    public static final String MOBILE_VS_DESKTOP = "Распределение Mobile vs Desktop";
    public static final String CLICK_DYNAMICS = "Динамика доли кликов";
    //cpc 2 slide varnames
    public static final String OTHER_SHOPS = "Другие магазины";

    public <T extends ReportingSlide> T getValues(T slide) {
        if (CollectionUtils.isNotEmpty(slide.getLines())) {
            if (slide instanceof CpcSlide) {
                if (slide.getType().equals(CpcSlide.CPC_SLIDE_1))
                    return (T) readCpc1Slide(slide);
                if (slide.getType().equals(CpcSlide.CPC_SLIDE_2))
                    return (T) readCpc2Slide(slide);
            }

            if (slide instanceof CpaSlide && slide.getType().equals(CpaSlide.CPA_SLIDE_1)) {
                return (T) readCpaSlide(slide);
            }
        }
        return slide;
    }


    public <T extends ReportingSlide> CpaSlide1 readCpaSlide(T slide) {
        assertThat("Not enough data on Cpa 1 slide!", slide.getLines(), CollectionsMatchers.containsAllItems(
                new ArrayList(Arrays.asList(SHOPS_RATIO_BY_ORDERS, DYNAMICS_IN_ORDERS_RATIO, DELIVERY_STATUSES, CATEGORY_DYNAMICS))));
        List<Map<String, String>> donuts = getChartsOfType(slide.getCharts(), CTDoughnutChart.class, 2);
        List<Map<String, String>> lines = getChartsOfType(slide.getCharts(), CTLineChart.class, 2);
        List<String> unparsedLines = slide.getLines();
        Map<String, String> deliveredCanceledData = donuts.stream().filter(donchart -> donchart.keySet().contains(DELVERED)).findAny().orElse(new HashMap<>());
        Map<String, String> categoryDynamics = lines.stream().filter(linechart -> linechart.values().contains(START_LEVEL)).findAny().orElse(new HashMap<>());
        String shopOrdersRatio = unparsedLines.stream().filter(l -> l.contains("%")).findFirst().orElse("0%");
        String cancelledRatioHeader = unparsedLines.stream().filter(l -> CANCELLED_RATIO_REGEX.matcher(l).find()).findAny().orElse("not found");
        String cancelledOrdersRatio = unparsedLines.get(unparsedLines.indexOf(cancelledRatioHeader) + 1);
        CpaSlide1 cpaSlide = new CpaSlide1(slide);
        cpaSlide.setCancelledOrdersCategoryShare(cancelledOrdersRatio);
        cpaSlide.setCompletedCancelledDiagram(deliveredCanceledData);
        cpaSlide.setPeriods(getPeriods(unparsedLines));
        cpaSlide.setFooter(getFooter(unparsedLines));
        cpaSlide.setShopRating(unparsedLines.subList(unparsedLines.indexOf(cancelledRatioHeader) + 2, unparsedLines.size()));
        cpaSlide.setCategoryDynamicDiagram(categoryDynamics);
        cpaSlide.setOrdersCount(Integer.valueOf(unparsedLines.get(unparsedLines.indexOf(shopOrdersRatio) + 1)));
        cpaSlide.setOrdersShareDiagram(getAnotherFrom(donuts, deliveredCanceledData));
        cpaSlide.setOrdersShare(shopOrdersRatio);
        cpaSlide.setOrdersShareDynamicDiagram(getAnotherFrom(lines, categoryDynamics));
        return cpaSlide;
    }

    public <T extends ReportingSlide> CpcSlide2 readCpc2Slide(T slide) {
        assertThat("Not enough data on Cpc 2 slide!", slide.getLines(), CollectionsMatchers.containsAllItems(
                new ArrayList(Collections.singletonList(OTHER_SHOPS))));
        List<String> unparsedLines = slide.getLines();
        List<Map<String, String>> bubbles = getChartsOfType(slide.getCharts(), CTBubbleChart.class, 2);
        CpcSlide2 cpcSlide = new CpcSlide2(slide);
        cpcSlide.setCompetitionMapRightDiagram(bubbles.get(0));
        cpcSlide.setCompetitionMapLeftDiagram(bubbles.get(1));
        cpcSlide.setFooter(getFooter(unparsedLines));
        cpcSlide.setPeriods(getPeriods(unparsedLines));
        cpcSlide.setTargetShop(unparsedLines.stream().filter(l -> URL_PATTERN.matcher(l).find()).distinct().findAny().orElse(""));
        return cpcSlide;
    }

    private String getFooter(List<String> unparsedLines) {
        return unparsedLines.stream().filter(l -> l.contains(REGION_HEADER)).findFirst().orElse("");
    }

    public <T extends ReportingSlide> CpcSlide1 readCpc1Slide(T slide) {
        assertThat("Not enough data on Cpc 1 slide!", slide.getLines(), CollectionsMatchers.containsAllItems(
                new ArrayList(Arrays.asList(CLICKS_BY_SHOPS_PRCNT, MOBILE_VS_DESKTOP, CLICK_DYNAMICS, CATEGORY_DYNAMICS, RATINGS))));
        List<String> unparsedLines = slide.getLines();
        List<Map<String, String>> donuts = getChartsOfType(slide.getCharts(), CTDoughnutChart.class, 2);
        List<Map<String, String>> lines = getChartsOfType(slide.getCharts(), CTLineChart.class, 2);
        Map<String, String> mobileVsDesktopData = donuts.stream().filter(donchart -> donchart.keySet().contains("mobile")).findAny().orElse(new HashMap<>());
        Map<String, String> categoryDynamics = lines.stream().filter(linechart -> linechart.values().contains("1.0")).findAny().orElse(new HashMap<>());
        String share = unparsedLines.stream().filter(l -> l.contains("%")).findFirst().orElse("0%");

        CpcSlide1 cpcSlide = new CpcSlide1(slide);
        cpcSlide.setClicksShare(share);
        cpcSlide.setClicksCount(Integer.valueOf(unparsedLines.get(unparsedLines.indexOf(share) + 1)));
        cpcSlide.setMobileDesktopDiagram(mobileVsDesktopData);
        cpcSlide.setCategoryDynamicDiagram(categoryDynamics);
        cpcSlide.setClicksShareDynamicsDiagram(getAnotherFrom(lines, categoryDynamics));
        cpcSlide.setShopRating(unparsedLines.subList(unparsedLines.indexOf(RATINGS) + 1, unparsedLines.size()));
        cpcSlide.setFooter(getFooter(unparsedLines));
        cpcSlide.setPeriods(getPeriods(unparsedLines));
        cpcSlide.setClicksShareDiagram(getAnotherFrom(donuts, mobileVsDesktopData));
        return cpcSlide;
    }

    private List<String> getPeriods(List<String> unparsedLines) {
        return unparsedLines.stream().filter(l -> PERIOD_PATTERN.matcher(l).find()).distinct().collect(toList());
    }

    protected Map<String, String> getAnotherFrom(List<Map<String, String>> charts, Map<String, String> other) {
        return charts.stream().filter(ch -> !ch.equals(other)).findAny().orElse(new HashMap<>());
    }

    protected List<Map<String, String>> getChartsOfType(Map<String, Map<String, String>> charts, Class clazz, int expectedCount) {
        List<Map<String, String>> someCharts = charts.entrySet().stream().filter(e -> e.getKey().contains(clazz.getSimpleName())).map(Map.Entry::getValue).collect(toList());
        assertThat(String.format("Not enough %s charts!", clazz.getSimpleName()), someCharts.size(), is(expectedCount));
        return someCharts;
    }
}
