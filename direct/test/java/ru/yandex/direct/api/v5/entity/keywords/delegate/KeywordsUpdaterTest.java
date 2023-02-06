package ru.yandex.direct.api.v5.entity.keywords.delegate;

import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.keywords.container.UpdateInputItem;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordOperationFactory;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;

@Api5Test
@RunWith(SpringRunner.class)
public class KeywordsUpdaterTest {
    private KeywordsUpdater keywordsUpdater;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Autowired
    private Steps steps;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private KeywordOperationFactory keywordOperationFactory;

    @Autowired
    private RelevanceMatchService relevanceMatchService;

    @Autowired
    private ClientService clientService;

    private CampaignInfo campaignInfo;
    private KeywordInfo keyword;

    @Before
    public void setUp() {
        campaignInfo = steps.campaignSteps()
                .createCampaign(newTextCampaign(null, null)
                        .withStrategy(manualStrategy().withSeparateBids(false)));

        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), campaignInfo);
        keyword = steps.keywordSteps().createKeyword(bannerInfo.getAdGroupInfo());

        ClientInfo targetUser = campaignInfo.getClientInfo();
        ApiUser apiTargetUser =
                new ApiUser().withClientId(targetUser.getClientId()).withUid(targetUser.getUid());

        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getChiefSubclient()).thenReturn(apiTargetUser);
        when(apiAuthenticationSource.getOperator()).thenReturn(apiTargetUser);

        keywordsUpdater = new KeywordsUpdater(apiAuthenticationSource,
                resultConverter, keywordOperationFactory, relevanceMatchService, clientService);
    }

    @Test
    public void thereIsNoAnyWarningsOnUpdate() {
        ApiMassResult<Long> apiResult = keywordsUpdater.doUpdate(createUpdateRequest());
        assertThat(apiResult.getResult().get(0).getWarnings()).isEmpty();
    }

    @Test
    public void doUpdate_TenThousandKeywords_UpdateSuccessful() {
        ApiMassResult<Long> apiResult =
                keywordsUpdater.doUpdate(createUpdateRequestWithTenThousandElements());
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(apiResult.isSuccessful()).isTrue();
        sa.assertThat(apiResult.getSuccessfulObjectsCount()).isEqualTo(10_000);
        sa.assertThat(apiResult.getUnsuccessfulObjectsCount()).isZero();
        sa.assertAll();
    }

    private List<UpdateInputItem> createUpdateRequest() {
        return singletonList(
                UpdateInputItem.createItemForKeyword(
                        ModelChanges.build(keyword.getId(), Keyword.class, Keyword.HREF_PARAM1, "1")));
    }

    private List<UpdateInputItem> createUpdateRequestWithTenThousandElements() {
        int groups = 5000;    // количество групп, на каждую группу привязана одна ключевая фраза и один автотаргетинг.

        List<AdGroupInfo> adGroupInfos = StreamEx.generate(AdGroupInfo::new)
                .map(g -> g.withCampaignInfo(campaignInfo))
                .limit(groups)
                .toList();
        steps.adGroupSteps().createAdGroups(adGroupInfos);

        List<KeywordInfo> keywordInfos = StreamEx.of(adGroupInfos)
                .map(g -> new KeywordInfo().withAdGroupInfo(g))
                .toList();
        steps.keywordSteps().createKeywords(keywordInfos);

        List<RelevanceMatch> relevanceMatches = steps.relevanceMatchSteps().addDefaultRelevanceMatches(adGroupInfos);

        List<UpdateInputItem> request = new ArrayList<>(groups * 2);
        for (int i = 0; i < groups; i++) {
            request.add(UpdateInputItem.createItemForKeyword(
                    ModelChanges.build(keywordInfos.get(i).getId(), Keyword.class,
                            Keyword.HREF_PARAM1, Integer.toString(i))
            ));
            request.add(UpdateInputItem.createItemForRelevanceMatch(
                    ModelChanges.build(relevanceMatches.get(i).getId(), RelevanceMatch.class,
                            RelevanceMatch.HREF_PARAM1, Integer.toString(i))
            ));
        }
        return request;
    }
}
