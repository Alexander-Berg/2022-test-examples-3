package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigInteger;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.keyword.container.AdGroupInfoForKeywordAdd;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImageFormat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationCopyActionTest extends KeywordsAddOperationBaseTest {

    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private TextBannerInfo textBanner;
    private BannerImageInfo<TextBannerInfo> bannerImage;

    private AdGroupInfo adGroupInfoForKeywordCopy;

    @Before
    public void before() {
        super.before();
        campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign(null, null).withOrderId(1L), clientInfo);
        textBanner = bannerSteps.createBanner(activeTextBanner(null, null).withBsBannerId(2L), campaignInfo);
        bannerImage = bannerSteps.createBannerImage(textBanner, defaultBannerImageFormat(null),
                defaultBannerImage(textBanner.getBannerId(), randomAlphanumeric(16)).withBsBannerId(3L));
        adGroupInfo = textBanner.getAdGroupInfo();

        adGroupInfoForKeywordCopy = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
    }

    @Test
    public void copiedKeywordSystemFieldsAreSetWell() {
        Keyword sourceKeyword = defaultKeyword()
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusModerate(StatusModerate.YES)
                .withIsSuspended(Boolean.TRUE)
                .withPhraseBsId(BigInteger.valueOf(2318213L))
                .withNeedCheckPlaceModified(false);
        steps.keywordSteps().createKeyword(adGroupInfo, sourceKeyword);

        Keyword keywordToCopy = KeywordUtils.cloneKeyword(sourceKeyword);

        Long copiedKeywordId = executeCopyOperation(keywordToCopy);

        Keyword expectedKeyword = new Keyword()
                .withCampaignId(keywordToCopy.getCampaignId())
                .withAdGroupId(keywordToCopy.getAdGroupId())
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(StatusModerate.NEW)
                .withIsSuspended(Boolean.FALSE)
                .withPhraseBsId(BigInteger.ZERO)
                .withNeedCheckPlaceModified(true);

        Keyword actualKeyword = keywordRepository
                .getKeywordsByIds(adGroupInfoForKeywordCopy.getShard(),
                        adGroupInfoForKeywordCopy.getClientId(),
                        singletonList(copiedKeywordId)).get(0);
        assertThat(actualKeyword, beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void historyIsNotCreatedWhenHasNoPhraseBsIdAndHasNoOldHistory() {
        KeywordInfo defaultKeyword = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword().withPhraseBsId(BigInteger.ZERO));
        Keyword keyword = defaultKeyword.getKeyword();

        executeCopyOperation(keyword);

        History actualKeywordHistory = keywordRepository
                .getKeywordsByIds(adGroupInfoForKeywordCopy.getShard(), adGroupInfoForKeywordCopy.getClientId(),
                        singletonList(keyword.getId())).get(0).getPhraseIdHistory();
        assertThat(actualKeywordHistory, nullValue());
    }

    @Test
    public void historyIsNotCreatedForNewKeyword() {
        Keyword newKeyword = defaultClientKeyword(adGroupInfo.getAdGroupId());
        executeCopyOperation(newKeyword);
        Keyword keyword = keywordRepository
                .getKeywordsByIds(adGroupInfoForKeywordCopy.getShard(), adGroupInfoForKeywordCopy.getClientId(),
                        singletonList(newKeyword.getId())).get(0);

        assertThat(keyword.getPhraseIdHistory(), nullValue());
    }

    private Long executeCopyOperation(Keyword keyword) {
        KeywordsAddOperation copyOperation = createOperationWithCopyAction(singletonList(keyword));
        copyOperation.setKeywordsAdGroupInfo(singletonMap(0, getAdGroupInfo()));
        Optional<MassResult<AddedKeywordInfo>> prepareResult = copyOperation.prepare();
        assumeThat("этап prepare должен завершиться успешно", prepareResult.isPresent(), is(false));
        copyOperation.setAdGroupsIds(singletonMap(0, adGroupInfoForKeywordCopy.getAdGroupId()));
        MassResult<AddedKeywordInfo> result = copyOperation.apply();
        assumeThat(result, isFullySuccessful());
        return result.get(0).getResult().getId();
    }

    private AdGroupInfoForKeywordAdd getAdGroupInfo() {
        return new AdGroupInfoForKeywordAdd(1, adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupType());
    }
}
