package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.yandex.direct.api.v5.adgroups.DeleteRequest;
import com.yandex.direct.api.v5.adgroups.DeleteResponse;
import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.IdsCriteria;
import one.util.streamex.StreamEx;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.adgroups.AdGroupTypeValidationService;
import ru.yandex.direct.api.v5.entity.adgroups.converter.DeleteAdGroupsRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(Parameterized.class)
public class DeleteAdGroupsDelegateCampaignSourceTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private TestCampaignRepository testCampaignRepository;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private DeleteAdGroupsRequestConverter requestConverter;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestCampaignAccessibilityCheckerProvider;
    @Mock
    private ApiAuthenticationSource auth;

    private DeleteAdGroupsDelegate delegate;

    private GenericApiService genericApiService;

    private ClientInfo clientInfo;

    private Map<Long, CampaignSource> adGroupCampaignSources;

    @Parameterized.Parameter
    public RequestSource requestSource;

    @Parameterized.Parameter(1)
    public List<CampaignSource> editableCampaignSources;

    @Parameterized.Parameters(name = "RequestSource = {0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {RequestSource.API_DEFAULT, List.of(CampaignSource.DIRECT, CampaignSource.DC, CampaignSource.API, CampaignSource.XLS, CampaignSource.WIDGET)},
                {RequestSource.API_EDA, List.of(CampaignSource.DIRECT, CampaignSource.EDA)},
                {RequestSource.API_USLUGI, List.of(CampaignSource.DIRECT, CampaignSource.USLUGI)},
                {RequestSource.API_GEO, List.of(CampaignSource.DIRECT, CampaignSource.GEO)},
                {RequestSource.API_K50, List.of(CampaignSource.DIRECT, CampaignSource.DC, CampaignSource.API, CampaignSource.XLS, CampaignSource.GEO, CampaignSource.WIDGET)},
        });
    }

    @Before
    public void setUp() {
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder, mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class), requestCampaignAccessibilityCheckerProvider);

        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        when(auth.getSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(auth.getRequestSource()).thenReturn(requestSource);
        AdGroupTypeValidationService typeValidationService = new AdGroupTypeValidationService(auth, adGroupService);
        delegate = new DeleteAdGroupsDelegate(auth,
                requestConverter,
                resultConverter,
                adGroupService,
                typeValidationService,
                ppcPropertiesSupport,
                featureService);

        adGroupCampaignSources = StreamEx.of(CampaignSource.values())
                .toMap(this::createAgGroup, Function.identity());
    }

    @Test
    public void deleteAdGroups() {
        var idsCriteria = new IdsCriteria().withIds(adGroupCampaignSources.keySet());
        DeleteRequest request = new DeleteRequest().withSelectionCriteria(idsCriteria);

        DeleteResponse response = genericApiService.doAction(delegate, request);

        Collection<Long> deletedAdGroupIds = StreamEx.of(response.getDeleteResults())
                .filter(addResult -> addResult.getErrors().isEmpty())
                .map(ActionResult::getId)
                .toList();
        Collection<CampaignSource> actualEditableCampaignSources = StreamEx.of(deletedAdGroupIds)
                .map(adGroupCampaignSources::get)
                .toList();
        Assert.assertThat(actualEditableCampaignSources.size(), is(editableCampaignSources.size()));
        Assert.assertThat(actualEditableCampaignSources, containsInAnyOrder(editableCampaignSources.toArray()));
    }

    private Long createAgGroup(CampaignSource source) {
        var adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        testCampaignRepository.setSource(adGroup.getShard(), adGroup.getCampaignId(), source);
        return adGroup.getAdGroupId();
    }

}
