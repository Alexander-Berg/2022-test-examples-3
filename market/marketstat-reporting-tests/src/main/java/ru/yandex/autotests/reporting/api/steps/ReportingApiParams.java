package ru.yandex.autotests.reporting.api.steps;

import com.hazelcast.util.StringUtil;
import org.fest.util.Collections;
import java.time.LocalDateTime;

import java.util.*;

import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.*;

/**
 * Created by kateleb on 07.02.17.
 */
public class ReportingApiParams {
    private boolean buildCpc;
    private boolean buildCpa;
    private boolean buildAssortment;
    private boolean buildPrice;
    private boolean buildForecaster;

    private String shop;
    private String domain;
    private List<Integer> categories;
    private List<Integer> regions;
    private LocalDateTime period;
    private int months;
    private int minThreshold;
    private int periodLength;
    private int numModels;
    private boolean groupByMonth;
    private Map<String, Object> otherParams;
    private static final Map<String, Object> DEFAULTS;
    private static final String CPC_SLIDES = "cpc_slides";

    static {
        DEFAULTS = new HashMap<>();
        DEFAULTS.put(CPC_SLIDES, Arrays.asList(1, 2));
    }

    private ReportingApiParams(Map<String, Object> params) {
        this(String.valueOf(params.get(SHOP)), String.valueOf(params.get(DOMAIN)),
                (List<Integer>) params.get(REGIONS), (List<Integer>) params.get(CATEGORIES), (LocalDateTime) params.get(PERIOD));
    }

    private ReportingApiParams(String shop, String domain, List<Integer> regions, List<Integer> categories, LocalDateTime period) {
        checkParams(shop, domain, regions, categories, period);
        this.shop = shop;
        this.domain = domain;
        this.regions = regions;
        this.categories = categories;
        this.period = period;
        this.months = 1;
        this.minThreshold = 5;
        this.periodLength = 7;
        this.numModels = -1;
        this.groupByMonth = false;
        this.otherParams = DEFAULTS;
    }

    public static ReportingApiParams forBigShop(ReportingApiParamsProvider provider) {
        return new ReportingApiParams(provider.getParamsForBigShop());
    }

    public static ReportingApiParams forBiggestShopAndRegion(ReportingApiParamsProvider provider) {
        return new ReportingApiParams(provider.getParamsForBestShops());
    }

    public static ReportingApiParams forBigShopWithManyParams(ReportingApiParamsProvider provider) {
        return new ReportingApiParams(provider.getParamsForBigShopManyParams());
    }

    public int getMinThreshold() {
        return minThreshold;
    }

    public int getPeriodsLength() {
        return periodLength;
    }

    public int getNumModels() {
        return numModels;
    }

    public boolean isGroupByMonth() {
        return groupByMonth;
    }

    public String getShop() {
        return shop;
    }

    public String getDomain() {
        return domain;
    }

    public List<Integer> getCategories() {
        return categories;
    }

    public List<Integer> getRegions() {
        return regions;
    }

    public LocalDateTime getPeriod() {
        return period;
    }

    public boolean allSlidesPresent() {
        return buildAssortment() && buildCpa() && buildCpc() && buildPrice() && buildForecaster();
    }

    public boolean buildCpa() {
        return buildCpa;
    }

    public boolean buildCpc() {
        return buildCpc;
    }

    public boolean buildPrice() {
        return buildPrice;
    }

    public boolean buildAssortment() {
        return buildAssortment;
    }

    public boolean buildForecaster() {
        return buildForecaster;
    }

    public boolean isCpaOnly() {
        return buildCpa && !buildCpc && !buildAssortment && !buildForecaster && !buildPrice;
    }

    public boolean isCpcOnly() {
        return !buildCpa && buildCpc && !buildAssortment && !buildForecaster && !buildPrice;
    }

    public boolean isAssortmentOnly() {
        return !buildCpa && !buildCpc && buildAssortment && !buildForecaster && !buildPrice;
    }

    public boolean isPriceOnly() {
        return !buildCpa && !buildCpc && !buildAssortment && !buildForecaster && buildPrice;
    }

    public boolean isForecasterOnly() {
        return !buildCpa && !buildCpc && !buildAssortment && !buildPrice && buildForecaster;
    }

    public boolean isCpcOnlyFirstSlide() {
        List<Integer> slides = (List<Integer>) otherParams.get(CPC_SLIDES);
        return isCpcOnly() && slides.contains(1) && !slides.contains(2);
    }

    public boolean isCpcOnlySecondSlide() {
        List<Integer> slides = (List<Integer>) otherParams.get(CPC_SLIDES);
        return isCpcOnly() && !slides.contains(1) && slides.contains(2);
    }

    /*================ Slides configuration =====================*/

    public ReportingApiParams withAllSlides() {
        return withCpaSlide().withCpcSlide().withAssortment().withPrices().withForecaster();
    }

    public ReportingApiParams withCpcSlide() {
        this.buildCpc = true;
        return this;
    }

    public ReportingApiParams withCpaSlide() {
        this.buildCpa = true;
        return this;
    }

    public ReportingApiParams withAssortment() {
        this.buildAssortment = true;
        return this;
    }

    public ReportingApiParams withPrices() {
        this.buildPrice = true;
        return this;
    }

    public ReportingApiParams withForecaster() {
        this.buildForecaster = true;
        return this;
    }

    public ReportingApiParams cpcSlide1only() {
        this.otherParams.put(CPC_SLIDES, java.util.Collections.singletonList(1));
        return this;
    }

    public ReportingApiParams cpcSlide2only() {
        this.otherParams.put(CPC_SLIDES, java.util.Collections.singletonList(2));
        return this;
    }

    /*================ Slide params configuration =====================*/

    public int getMonths() {
        return months;
    }

    public ReportingApiParams forMonths(int months) {
        this.months = months;
        return this;
    }

    public ReportingApiParams numModels(int num) {
        this.numModels = num;
        return this;
    }

    public ReportingApiParams groupedByMonth() {
        this.groupByMonth = true;
        return this;
    }

    public ReportingApiParams minThreshold(int clicks) {
        this.minThreshold = clicks;
        return this;
    }

    private void checkParams(String shop, String domain, List<Integer> regions, List<Integer> categories, LocalDateTime period) {
        if (StringUtil.isNullOrEmpty(shop) || StringUtil.isNullOrEmpty(domain) ||
                Collections.isNullOrEmpty(regions) || Collections.isNullOrEmpty(categories) || Objects.isNull(period)) {
            throw new IllegalArgumentException("Required params for report are empty!\n" + shop + "\n" + domain);
        }
    }
}
