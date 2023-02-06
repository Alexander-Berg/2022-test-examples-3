package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.AdGroupGetItem;
import com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria;
import com.yandex.direct.api.v5.adgroups.GetRequest;
import com.yandex.direct.api.v5.adgroups.GetResponse;
import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Assert;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateCampaignSourceTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private TestCampaignRepository testCampaignRepository;
    @Autowired
    private ApiAuthenticationSource auth;
    @Autowired
    private GetAdGroupsDelegate delegate;
    @Autowired
    private GenericApiService genericApiService;

    private ClientInfo clientInfo;

    private Map<Long, CampaignSource> adGroupCampaignSources;

    @Parameterized.Parameter
    public RequestSource requestSource;

    @Parameterized.Parameter(1)
    public List<CampaignSource> readableCampaignSources;

    @Parameterized.Parameters(name = "RequestSource = {0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {RequestSource.API_DEFAULT, List.of(CampaignSource.DIRECT, CampaignSource.DC, CampaignSource.API,
                        CampaignSource.XLS, CampaignSource.WIDGET)},
                {RequestSource.API_EDA, List.of(CampaignSource.DIRECT, CampaignSource.EDA)},
                {RequestSource.API_USLUGI, List.of(CampaignSource.DIRECT, CampaignSource.USLUGI)},
                {RequestSource.API_GEO, List.of(CampaignSource.DIRECT, CampaignSource.GEO)},
                {RequestSource.API_K50, List.of(CampaignSource.DIRECT, CampaignSource.DC, CampaignSource.API, CampaignSource.XLS, CampaignSource.GEO, CampaignSource.WIDGET)},
        });
    }

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        when(auth.getSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(auth.getRequestSource()).thenReturn(requestSource);

        adGroupCampaignSources = StreamEx.of(CampaignSource.values())
                .toMap(this::createAgGroup, Function.identity());
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void getAdGroups() {
        var idsCriteria = new AdGroupsSelectionCriteria().withIds(adGroupCampaignSources.keySet());
        GetRequest request = new GetRequest()
                .withSelectionCriteria(idsCriteria)
                .withFieldNames(AdGroupFieldEnum.ID);

        GetResponse response = genericApiService.doAction(delegate, request);

        Collection<Long> returnedAdGroupIds = StreamEx.of(response.getAdGroups())
                .map(AdGroupGetItem::getId)
                .toList();
        Collection<CampaignSource> actualEditableCampaignSources = StreamEx.of(returnedAdGroupIds)
                .map(adGroupCampaignSources::get)
                .toList();
        Assert.assertThat(actualEditableCampaignSources.size(), is(readableCampaignSources.size()));
        Assert.assertThat(actualEditableCampaignSources, containsInAnyOrder(readableCampaignSources.toArray()));
    }

    private Long createAgGroup(CampaignSource source) {
        var adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        testCampaignRepository.setSource(adGroup.getShard(), adGroup.getCampaignId(), source);
        return adGroup.getAdGroupId();
    }

}
