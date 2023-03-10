package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

/**
 * ?????????????????? ???????????????? ?????????????????? ?????????????? ?? ?????????????????? ?????? ????????????????????
 * ?? ?????????????????????? ???? ?????????????? ???????????????? ?????????????????????? ?? ????????????
 * ???????????????????????????????? ???????????? ??????????????????????. ?????????? ???? ?? ?????????????????? ????????
 * ???????????????? ??????????????????????, ?????? ?????????????????? ???????????????????????? ???????????? ??????????????????
 * ????????????????, ?????????????? ???????????????????? ???????????????? ?????????????? ??????????????????.
 * <p>
 * ?????? ???? ?????????? ?????????????????????? ???????????????? ???? ???????????? auto_moderate, post_moderate, minus_geo.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationAdditionalTasksForBannersTest extends KeywordsAddOperationBaseTest {

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    // ?????? ???????????????? ?????? ?????? ???? ??????????????????

    @Test
    public void execute_NoBanners_WorksFine() {
        createOneActiveAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
    }

    @Test
    public void execute_ActiveNonTemplateBanner_BannerNotChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveNonTemplateBanner(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo);
    }

    @Test
    public void execute_ActiveNonTemplateBannerAndActiveTemplateBanner_OnlyTemplateBannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo1 = createActiveNonTemplateBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveBannerWithTemplateTitle(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo1);
        assertActiveBannerStatusesChanged(bannerInfo2);
    }

    @Test
    public void execute_ActiveHalfTemplateBanner_BannerNotChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveHalfTemplateBanner(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo);
    }

    // ???????????? ?????????????????????????? ?????? ???????????????? ????????????????????

    @Test
    public void execute_TemplateDraftBanner_BannerNotChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createDraftBannerWithTemplateTitle(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertDraftBannerStatusesNotChanged(bannerInfo);
    }

    @Test
    public void execute_TemplateArchivedAndActiveBanners_OnlyActiveBannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo1 = createArchivedBannerWithTemplateTitle(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveBannerWithTemplateTitle(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo1);
        assertActiveBannerStatusesChanged(bannerInfo2);
    }

    // ???????????????? ?????????? ???? ?????????? ???? ?????????????????????? ????-???? ???????????????????????????? ?????? ????????????????????????

    @Test
    public void execute_ActiveBannerButInvalidKeyword_BannerNotChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateTitle(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveBannerWithTemplateTitle(adGroupInfo2);

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));

        assertActiveBannerStatusesChanged(bannerInfo1);
        assertActiveBannerStatusesNotChanged(bannerInfo2);
    }

    @Test
    public void execute_ActiveBannerAndOneOfKeywordsInvalid_BannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateTitle(adGroupInfo1);

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));

        assertActiveBannerStatusesChanged(bannerInfo1);
    }

    @Test
    public void execute_ActiveBannerButDuplicatedKeyword_BannerNotChanged() {
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo2, PHRASE_1);
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateTitle(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveBannerWithTemplateTitle(adGroupInfo2);

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo1);
        assertActiveBannerStatusesNotChanged(bannerInfo2);
    }

    @Test
    public void execute_ActiveBannerButOneOfKeywordsDuplicated_BannerNotChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateTitle(adGroupInfo1);

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo1);
    }

    // ?????????????? ?? ???????????? ??????????????

    @Test
    public void execute_TwoTemplateBannersInDifferentAdGroups_BannersChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateBody(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveBannerWithTemplateTitle(adGroupInfo2);

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertActiveBannerStatusesChanged(bannerInfo1);
        assertActiveBannerStatusesChanged(bannerInfo2);
    }

    @Test
    public void execute_OneAdGroupWithTemplateBannerAndOneWithNot_OneBannerChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateBody(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createDraftBannerWithTemplateTitle(adGroupInfo2);

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertActiveBannerStatusesChanged(bannerInfo1);
        assertDraftBannerStatusesNotChanged(bannerInfo2);
    }

    @Test
    public void execute_OneAdGroupWithTemplateBannerAndOneWithoutBanner_OneBannerChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateBody(adGroupInfo1);

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertActiveBannerStatusesChanged(bannerInfo1);
    }

    @Test
    public void execute_TwoTemplateBanners_BannersChanged() {
        createTwoActiveAdGroups();
        TextBannerInfo bannerInfo1 = createActiveBannerWithTemplateBody(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createActiveBannerWithTemplateTitle(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo1);
        assertActiveBannerStatusesChanged(bannerInfo2);
    }

    // ?????????????????? ????????????????

    // ???????? ???????? ?????????? ?????? ?????????????????????? ?????????????????????? ??????????????

    @Test
    public void execute_ActiveBannerWithTemplateTitle_BannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveBannerWithTemplateTitle(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo);
    }

    @Test
    public void execute_ActiveBannerWithTemplateTitleExtension_BannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveBannerWithTemplateTitleExtension(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo);
    }

    @Test
    public void execute_ActiveBannerWithTemplateBody_BannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveBannerWithTemplateBody(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo);
    }

    @Test
    public void execute_ActiveBannerWithTemplateHref_BannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveBannerWithTemplateHref(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo);
    }

    @Test
    public void execute_ActiveBannerWithTemplateDisplayHref_BannerChanged() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveBannerWithTemplateDisplayHref(adGroupInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo);
    }

    // ???????????????? ?????????????? ???? ???????????? banners_minus_geo

    @Test
    public void execute_ActiveTemplateBannerWithCurrentMinusGeo_CurrentMinusGeoDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveBannerWithTemplateTitle(adGroupInfo1);
        addMinusGeo(bannerInfo);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo);
        assertCurrentBannerMinusGeoDeleted(bannerInfo);
        assertBsSyncedBannerMinusGeoNotDeleted(bannerInfo);
    }

    @Test
    public void execute_ActiveNonTemplateBannerWithCurrentMinusGeo_CurrentMinusGeoNotDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveNonTemplateBanner(adGroupInfo1);
        addMinusGeo(bannerInfo);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo);
        assertCurrentBannerMinusGeoNotDeleted(bannerInfo);
        assertBsSyncedBannerMinusGeoNotDeleted(bannerInfo);
    }

    @Test
    public void execute_DraftTemplateBannerWithCurrentMinusGeo_CurrentMinusGeoDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createDraftBannerWithTemplateTitle(adGroupInfo1);
        addMinusGeo(bannerInfo);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertDraftBannerStatusesNotChanged(bannerInfo);
        assertCurrentBannerMinusGeoDeleted(bannerInfo);
        assertBsSyncedBannerMinusGeoNotDeleted(bannerInfo);
    }

    @Test
    public void execute_ArchivedTemplateBannerWithCurrentMinusGeo_CurrentMinusGeoDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo1 = createArchivedBannerWithTemplateTitle(adGroupInfo1);
        createActiveBannerWithTemplateTitle(adGroupInfo1);
        addMinusGeo(bannerInfo1);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo1);
        assertCurrentBannerMinusGeoDeleted(bannerInfo1);
        assertBsSyncedBannerMinusGeoNotDeleted(bannerInfo1);
    }

    // ???????????????? ?????????????? ???? ???????????? auto_moderate, post_moderate

    @Test
    public void execute_ActiveTemplateBannerWithRecordsInAutoAndPostModerate_RecordsDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveBannerWithTemplateTitle(adGroupInfo1);
        testModerationRepository.addAutoModerate(bannerInfo.getShard(), bannerInfo.getBannerId());
        testModerationRepository.addPostModerate(bannerInfo.getShard(), bannerInfo.getBannerId());

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesChanged(bannerInfo);
        assertPostAndAutoModerateRecordsDeleted(bannerInfo);
    }

    @Test
    public void execute_ActiveNonTemplateBannerWithRecordsInAutoAndPostModerate_RecordsNotDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createActiveNonTemplateBanner(adGroupInfo1);
        testModerationRepository.addAutoModerate(bannerInfo.getShard(), bannerInfo.getBannerId());
        testModerationRepository.addPostModerate(bannerInfo.getShard(), bannerInfo.getBannerId());

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo);
        assertPostAndAutoModerateRecordsNotDeleted(bannerInfo);
    }

    @Test
    public void execute_ArchivedTemplateBannerWithRecordsInAutoAndPostModerate_RecordsDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo1 = createArchivedBannerWithTemplateTitle(adGroupInfo1);
        createActiveBannerWithTemplateTitle(adGroupInfo1);
        testModerationRepository.addAutoModerate(bannerInfo1.getShard(), bannerInfo1.getBannerId());
        testModerationRepository.addPostModerate(bannerInfo1.getShard(), bannerInfo1.getBannerId());

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveBannerStatusesNotChanged(bannerInfo1);
        assertPostAndAutoModerateRecordsDeleted(bannerInfo1);
    }

    @Test
    public void execute_DraftTemplateBannerWithRecordsInAutoAndPostModerate_RecordsDeleted() {
        createOneActiveAdGroup();
        TextBannerInfo bannerInfo = createDraftBannerWithTemplateTitle(adGroupInfo1);
        testModerationRepository.addAutoModerate(bannerInfo.getShard(), bannerInfo.getBannerId());
        testModerationRepository.addPostModerate(bannerInfo.getShard(), bannerInfo.getBannerId());

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertDraftBannerStatusesNotChanged(bannerInfo);
        assertPostAndAutoModerateRecordsDeleted(bannerInfo);
    }

    private void assertPostAndAutoModerateRecordsDeleted(AbstractBannerInfo bannerInfo) {
        Long postModerateId = testModerationRepository
                .getPostModerateId(bannerInfo.getShard(), bannerInfo.getBannerId());
        Long autoModerateId = testModerationRepository
                .getAutoModerateId(bannerInfo.getShard(), bannerInfo.getBannerId());
        assertThat(postModerateId, nullValue());
        assertThat(autoModerateId, nullValue());
    }

    private void assertPostAndAutoModerateRecordsNotDeleted(AbstractBannerInfo bannerInfo) {
        Long postModerateId = testModerationRepository
                .getPostModerateId(bannerInfo.getShard(), bannerInfo.getBannerId());
        Long autoModerateId = testModerationRepository
                .getAutoModerateId(bannerInfo.getShard(), bannerInfo.getBannerId());
        assertThat(postModerateId, notNullValue());
        assertThat(autoModerateId, notNullValue());
    }

    // ???????????????? ????????????????

    private TextBannerInfo createActiveNonTemplateBanner(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActive(defaultTextBanner(null, null))
                .withTitle("???????????? ??????????????????");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createActiveHalfTemplateBanner(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActive(defaultTextBanner(null, null))
                .withTitle("?????????? #????????????");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createDraftBannerWithTemplateTitle(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerDraft(defaultTextBanner(null, null))
                .withTitle("???????????????????????? #????????#");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createArchivedBannerWithTemplateTitle(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerArchived(defaultTextBanner(null, null))
                .withTitle("???????????????????????? #????????#");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createActiveBannerWithTemplateTitle(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActive(defaultTextBanner(null, null))
                .withTitle("???????????????????????? #????????#");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createActiveBannerWithTemplateTitleExtension(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActive(defaultTextBanner(null, null))
                .withTitleExtension("???????????????????????? #????????#");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createActiveBannerWithTemplateBody(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActive(defaultTextBanner(null, null))
                .withBody("???????????? ???????????????????????? #????????#");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createActiveBannerWithTemplateHref(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActive(defaultTextBanner(null, null))
                .withHref("https://yandex.ru?phrase=#undefined#");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private TextBannerInfo createActiveBannerWithTemplateDisplayHref(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = makeBannerActive(defaultTextBanner(null, null))
                .withDisplayHref("yandex.ru/#undef#");
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private void addMinusGeo(TextBannerInfo bannerInfo) {
        Long bannerId = bannerInfo.getBannerId();
        testBannerRepository.addMinusGeo(clientInfo.getShard(), bannerId, BannersMinusGeoType.current);
        testBannerRepository.addMinusGeo(clientInfo.getShard(), bannerId, BannersMinusGeoType.bs_synced);
    }

    // ??????????????

    private void assertDraftBannerStatusesNotChanged(AbstractBannerInfo bannerInfo) {
        OldTextBanner expectedBanner = makeBannerDraft(new OldTextBanner());
        assertBanner(bannerInfo, expectedBanner);
    }

    private void assertActiveBannerStatusesNotChanged(AbstractBannerInfo bannerInfo) {
        OldTextBanner expectedBanner = makeBannerActive(new OldTextBanner());
        assertBanner(bannerInfo, expectedBanner);
    }

    private void assertActiveBannerStatusesChanged(AbstractBannerInfo bannerInfo) {
        OldTextBanner expectedBanner = new OldTextBanner()
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withStatusPostModerate(OldBannerStatusPostModerate.NO)
                .withStatusBsSynced(StatusBsSynced.NO);
        assertBanner(bannerInfo, expectedBanner);
    }

    private void assertBanner(AbstractBannerInfo bannerInfo, OldBanner expectedBanner) {
        OldBanner banner = bannerRepository
                .getBanners(bannerInfo.getShard(), singletonList(bannerInfo.getBannerId())).get(0);
        assertThat("?????????????????? ?????????????? ???? ?????????????????????????? ??????????????????????",
                banner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    private void assertCurrentBannerMinusGeoDeleted(AbstractBannerInfo bannerInfo) {
        Long bannerId = bannerInfo.getBannerId();
        Long minusGeoBid = testBannerRepository
                .getMinusGeoBannerId(clientInfo.getShard(), bannerId, BannersMinusGeoType.current);
        assertThat("???????????? ?? ?????????????? banners_minus_geo ?? current ???????????? ???????? ??????????????",
                minusGeoBid, nullValue());
    }

    private void assertCurrentBannerMinusGeoNotDeleted(AbstractBannerInfo bannerInfo) {
        Long bannerId = bannerInfo.getBannerId();
        Long minusGeoBid = testBannerRepository
                .getMinusGeoBannerId(clientInfo.getShard(), bannerId, BannersMinusGeoType.current);
        assertThat("???????????? ?? ?????????????? banners_minus_geo ?? current ???????????? ????????????????",
                minusGeoBid, notNullValue());
    }

    private void assertBsSyncedBannerMinusGeoNotDeleted(AbstractBannerInfo bannerInfo) {
        Long bannerId = bannerInfo.getBannerId();
        Long minusGeoBid = testBannerRepository
                .getMinusGeoBannerId(clientInfo.getShard(), bannerId, BannersMinusGeoType.bs_synced);
        MatcherAssert.assertThat("???????????? ?? ?????????????? banners_minus_geo ?? bs_synced ???????????? ????????????????",
                minusGeoBid, notNullValue());
    }

    private OldTextBanner makeBannerActive(OldTextBanner banner) {
        return banner.withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusPostModerate(OldBannerStatusPostModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES);
    }

    private OldTextBanner makeBannerArchived(OldTextBanner banner) {
        return banner.withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusPostModerate(OldBannerStatusPostModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusArchived(true);
    }

    private OldTextBanner makeBannerDraft(OldTextBanner banner) {
        return banner.withStatusModerate(OldBannerStatusModerate.NEW)
                .withStatusPostModerate(OldBannerStatusPostModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES);
    }
}
