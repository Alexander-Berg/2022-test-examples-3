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

import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.PpcdictSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.controller.MetrikaExportObjectInfoController;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.intapi.entity.metrika.model.objectinfo.TestObjectInfo.OBJECT_INFO_CREATIVES;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaExportObjectInfoForCreativesTest {

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
    private Long orderId;
    private Long creativeId;

    @Before
    public void initTestData() {
        mockMvc = mockMvcCreator.setup(controller).build();
        LocalDateTime lastChangeTime = ppcdictSteps.getTimestamp()
                .toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Creative creative = steps.creativeSteps().createCreative(clientInfo).getCreative();
        BannerCreativeInfo<OldCpcVideoBanner> bannerCreativeInfo = steps.bannerCreativeSteps()
                .createCpcVideoBannerCreative(creative, clientInfo);

        creativeId = bannerCreativeInfo.getCreativeId();
        orderId = bannerCreativeInfo.getBannerInfo().getCampaignInfo().getOrderId();

        String lastChangeTimeString = lastChangeTime.minusMinutes(2).toString();
        String timeToken = lastChangeTimeString + "/" + creativeId;

        requestBuilder = MockMvcRequestBuilders.get(OBJECT_INFO_CREATIVES).contentType(MediaType.APPLICATION_JSON);
        requestBuilder.param("time_token", timeToken);
    }

    @Test
    public void objectInfoForCreativesTest() throws Exception {
        String responseContent = mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        MetrikaObjectInfoResponse<CreativeInfo> response =
                JsonUtils.fromJson(responseContent, new TypeReference<>() {
                });

        var expected = new CreativeInfo(creativeId, orderId, null, LocalDateTime.now());
        assertThat(response.getObjects())
                .usingElementComparatorOnFields("creativeId", "orderId")
                .contains(expected);
    }

}
