package ru.yandex.autotests.direct.cmd.data.stat.filter;

import ru.yandex.autotests.direct.cmd.data.campaigns.CampaignTypeEnum;

import java.util.ArrayList;

public class FiltersFactory {

    public static JsonFiltersSet simpleFilter(String name) {
        Data data = new Data();
        CampaignType campaignType = new CampaignType();
        ArrayList<String> list = new ArrayList<String>();
        list.add(CampaignTypeEnum.TEXT.getValue());
        list.add(CampaignTypeEnum.MOBILE.getValue());
        campaignType.setEq(list);
        data.setCampaignType(campaignType);

        JsonFiltersSet filtersSet = new JsonFiltersSet();
        filtersSet.setName(name);
        filtersSet.setData(data);

        return filtersSet;
    }

    public static JsonFiltersSet simpleFilter() {
        return simpleFilter("simple filter");
    }
}
