package ru.yandex.market.fmcg.bff.controller.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import ru.yandex.market.fmcg.bff.report.model.ReportSearchResult;

import static java.util.Collections.singletonMap;

class MarketProductDtoTest {

    @Test
    void getTableProperties() {
        ReportSearchResult.Document document = new ReportSearchResult.Document();
        ReportSearchResult.Specs specs = new ReportSearchResult.Specs();
        ReportSearchResult.Group group1 = new ReportSearchResult.Group();
        ReportSearchResult.Group group2 = new ReportSearchResult.Group();
        ReportSearchResult.GroupSpec groupSpec11 = new ReportSearchResult.GroupSpec();
        ReportSearchResult.GroupSpec groupSpec12 = new ReportSearchResult.GroupSpec();
        ReportSearchResult.GroupSpec groupSpec21 = new ReportSearchResult.GroupSpec();
        ReportSearchResult.GroupSpec groupSpec22 = new ReportSearchResult.GroupSpec();
        document.setSpecs(specs);
        specs.setFull(Arrays.asList(group1, group2));
        group1.setGroupSpecs(Arrays.asList(groupSpec11, groupSpec12));
        group2.setGroupSpecs(Arrays.asList(groupSpec21, groupSpec22));
        groupSpec11.setName("specName");
        groupSpec12.setName("specName");
        groupSpec21.setName("specName");
        groupSpec22.setName("specName");
        groupSpec11.setValue("1");
        groupSpec12.setValue("2");
        groupSpec21.setValue("3");
        groupSpec22.setValue("4");
        group1.setGroupName("awesome-group");
        group2.setGroupName("awesome-group");

        Assertions.assertEquals(singletonMap("awesome-group", singletonMap("specName", "1, 2, 3, 4")), MarketProductDto.tableProperties(document));
    }
}
