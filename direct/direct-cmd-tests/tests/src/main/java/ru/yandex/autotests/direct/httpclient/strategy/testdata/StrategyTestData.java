package ru.yandex.autotests.direct.httpclient.strategy.testdata;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.direct.utils.strategy.data.StrategyGroup;

public class StrategyTestData {

    private static List<Object[]> getCampStrategiesList(CampaignTypeEnum campaignType, StrategyGroup... excludeGroups) {
        return Strategies.getStrategyListExcludeGroup(campaignType, excludeGroups).stream()
                .map(t -> new Object[]{t})
                .collect(Collectors.toList());
    }

    public static List<Object[]> getTextCampStrategiesList(StrategyGroup... excludeGroups) {
        return getCampStrategiesList(CampaignTypeEnum.TEXT, excludeGroups);
    }

    public static List<Object[]> getMobileAppCampStrategiesList(StrategyGroup... excludeGroups) {
        return getCampStrategiesList(CampaignTypeEnum.MOBILE, excludeGroups);
    }

    public static List<Object[]> getDynamicCampStrategiesList(StrategyGroup... excludeGroups) {
        return getCampStrategiesList(CampaignTypeEnum.DTO, excludeGroups);
    }

    public static List<Object[]> getPerformanceCampStrategiesList(StrategyGroup... excludeGroups) {
        return getCampStrategiesList(CampaignTypeEnum.DMO, excludeGroups);
    }

    public static List<Object[]> getStrat() {
        List<Object[]> strategies = new ArrayList<>();
        for (Strategies strategy : Strategies.values()) {
            if (strategy.name().contains("ROI_OPTIMIZATION") || strategy.name().endsWith("SHOWS_DISABLED")) {
                continue;
            }
            strategies.add(new Object[]{strategy, "at-daybudget-c", "YNDX"});
        }

        return strategies;
    }
}
