package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAdditionalTasksForBannersTest extends KeywordsUpdateOperationBaseTest {

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private static final CompareStrategy COMPARE_STRATEGY = onlyExpectedFields()
            .forFields(newPath("lastChange")).useMatcher(approximatelyNow());

    @Test
    public void execute_NoBanners_WorksFine() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, PHRASE_2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
    }

    @Test
    public void execute_NoChange_BannerNotChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        TextBannerInfo bannerInfo = createActiveTemplateBanner(adGroupInfo1);

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertActiveBannerStatusesIsNotChanged(bannerInfo);
    }

    @Test
    public void execute_NormPhraseNotChanged_BannerIsChanged() {
        String phrase = "конь";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase).getId();
        TextBannerInfo bannerInfo = createActiveTemplateBanner(adGroupInfo1);

        String newPhrase = "коня";
        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, newPhrase);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertActiveBannerStatusesIsNotChanged(bannerInfo);
    }

    @Test
    public void execute_NormPhraseChanged_BannerIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        TextBannerInfo bannerInfo = createActiveTemplateBanner(adGroupInfo1);

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, PHRASE_2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        assertActiveBannerStatusesIsChanged(bannerInfo);
    }

    @Test
    public void execute_NormPhraseChangedWithoutResetStatusModerate_BannerIsChanged() {
        createOneActiveAdGroup();
        String phrase1 = "слон";
        String phrase2 = phrase1 + " купить";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase1).getId();
        TextBannerInfo bannerInfo = createActiveTemplateBanner(adGroupInfo1);

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, phrase2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase2)));

        assertActiveBannerStatusesIsChanged(bannerInfo);
    }

    @Test
    public void execute_TwoTemplateBannersInDifferentAdGroups_BannersAreChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveTemplateBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveTemplateBanner(adGroupInfo2);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        assertActiveBannerStatusesIsChanged(bannerInfo1);
        assertActiveBannerStatusesIsChanged(bannerInfo2);
    }

    @Test
    public void execute_OneKeywordIsUpdatedAndAnotherIsDuplicated_OneBannerIsChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveTemplateBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveTemplateBanner(adGroupInfo2);

        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_1),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, PHRASE_1),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));

        assertActiveBannerStatusesIsNotChanged(bannerInfo1);
        assertActiveBannerStatusesIsChanged(bannerInfo2);
    }

    @Test
    public void execute_OneKeywordIsUpdatedAndAnotherIsInvalid_OneBannerIsChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveTemplateBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveTemplateBanner(adGroupInfo2);

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, INVALID_PHRASE_1),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(null, isUpdated(keywordIdToUpdate2, PHRASE_3)));

        assertActiveBannerStatusesIsNotChanged(bannerInfo1);
        assertActiveBannerStatusesIsChanged(bannerInfo2);
    }

    private void assertActiveBannerStatusesIsNotChanged(TextBannerInfo bannerInfo) {
        OldTextBanner expectedBanner = new OldTextBanner()
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES);
        assertBanner(bannerInfo, expectedBanner);
        Long minusGeoBid = testBannerRepository
                .getMinusGeoBannerId(clientInfo.getShard(), bannerInfo.getBannerId(), BannersMinusGeoType.current);
        assertThat("запись в таблице banners_minus_geo с current должна остаться", minusGeoBid,
                notNullValue());
        assertBsSyncedBannerMinusGeo(bannerInfo.getBannerId());

        assertThat("запись в таблице auto_moderate должна остаться",
                testModerationRepository.getAutoModerateId(clientInfo.getShard(), bannerInfo.getBannerId()),
                notNullValue());
        assertThat("запись в таблице post_moderate должна остаться",
                testModerationRepository.getPostModerateId(clientInfo.getShard(), bannerInfo.getBannerId()),
                notNullValue());
    }

    private void assertActiveBannerStatusesIsChanged(TextBannerInfo bannerInfo) {
        OldTextBanner expectedBanner = new OldTextBanner()
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withStatusBsSynced(StatusBsSynced.NO);
        assertBanner(bannerInfo, expectedBanner);

        Long minusGeoBid =
                testBannerRepository
                        .getMinusGeoBannerId(clientInfo.getShard(), bannerInfo.getBannerId(), BannersMinusGeoType.current);
        assertThat("запись в таблице banners_minus_geo с current должна быть удалена", minusGeoBid,
                nullValue());
        assertBsSyncedBannerMinusGeo(bannerInfo.getBannerId());

        assertThat("запись в таблице auto_moderate должна быть удалена",
                testModerationRepository.getAutoModerateId(clientInfo.getShard(), bannerInfo.getBannerId()),
                nullValue());
        assertThat("запись в таблице post_moderate должна быть удалена",
                testModerationRepository.getPostModerateId(clientInfo.getShard(), bannerInfo.getBannerId()),
                nullValue());
    }

    private void assertBanner(TextBannerInfo bannerInfo, OldBanner expectedBanner) {
        OldBanner banner =
                bannerRepository.getBanners(bannerInfo.getShard(), singletonList(bannerInfo.getBannerId())).get(0);
        assertThat("состояние баннера не соответствует ождидаемому",
                banner, beanDiffer(expectedBanner).useCompareStrategy(COMPARE_STRATEGY));
    }

    private void assertBsSyncedBannerMinusGeo(Long bannerId) {
        Long minusGeoBid =
                testBannerRepository.getMinusGeoBannerId(clientInfo.getShard(), bannerId, BannersMinusGeoType.bs_synced);
        MatcherAssert.assertThat("запись в таблице banners_minus_geo с bs_synced должна остаться", minusGeoBid,
                notNullValue());
    }

    private TextBannerInfo createActiveTemplateBanner(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActiveWithTemplateTitle(defaultTextBanner(null, null));
        TextBannerInfo textBannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo);

        testBannerRepository
                .addMinusGeo(adGroupInfo.getShard(), textBannerInfo.getBannerId(), BannersMinusGeoType.current);
        testBannerRepository
                .addMinusGeo(adGroupInfo.getShard(), textBannerInfo.getBannerId(), BannersMinusGeoType.bs_synced);

        testModerationRepository.addPostModerate(adGroupInfo.getShard(), textBannerInfo.getBannerId());
        testModerationRepository.addAutoModerate(adGroupInfo.getShard(), textBannerInfo.getBannerId());

        return textBannerInfo;
    }

    private OldTextBanner makeBannerActiveWithTemplateTitle(OldTextBanner banner) {
        return banner.withTitle("конструкторы #лего#")
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES);
    }
}
