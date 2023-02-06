package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.adgroups.AdGroupUpdateItem;
import com.yandex.direct.api.v5.adgroups.ObjectFactory;
import com.yandex.direct.api.v5.adgroups.UpdateRequest;
import com.yandex.direct.api.v5.adgroups.UpdateResponse;
import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import one.util.streamex.StreamEx;
import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.Matcher;
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
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(Parameterized.class)
public class UpdateAdGroupsDelegateCampaignSourceTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    public static final ObjectFactory FACTORY = new ObjectFactory();

    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;
    @Autowired
    private TestCampaignRepository testCampaignRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private ApiAuthenticationSource auth;
    @Autowired
    private UpdateAdGroupsDelegate delegate;
    @Autowired
    private GenericApiService genericApiService;

    private UpdateRequest updateRequest;
    private Long defaultPackId;
    private int shard;
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
    public void before() {
        clientInfo = steps.clientSteps().createClient(new ClientInfo());
        ApiUser apiUser = new ApiUser().withClientId(clientInfo.getClientId()).withUid(clientInfo.getUid());
        when(auth.getSubclient()).thenReturn(apiUser);
        when(auth.getChiefSubclient()).thenReturn(apiUser);
        when(auth.getOperator()).thenReturn(apiUser);
        when(auth.getRequestSource()).thenReturn(requestSource);

        shard = clientInfo.getShard();
        adGroupCampaignSources = StreamEx.of(CampaignSource.values())
                .toMap(this::createAgGroup, Function.identity());
        defaultPackId = steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void updateAdGroups_LinkLibraryMinusKeywords_LibraryMinusKeywordsLinked() {

        JAXBElement<ArrayOfLong> negativeKeywordSharedSetIds = FACTORY
                .createAdGroupBaseNegativeKeywordSharedSetIds(new ArrayOfLong().withItems(defaultPackId));

        updateRequest = new UpdateRequest().withAdGroups(
                StreamEx.ofKeys(adGroupCampaignSources).map(adGroupId -> new AdGroupUpdateItem()
                        .withId(adGroupId)
                        .withNegativeKeywordSharedSetIds(negativeKeywordSharedSetIds)
                ).toList());

        doAction(updateRequest, true);
    }

    @Test
    public void updateAdGroups_UnlinkLibraryMinusKeywords_LibraryMinusKeywordsUnlinked() {

        JAXBElement<ArrayOfLong> emptyArray = FACTORY
                .createAdGroupBaseNegativeKeywordSharedSetIds(new ArrayOfLong());

        updateRequest = new UpdateRequest().withAdGroups(
                StreamEx.ofKeys(adGroupCampaignSources).map(adGroupId -> new AdGroupUpdateItem()
                        .withId(adGroupId)
                        .withNegativeKeywordSharedSetIds(emptyArray)
                ).toList());

        StreamEx.ofKeys(adGroupCampaignSources).forEach(adGroupId -> testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, defaultPackId, adGroupId));

        doAction(updateRequest, false);
    }

    private void doAction(UpdateRequest request, boolean addKeywords) {
        UpdateResponse response = genericApiService.doAction(delegate, request);

        Collection<Long> updatedAdGroupIds = StreamEx.of(response.getUpdateResults())
                .filter(addResult -> addResult.getErrors().isEmpty())
                .map(ActionResult::getId)
                .toList();
        Collection<CampaignSource> actualEditableCampaignSources = StreamEx.of(updatedAdGroupIds)
                .map(adGroupCampaignSources::get)
                .toList();
        assertThat(actualEditableCampaignSources).containsExactlyInAnyOrderElementsOf(editableCampaignSources);

        StreamEx.of(adGroupRepository.getAdGroups(shard, adGroupCampaignSources.keySet())).forEach(adGroup -> {
            boolean isUpdated = updatedAdGroupIds.contains(adGroup.getId());
            boolean isEmptyKeywords = isUpdated ^ addKeywords;
            Matcher matcher = isEmptyKeywords ? empty() : contains(defaultPackId);
            assertThat(adGroup.getLibraryMinusKeywordsIds()).is(new HamcrestCondition(matcher));
        });
    }

    private Long createAgGroup(CampaignSource source) {
        var adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        testCampaignRepository.setSource(adGroup.getShard(), adGroup.getCampaignId(), source);
        return adGroup.getAdGroupId();
    }
}
