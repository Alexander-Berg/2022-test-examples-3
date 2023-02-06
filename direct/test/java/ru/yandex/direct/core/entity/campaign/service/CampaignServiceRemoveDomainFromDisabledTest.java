package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.qatools.allure.annotations.Description;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.StatusBsSynced.NO;
import static ru.yandex.direct.core.entity.StatusBsSynced.YES;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка удаления домена из черного списка")
public class CampaignServiceRemoveDomainFromDisabledTest {
    private static final int SHARD = 1;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    private static final String DOMAIN1 = "ya.ru";
    private static final String DOMAIN2 = "yandex.ru";
    private static final String DOMAIN_FOR_REMOVAL = "wrong.com";

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Parameterized.Parameter()
    public Set<String> initialDomains;

    @Parameterized.Parameter(1)
    public Set<String> finalDomains;

    @Parameterized.Parameter(2)
    public StatusBsSynced statusBsSynced;

    @Parameterized.Parameter(3)
    public Boolean lastChangeWasChanged;

    @Parameterized.Parameters(name = "initialDomains = {0}, finalDomains = {1}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {null, null, YES, true},
                {singleton(DOMAIN1), singleton(DOMAIN1), YES, true},
                {singleton(DOMAIN_FOR_REMOVAL), null, NO, false},
                {ImmutableSet.of(DOMAIN1, DOMAIN_FOR_REMOVAL), singleton(DOMAIN1), NO, false},
                {ImmutableSet.of(DOMAIN1, DOMAIN2, DOMAIN_FOR_REMOVAL), ImmutableSet.of(DOMAIN1, DOMAIN2), NO, false},
        });
    }

    private Campaign campaign;
    private LocalDateTime lastChange;

    @Before
    public void before() {
        lastChange = LocalDateTime.now().minus(1, HOURS).truncatedTo(SECONDS);
        CampaignInfo campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withDisabledDomains(initialDomains)
                        .withStatusBsSynced(YES)
                        .withLastChange(lastChange));
        Long campaignId = campaignInfo.getCampaignId();

        campaignService.removeDomainFromDisabled(SHARD, singletonList(campaignId), DOMAIN_FOR_REMOVAL);
        campaign = campaignService.getCampaigns(campaignInfo.getClientId(), singletonList(campaignId)).get(0);
    }

    @Test
    public void domainIsRemovedTest() {
        assertEquals(finalDomains, campaign.getDisabledDomains());
    }

    @Test
    public void statusIsCorrectTest() {
        assertEquals(statusBsSynced, campaign.getStatusBsSynced());
    }

    @Test
    public void lastChangeIsCorrectTest() {
        assertEquals(lastChangeWasChanged, campaign.getLastChange().equals(lastChange));
    }
}
