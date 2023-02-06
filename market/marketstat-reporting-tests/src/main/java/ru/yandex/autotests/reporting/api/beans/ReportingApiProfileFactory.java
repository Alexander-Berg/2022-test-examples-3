package ru.yandex.autotests.reporting.api.beans;

import java.time.LocalDateTime;
import ru.yandex.autotests.market.common.attacher.Attacher;

import java.util.List;

/**
 * Created by kateleb on 15.11.16.
 */
public class ReportingApiProfileFactory {

    public static ReportingApiProfile cpcSimpleProfile(String shop, String domain,
                                                       List<Integer> categories, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiProfile reportingApiProfile = getBaseProfileForParams(shop, domain, categories, regions)
                .withComponent(ReportingApiComponent.cpcSlide1(eventtime))
                .withComponent(ReportingApiComponent.cpcSlide2(eventtime));
        Attacher.attach("Profile", reportingApiProfile.toString());
        return reportingApiProfile;
    }

    public static ReportingApiProfile cpaSimpleProfile(String shop, String domain,
                 List<Integer> categories, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiProfile reportingApiProfile = getBaseProfileForParams(shop, domain, categories, regions)
                .withComponent(ReportingApiComponent.cpaSlide1(eventtime));
        Attacher.attach("Profile", reportingApiProfile.toString());
        return reportingApiProfile;
    }

    public static ReportingApiProfile allComponents(String shop, String domain,
                                                    List<Integer> categories, List<Integer> regions,
                                                    int numModels, boolean groupByMonths, int clickThreshold, int periodLength, LocalDateTime... eventtime) {
        ReportingApiProfile reportingApiProfile = cpcSimpleProfile(shop, domain, categories, regions, eventtime)
                .withComponent(ReportingApiComponent.cpaSlide1(eventtime))
                .withComponent(ReportingApiComponent.assortment(numModels, groupByMonths, eventtime))
                .withComponent(ReportingApiComponent.prices(clickThreshold, eventtime))
                .withComponent(ReportingApiComponent.forecaster(periodLength));
        Attacher.attach("Profile", reportingApiProfile.toString());
        return reportingApiProfile;
    }

    public static ReportingApiProfile cpcCpaProfile(String shop, String domain,
                                                    List<Integer> categories, List<Integer> regions,  LocalDateTime... eventtime) {
        ReportingApiProfile reportingApiProfile = cpcSimpleProfile(shop, domain, categories, regions, eventtime)
                .withComponent(ReportingApiComponent.cpaSlide1(eventtime));
        Attacher.attach("Profile", reportingApiProfile.toString());
        return reportingApiProfile;
    }

    public static ReportingApiProfile cpcAssortmentSimpleProfile(String shop, String domain,
                                                          List<Integer> categories, List<Integer> regions, int numModels, boolean groupByMonths, LocalDateTime... eventtime) {
        ReportingApiProfile reportingApiProfile = cpcSimpleProfile(shop, domain, categories, regions, eventtime)
                .withComponent(ReportingApiComponent.assortment(numModels, groupByMonths, eventtime));
        Attacher.attach("Profile", reportingApiProfile.toString());
        return reportingApiProfile;
    }

    public static ReportingApiProfile cpc1stSlideSimpleProfile(String shop, String domain,
                  List<Integer> categories, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiProfile reportingApiProfile = getBaseProfileForParams(shop, domain, categories, regions)
                .withComponent(ReportingApiComponent.cpcSlide1(eventtime));
        Attacher.attach("Profile", reportingApiProfile.toString());
        return reportingApiProfile;
    }

    public static ReportingApiProfile cpc2ndSlideSimpleProfile(String shop, String domain,
                                                               List<Integer> categories, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiProfile reportingApiProfile = getBaseProfileForParams(shop, domain, categories, regions)
                .withComponent(ReportingApiComponent.cpcSlide2(eventtime));
        Attacher.attach("Profile", reportingApiProfile.toString());
        return reportingApiProfile;
    }

    private static ReportingApiProfile getBaseProfileForParams(String shop, String domain, List<Integer> categories, List<Integer> regions) {
        return new ReportingApiProfile(shop)
                .withCategories(categories)
                .withDomain(domain)
                .withRegions(regions);
    }

    public static ReportingApiProfile pricesOnlyProfile(String shop, String domain,
                                                               List<Integer> categories, List<Integer> regions, int minThreshold, LocalDateTime... eventtime) {
        return getBaseProfileForParams(shop, domain, categories, regions)
                .withComponent(ReportingApiComponent.prices(minThreshold,  eventtime));
    }

    public static ReportingApiProfile forecasterOnlyProfile(String shop, String domain,
                                                        List<Integer> categories, List<Integer> regions, int periodLength) {
        return getBaseProfileForParams(shop, domain, categories, regions)
                .withComponent(ReportingApiComponent.forecaster(periodLength));
    }

    public static ReportingApiProfile profileWithAssortment(String shop, String domain,
                                                               List<Integer> categories, List<Integer> regions, int numModels, boolean groupByMonths, LocalDateTime... eventtime) {
        return getBaseProfileForParams(shop, domain, categories, regions)
                .withComponent(ReportingApiComponent.assortment(numModels, groupByMonths, eventtime));
    }

}
