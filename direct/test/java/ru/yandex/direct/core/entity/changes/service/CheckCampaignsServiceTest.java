package ru.yandex.direct.core.entity.changes.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignSourceUtils;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.changes.model.CheckCampaignsIntResp;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CheckCampaignsServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private CheckCampaignsService checkCampaignsService;

    private ClientInfo clientInfo;

    private static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {
                        "campaign last change after request time",
                        LocalDateTime.now(),
                        false,
                        LocalDateTime.now().minusDays(10),
                        true
                },
                {
                        "universal campaigns are filtered",
                        LocalDateTime.now(),
                        true,
                        LocalDateTime.now().minusDays(10),
                        false
                },
                {
                        "campaign last change before request time",
                        LocalDateTime.now().minusDays(3),
                        false,
                        LocalDateTime.now().minusDays(2),
                        false
                },
                {
                        "campaign last change is null",
                        null,
                        false,
                        LocalDateTime.now().minusDays(2),
                        false
                },
        });
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "params")
    public void test(@SuppressWarnings("unused") String description,
                     LocalDateTime lastChange, boolean isUniversal,
                     LocalDateTime requestDateTime, boolean result) {
        var campaign = fullTextCampaign()
                .withIsUniversal(isUniversal);
        if (lastChange != null) {
            campaign.withLastChange(lastChange);
        }
        var campaignInfo = steps.textCampaignSteps().createCampaign(clientInfo, campaign);
        if (lastChange == null) {
            steps.campaignSteps().setLastChange(clientInfo.getShard(), campaignInfo.getCampaignId(), null);
        }
        List<CheckCampaignsIntResp> checkCampaignsIntResps = checkCampaignsService.getCampaignsChanges(
                clientInfo.getClientId(), clientInfo.getShard(), requestDateTime, false,
                CampaignSourceUtils.getApi5VisibleSources(RequestSource.DEFAULT));
        assertThat(checkCampaignsIntResps.stream().anyMatch(t -> t.getCampaignId().equals(campaignInfo.getCampaignId())),
                Matchers.is(result));
    }
}
