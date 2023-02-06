package ru.yandex.direct.intapi.entity.metrika.model.objectinfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.PpcdictSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.controller.MetrikaExportObjectInfoController;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.intapi.entity.metrika.model.objectinfo.TestObjectInfo.OBJECT_INFO_CREATIVES;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaExportObjectInfoForCreativesLimit10Test {
    private static final int LIMIT = 10;

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

    private List<Long> unexpectedIds;

    @Before
    public void initTestData() {
        mockMvc = mockMvcCreator.setup(controller).build();

        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        steps.adGroupSteps().createAdGroup(new AdGroupInfo().withCampaignInfo(campaignInfo));

        LocalDateTime lastChangeTime = ppcdictSteps.getTimestamp()
                .toLocalDateTime();

        unexpectedIds = createData(campaignInfo, 5, lastChangeTime.minusMinutes(2));

        Long creativeId = createData(campaignInfo, LIMIT + 2, lastChangeTime.plusSeconds(5)).get(LIMIT + 1);

        String lastChangeTimeString = lastChangeTime.toString();
        String timeToken = lastChangeTimeString + "/" + creativeId;

        requestBuilder = MockMvcRequestBuilders.get(OBJECT_INFO_CREATIVES).contentType(MediaType.APPLICATION_JSON);
        requestBuilder.param("time_token", timeToken);
        requestBuilder.param("limit", "10");
    }

    @Test
    public void objectInfoForCreativesLimit10Test() throws Exception {
        String responseContent = mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        MetrikaObjectInfoResponse<CreativeInfo> response =
                JsonUtils.fromJson(responseContent, new TypeReference<>() {
                });

        assertThat(response.getObjects())
                .extracting(CreativeInfo::getSortId)
                .hasSize(10)
                .doesNotContainAnyElementsOf(unexpectedIds);
    }

    private List<Long> createData(CampaignInfo campaignInfo, int amount, LocalDateTime moderateSendTime) {
        List<Long> ids = new ArrayList<>();
        for (int counter = 0; counter < amount; counter++) {
            var creativeInfo = steps.creativeSteps().createCreative(campaignInfo.getClientInfo());
            steps.creativeSteps().setCreativeModerateSendTime(creativeInfo, moderateSendTime);
            steps.bannerCreativeSteps().createCpcVideoBannerCreative(creativeInfo.getCreative(),
                    campaignInfo.getClientInfo());
            ids.add(creativeInfo.getCreativeId());
        }
        return ids;
    }

}
