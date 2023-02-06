package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupAddItem;
import com.yandex.direct.api.v5.adgroups.AddRequest;
import com.yandex.direct.api.v5.adgroups.AddResponse;
import com.yandex.direct.api.v5.general.ActionResult;
import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AddAdGroupsDelegateCampaignSourceTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private ApiAuthenticationSource auth;

    @Autowired
    private AddAdGroupsDelegate delegate;

    @Autowired
    private GenericApiService genericApiService;

    private ClientInfo clientInfo;

    @Parameterized.Parameter
    public RequestSource requestSource;

    @Parameterized.Parameter(1)
    public List<CampaignSource> editableCampaignSources;

    @Parameterized.Parameters(name = "RequestSource = {0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {RequestSource.API_DEFAULT, List.of(CampaignSource.DIRECT, CampaignSource.DC, CampaignSource.API,
                        CampaignSource.XLS, CampaignSource.WIDGET)},
                {RequestSource.API_EDA, List.of(CampaignSource.DIRECT, CampaignSource.EDA)},
                {RequestSource.API_USLUGI, List.of(CampaignSource.DIRECT, CampaignSource.USLUGI)},
                {RequestSource.API_GEO, List.of(CampaignSource.DIRECT, CampaignSource.GEO)},
                {RequestSource.API_K50,
                        List.of(CampaignSource.DIRECT, CampaignSource.DC, CampaignSource.API, CampaignSource.XLS,
                                CampaignSource.GEO, CampaignSource.WIDGET)},
        });
    }

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(auth.getRequestSource()).thenReturn(requestSource);
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void addAdGroups() {
        Map<Long, CampaignSource> campaignSources = StreamEx.of(CampaignSource.values())
                .toMap(this::createCampaign, Function.identity());
        var request = new AddRequest().withAdGroups(
                StreamEx.ofKeys(campaignSources).map(this::getAdGroupAddItem).toList());

        AddResponse response = genericApiService.doAction(delegate, request);

        Collection<Long> createdAdGroupIds = StreamEx.of(response.getAddResults())
                .filter(addResult -> addResult.getErrors().isEmpty())
                .map(ActionResult::getId)
                .toList();
        Collection<CampaignSource> actualEditableCampaignSources = StreamEx.ofValues(
                        adGroupService.getCampaignIdsByAdgroupIds(clientInfo.getClientId(), createdAdGroupIds))
                .map(campaignSources::get)
                .toList();
        assertThat(actualEditableCampaignSources).containsExactlyInAnyOrderElementsOf(editableCampaignSources);
    }

    private Long createCampaign(CampaignSource source) {
        var campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        testCampaignRepository.setSource(campaign.getShard(), campaign.getCampaignId(), source);
        return campaign.getCampaignId();
    }

    private AdGroupAddItem getAdGroupAddItem(Long campaignId) {
        return new AdGroupAddItem()
                .withName("name")
                .withRegionIds(225L)
                .withCampaignId(campaignId);
    }
}
