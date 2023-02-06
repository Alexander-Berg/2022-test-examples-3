package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignRepositoryFrontpageCampaignsShowTypeTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void cpmYndxFrontpageCampaignsReadShowTypeFromDbTest() {
        Long desktopCampaignId =
                createFrontpageCampaignWithShowType(ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE));
        Long mobileCampaignId =
                createFrontpageCampaignWithShowType(ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE_MOBILE));
        Long bothTypesCampaignId1 = createFrontpageCampaignWithShowType(
                ImmutableList.of(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE));
        Long bothTypesCampaignId2 = createFrontpageCampaignWithShowType(
                ImmutableList.of(FrontpageCampaignShowType.FRONTPAGE_MOBILE, FrontpageCampaignShowType.FRONTPAGE));
        Long emptyTypeCampaignId = createFrontpageCampaignWithShowType(emptyList());

        List<Long> campaignIds = ImmutableList.of(desktopCampaignId, mobileCampaignId, bothTypesCampaignId1,
                bothTypesCampaignId2, emptyTypeCampaignId);
        Map<Long, Set<FrontpageCampaignShowType>> showTypesByCampaigns =
                campaignRepository.getFrontpageTypesForCampaigns(clientInfo.getShard(), campaignIds);


        assertThat("полученные идентификаторы десктопных кампаний на главной должны совпадать с ожидаемыми",
                showTypesByCampaigns, equalTo(ImmutableMap.of(
                        desktopCampaignId, ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE),
                        mobileCampaignId, ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE_MOBILE),
                        bothTypesCampaignId1, ImmutableSet
                                .of(FrontpageCampaignShowType.FRONTPAGE_MOBILE, FrontpageCampaignShowType.FRONTPAGE),
                        bothTypesCampaignId2, ImmutableSet
                                .of(FrontpageCampaignShowType.FRONTPAGE_MOBILE, FrontpageCampaignShowType.FRONTPAGE),
                        emptyTypeCampaignId, ImmutableSet.of()
                )));
    }

    private Long createFrontpageCampaignWithShowType(Collection<FrontpageCampaignShowType> frontpageCampaignShowTypes) {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        testCpmYndxFrontpageRepository
                .setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                        clientInfo.getShard(), campaignId, frontpageCampaignShowTypes);
        return campaignId;
    }
}
