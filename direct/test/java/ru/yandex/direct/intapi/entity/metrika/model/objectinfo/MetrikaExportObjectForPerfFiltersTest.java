package ru.yandex.direct.intapi.entity.metrika.model.objectinfo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.core.testing.steps.PpcdictSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.controller.MetrikaExportObjectInfoController;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.intapi.entity.metrika.model.objectinfo.TestObjectInfo.OBJECT_INFO_PERF_FILTERS;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaExportObjectForPerfFiltersTest {
    private static final String FILTER_TEST_NAME = "perfFilterNameForTest";

    @Autowired
    private MetrikaExportObjectInfoController controller;

    @Autowired
    private MockMvcCreator mockMvcCreator;

    @Autowired
    private PpcdictSteps ppcdictSteps;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;
    private MockHttpServletRequestBuilder requestBuilder;
    private Long filterId;
    private Long orderId;

    private LocalDateTime lastChangeTime;

    @Before
    public void initTestData() {
        mockMvc = mockMvcCreator.setup(controller).build();
        lastChangeTime = ppcdictSteps.getTimestamp()
                .toLocalDateTime()
                .minusMinutes(1)
                .truncatedTo(ChronoUnit.SECONDS);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.bsFakeSteps().setOrderId(adGroupInfo.getCampaignInfo());

        BidsPerformanceRecord perfFilter = PerformanceFiltersSteps.getDefaultBidsPerformanceRecord();
        perfFilter.setName(FILTER_TEST_NAME);
        perfFilter.setLastchange(lastChangeTime);
        steps.performanceFilterSteps().addBidsPerformance(adGroupInfo, perfFilter);

        orderId = adGroupInfo.getCampaignInfo().getCampaign().getOrderId();
        filterId = perfFilter.getPerfFilterId();

        String lastChangeTimeString = lastChangeTime.minusMinutes(2).toString();
        String timeToken = lastChangeTimeString + "/" + filterId;

        requestBuilder = MockMvcRequestBuilders.get(OBJECT_INFO_PERF_FILTERS).contentType(MediaType.APPLICATION_JSON);
        requestBuilder.param("time_token", timeToken);
    }

    @Test
    public void objectInfoForPerfFiltersTest() throws Exception {
        String responseContent = mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        MetrikaObjectInfoResponse<PerformanceFilterInfo> response =
                JsonUtils.fromJson(responseContent, new TypeReference<>() {
                });

        assertThat(response.getObjects())
                .contains(new PerformanceFilterInfo(filterId, orderId, FILTER_TEST_NAME, lastChangeTime));

    }

}
