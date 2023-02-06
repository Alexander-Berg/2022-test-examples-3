package ru.yandex.direct.intapi.entity.campaigns.allowedpageids;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@IntApiTest
public class CampaignAllowedPageIdsControllerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    CampaignAllowedPageIdsController controller;

    @Autowired
    public CampaignSteps campaignSteps;

    private Long filledPageIdsCampaignId;
    private Long notFilledCampaignId;
    private Long notExistingCampaignId = Long.MAX_VALUE - 1L; // ¯\_(ツ)_/¯

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        filledPageIdsCampaignId = campaignSteps.createDefaultCampaign().getCampaignId();
        notFilledCampaignId = campaignSteps.createDefaultCampaign().getCampaignId();

        HttpStatus httpStatus = controller
                .setCampaignAllowedPageIds(String.valueOf(filledPageIdsCampaignId), List.of("123", "456"));
        assumeTrue("setting allowed pages request returns 200 OK",
                httpStatus.value() == 200 && httpStatus.getReasonPhrase().equals("OK"));
    }


    @Test
    public void setPageIdsForNonExistingCampaignIdFails() {
        thrown.expect(IntApiException.class);
        thrown.expectMessage("{\"code\":\"NOT_FOUND\",\"message\":\"Not found\"}");
        controller.setCampaignAllowedPageIds(String.valueOf(notExistingCampaignId), null);
    }

    @Test
    public void getCampaignsAllowedPageIds() {
        Map<Long, List<Long>> campaignsAllowedPageIds = controller.getCampaignsAllowedPageIds(
                Stream.of(filledPageIdsCampaignId, notFilledCampaignId, notExistingCampaignId)
                        .map(String::valueOf).collect(Collectors.toList())
        );

        Map<Long, List<Long>> expectedResult = new HashMap<>();
        expectedResult.put(filledPageIdsCampaignId, List.of(123L, 456L));
        expectedResult.put(notFilledCampaignId, Collections.emptyList());
        assertEquals("Got expected pageIds", expectedResult, campaignsAllowedPageIds);
    }
}
