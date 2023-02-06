package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singleton;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignRepositoryGetCampaignsTypeMapTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    private CampaignInfo campaignInfo;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void setUp() {
        campaignInfo = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);
    }

    @Test
    public void testSuccess() {
        Map<Long, CampaignType> campaigns = campaignRepository.getCampaignsTypeMap(
                campaignInfo.getShard(),
                campaignInfo.getClientId(), singleton(campaignInfo.getCampaignId()),
                singleton(campaignInfo.getCampaign().getType()));

        assertThat(campaigns)
                .containsExactly(entry(campaignInfo.getCampaignId(), campaignInfo.getCampaign().getType()));
    }

    @Test
    public void testAnotherClient() {
        Map<Long, CampaignType> campaigns = campaignRepository.getCampaignsTypeMap(
                campaignInfo.getShard(),
                ClientId.fromLong(campaignInfo.getClientId().asLong() + 1), singleton(campaignInfo.getCampaignId()),
                singleton(campaignInfo.getCampaign().getType()));

        assertThat(campaigns).isEmpty();
    }

    @Test
    public void testNotInAllowableTypes() {
        EnumSet<CampaignType> campaignTypes = EnumSet.allOf(CampaignType.class);
        campaignTypes.remove(campaignInfo.getCampaign().getType());
        Map<Long, CampaignType> campaigns = campaignRepository.getCampaignsTypeMap(
                campaignInfo.getShard(),
                campaignInfo.getClientId(), singleton(campaignInfo.getCampaignId()),
                campaignTypes);

        assertThat(campaigns).isEmpty();
    }
}
