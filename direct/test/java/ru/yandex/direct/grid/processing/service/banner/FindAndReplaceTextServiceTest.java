package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.OldAbstractBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingParams;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceBannerChangeItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceChangeItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceLinkMode;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceOptions;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceSitelinkChangeItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceText;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceTextInstruction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceTextPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdReplacementMode;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMcBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultMcBannerImageFormat;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.BODY;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.DISPLAY_HREF;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.HREF;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.SITELINK_HREF;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.SITELINK_TITLE;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.TITLE;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.TITLE_EXTENSION;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.TURBOLANDING_PARAMS;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FindAndReplaceTextServiceTest {

    private static final String REPLACE_TEXT_ARG_NAME = "input";
    private static final Set<GdFindAndReplaceAdsTargetType> ALL_TARGETS =
            Set.of(TITLE, TITLE_EXTENSION, BODY, SITELINK_TITLE, DISPLAY_HREF, TURBOLANDING_PARAMS);
    private static final String SEARCH = "Default";
    private static final String REPLACEMENT = "Rplcmnt";
    private static final Set<Long> FULL_HOUSE = Set.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L);

    @Autowired
    private Steps steps;

    @Autowired
    private FindAndReplaceTextService service;

    @Autowired
    private BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    private BannerTypedRepository bannerRepository;

    @Autowired
    private SitelinkSetRepository sitelinkSetRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ClientInfo clientInfo;
    private long operatorUid;
    private ClientId clientId;

    private GdFindAndReplaceText validInput;

    private TextBannerInfo bannerForUpdate;
    private SitelinkSetInfo sitelinkSetForUpdate;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        var bannerId = RandomNumberUtils.nextPositiveLong();

        sitelinkSetForUpdate = steps.sitelinkSetSteps().createSitelinkSet(new SitelinkSet()
                .withSitelinks(List.of(defaultSitelink().withTitle("Default 1"),
                        defaultSitelink2().withTitle("Default 2"))), clientInfo);
        bannerForUpdate = steps.bannerSteps().createBanner(activeTextBanner()
                        //.withId(bannerId)
                        .withTurboLandingParams(new OldBannerTurboLandingParams()
                                //.withBannerId(bannerId)
                                .withHrefParams("Default 1"))
                        .withTitle("Default title")
                        .withTitleExtension("Default title extension")
                        .withBody("Default body")
                        .withDisplayHref("Default-display-href")
                        .withSitelinksSetId(sitelinkSetForUpdate.getSitelinkSetId()),
                clientInfo);

        validInput = new GdFindAndReplaceText()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withTargetTypes(ALL_TARGETS)
                .withReplaceInstruction(new GdFindAndReplaceTextInstruction()
                        .withSearch(SEARCH)
                        .withReplace(REPLACEMENT)
                        .withOptions(new GdFindAndReplaceOptions()
                                .withCaseSensitive(true)
                                .withReplacementMode(GdReplacementMode.FIND_AND_REPLACE)
                                .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL)
                                .withSitelinkOrderNumsToUpdateDescription(emptySet())
                                .withSitelinkOrderNumsToUpdateHref(emptySet())
                                .withSitelinkOrderNumsToUpdateTitle(emptySet())));
    }

    @Test
    public void previewAllTargetsSuccess() {
        validInput.withReplaceInstruction(validInput.getReplaceInstruction()
                .withOptions(validInput.getReplaceInstruction().getOptions()
                        .withSitelinkOrderNumsToUpdateTitle(FULL_HOUSE)
                        .withSitelinkOrderNumsToUpdateHref(FULL_HOUSE)
                        .withSitelinkOrderNumsToUpdateDescription(FULL_HOUSE)));

        var preview = service.preview(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerForUpdate.getBanner();
        var sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        var sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);
        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE)
                        .withOldValue(banner.getTitle())
                        .withNewValue(banner.getTitle().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE_EXTENSION)
                        .withOldValue(banner.getTitleExtension())
                        .withNewValue(banner.getTitleExtension().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(BODY)
                        .withOldValue(banner.getBody())
                        .withNewValue(banner.getBody().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(DISPLAY_HREF)
                        .withOldValue(banner.getDisplayHref())
                        .withNewValue(banner.getDisplayHref().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TURBOLANDING_PARAMS)
                        .withOldValue(banner.getTurboLandingParams().getHrefParams())
                        .withNewValue(banner.getTurboLandingParams().getHrefParams().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceSitelinkChangeItem()
                        .withTarget(SITELINK_TITLE)
                        .withSitelinkId(sitelink1.getId())
                        .withOrderNum(sitelink1.getOrderNum())
                        .withOldValue(sitelink1.getTitle())
                        .withNewValue(sitelink1.getTitle().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceSitelinkChangeItem()
                        .withTarget(SITELINK_TITLE)
                        .withSitelinkId(sitelink2.getId())
                        .withOrderNum(sitelink2.getOrderNum())
                        .withOldValue(sitelink2.getTitle())
                        .withNewValue(sitelink2.getTitle().replace(SEARCH, REPLACEMENT))
        );
        var expected = new GdCachedResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(preview, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void previewTargets_McBanner_Success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash();
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);

        BannerInfo bannerInfo = steps.bannerSteps().createBanner(activeMcBanner(campaignId, adGroupId)
                .withHref("http://search.net")
                .withImage(image), adGroupInfo);

        validInput = new GdFindAndReplaceText()
                .withAdIds(List.of(bannerInfo.getBannerId()))
                .withTargetTypes(Set.of(GdFindAndReplaceAdsTargetType.values()))
                .withReplaceInstruction(new GdFindAndReplaceTextInstruction()
                        .withSearch("search")
                        .withReplace("replace")
                        .withOptions(new GdFindAndReplaceOptions()
                                .withCaseSensitive(true)
                                .withReplacementMode(GdReplacementMode.FIND_AND_REPLACE)
                                .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL)
                                .withSitelinkOrderNumsToUpdateDescription(emptySet())
                                .withSitelinkOrderNumsToUpdateHref(emptySet())
                                .withSitelinkOrderNumsToUpdateTitle(emptySet())));

        var preview = service.preview(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(HREF)
                        .withOldValue("http://search.net")
                        .withNewValue("http://replace.net")
        );
        var expected = new GdCachedResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(bannerInfo.getBannerId())
                        .withChanges(expectedChanges)
                ));
        assertThat(preview, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void previewSingleTargetSuccess() {
        validInput.withTargetTypes(Set.of(TITLE));
        var preview = service.preview(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);
        var banner = bannerForUpdate.getBanner();

        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE)
                        .withOldValue(banner.getTitle())
                        .withNewValue(banner.getTitle().replace(SEARCH, REPLACEMENT))
        );
        var expected = new GdCachedResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(preview, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void previewInputValidationError() {
        validInput.withTargetTypes(null); // It's invalid now!

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_TEXT_ARG_NAME + "." + GdFindAndReplaceText.TARGET_TYPES.name(),
                        new Defect<>(DefectIds.CANNOT_BE_NULL)))));

        service.preview(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);
    }

    @Test
    public void previewReplacementValidationError() {
        validInput.getReplaceInstruction().withReplace("This replacement is too long at least for sitelink titles");

        var preview = service.preview(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        assertThat(preview.getSuccessCount(), is(0));
        assertThat(preview.getValidationResult(), notNullValue());
    }

    @Test
    public void replaceAllTargetsSuccess() {
        validInput.withReplaceInstruction(validInput.getReplaceInstruction()
                .withOptions(validInput.getReplaceInstruction().getOptions()
                        .withSitelinkOrderNumsToUpdateTitle(FULL_HOUSE)
                        .withSitelinkOrderNumsToUpdateHref(FULL_HOUSE)
                        .withSitelinkOrderNumsToUpdateDescription(FULL_HOUSE)));

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerForUpdate.getBanner();
        var newTitle = banner.getTitle().replace(SEARCH, REPLACEMENT);
        var newTitleExtension = banner.getTitleExtension().replace(SEARCH, REPLACEMENT);
        var newBody = banner.getBody().replace(SEARCH, REPLACEMENT);
        var newDisplayHref = banner.getDisplayHref().replace(SEARCH, REPLACEMENT);
        var sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        var sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);
        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE)
                        .withOldValue(banner.getTitle())
                        .withNewValue(newTitle),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE_EXTENSION)
                        .withOldValue(banner.getTitleExtension())
                        .withNewValue(newTitleExtension),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(BODY)
                        .withOldValue(banner.getBody())
                        .withNewValue(newBody),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(DISPLAY_HREF)
                        .withOldValue(banner.getDisplayHref())
                        .withNewValue(newDisplayHref),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TURBOLANDING_PARAMS)
                        .withOldValue(banner.getTurboLandingParams().getHrefParams())
                        .withNewValue(banner.getTurboLandingParams().getHrefParams().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceSitelinkChangeItem()
                        .withTarget(SITELINK_TITLE)
                        .withSitelinkId(sitelink1.getId())
                        .withOrderNum(sitelink1.getOrderNum())
                        .withOldValue(sitelink1.getTitle())
                        .withNewValue(sitelink1.getTitle().replace(SEARCH, REPLACEMENT)),
                new GdFindAndReplaceSitelinkChangeItem()
                        .withTarget(SITELINK_TITLE)
                        .withSitelinkId(sitelink2.getId())
                        .withOrderNum(sitelink2.getOrderNum())
                        .withOldValue(sitelink2.getTitle())
                        .withNewValue(sitelink2.getTitle().replace(SEARCH, REPLACEMENT))
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        assertThat(updatedBanner.getTitle(), is(newTitle));
        assertThat(updatedBanner.getTitleExtension(), is(newTitleExtension));
        assertThat(updatedBanner.getBody(), is(newBody));
        assertThat(updatedBanner.getDisplayHref(), is(newDisplayHref));
    }

    @Test
    public void clearAllOptionalTargetsSuccess() {
        validInput
                .withTargetTypes(Set.of(TITLE_EXTENSION, DISPLAY_HREF, TURBOLANDING_PARAMS))
                .withReplaceInstruction(new GdFindAndReplaceTextInstruction()
                        .withSearch(null)
                        .withReplace(null)
                        .withOptions(new GdFindAndReplaceOptions()
                                .withCaseSensitive(false)
                                .withReplacementMode(GdReplacementMode.DELETE)
                                .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL)
                                .withSitelinkOrderNumsToUpdateDescription(FULL_HOUSE)
                                .withSitelinkOrderNumsToUpdateHref(FULL_HOUSE)
                                .withSitelinkOrderNumsToUpdateTitle(FULL_HOUSE)));

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerForUpdate.getBanner();
        var sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        var sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);
        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE_EXTENSION)
                        .withOldValue(banner.getTitleExtension())
                        .withNewValue(null),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(DISPLAY_HREF)
                        .withOldValue(banner.getDisplayHref())
                        .withNewValue(null),
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TURBOLANDING_PARAMS)
                        .withOldValue(banner.getTurboLandingParams().getHrefParams())
                        .withNewValue("")
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        assertThat(updatedBanner.getTitleExtension(), is(nullValue()));
        assertThat(updatedBanner.getDisplayHref(), is(nullValue()));
    }

    @Test
    public void replaceSingleTargetSuccess() {
        validInput.withTargetTypes(Set.of(TITLE));
        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);
        var banner = bannerForUpdate.getBanner();
        var newTitle = banner.getTitle().replace(SEARCH, REPLACEMENT);

        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE)
                        .withOldValue(banner.getTitle())
                        .withNewValue(banner.getTitle().replace(SEARCH, REPLACEMENT))
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        assertThat(updatedBanner.getTitle(), is(newTitle));
    }

    @Test
    public void replaceInputValidationError() {
        validInput.withTargetTypes(null); // It's invalid now!

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_TEXT_ARG_NAME + "." + GdFindAndReplaceText.TARGET_TYPES.name(),
                        new Defect<>(DefectIds.CANNOT_BE_NULL)))));

        service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);
    }

    @Test
    public void replaceCaseInsensitive() {
        validInput.withTargetTypes(Set.of(TITLE));
        validInput.getReplaceInstruction().withSearch("DEFAULT");
        validInput.getReplaceInstruction().getOptions().withCaseSensitive(false);

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);
        var banner = bannerForUpdate.getBanner();
        var newTitle = banner.getTitle().replace(SEARCH, REPLACEMENT);

        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE)
                        .withOldValue(banner.getTitle())
                        .withNewValue(banner.getTitle().replace(SEARCH, REPLACEMENT))
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        assertThat(updatedBanner.getTitle(), is(newTitle));
    }

    @Test
    public void replaceReplacementValidationError() {
        validInput.getReplaceInstruction().withReplace("This replacement is too long at least for sitelink titles");

        var result = service.preview(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        assertThat(result.getSuccessCount(), is(0));
        assertThat(result.getValidationResult(), notNullValue());
    }

    @Test
    public void replaceSitelinkTitle() {
        validInput.withTargetTypes(Set.of(SITELINK_TITLE))
                .withReplaceInstruction(validInput.getReplaceInstruction()
                        .withOptions(validInput.getReplaceInstruction().getOptions()
                                .withSitelinkOrderNumsToUpdateTitle(FULL_HOUSE)));

        var banner = bannerForUpdate.getBanner();
        var sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        var sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);

        validInput.getReplaceInstruction().setSearch(sitelink1.getTitle());
        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceSitelinkChangeItem()
                        .withTarget(SITELINK_TITLE)
                        .withSitelinkId(sitelink1.getId())
                        .withOrderNum(sitelink1.getOrderNum())
                        .withOldValue(sitelink1.getTitle())
                        .withNewValue(REPLACEMENT)
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var bannerIdsToSitelinkSetIds = bannerRelationsRepository.getBannerIdsToSitelinkSetIds(clientInfo.getShard(),
                Set.of(banner.getId()));
        var sitelinkSetId = bannerIdsToSitelinkSetIds.get(banner.getId());
        var updatedSitelinks = sitelinkSetRepository.getSitelinksBySetIds(clientInfo.getShard(), Set.of(sitelinkSetId));

        assertThat(updatedSitelinks.get(sitelinkSetId), hasSize(2));

        var expectedSitelinks = List.of(
                new Sitelink()
                        .withTitle(REPLACEMENT)
                        .withDescription(sitelink1.getDescription())
                        .withHref(sitelink1.getHref())
                        .withOrderNum(sitelink1.getOrderNum()),
                new Sitelink()
                        .withTitle(sitelink2.getTitle())
                        .withDescription(sitelink2.getDescription())
                        .withHref(sitelink2.getHref())
                        .withOrderNum(sitelink2.getOrderNum())
        );
        assertThat(updatedSitelinks.get(sitelinkSetId),
                beanDiffer(expectedSitelinks).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void removeSitelinkTitleAndHref() {
        validInput.withTargetTypes(Set.of(SITELINK_TITLE, SITELINK_HREF))
                .withReplaceInstruction(validInput.getReplaceInstruction()
                        .withOptions(validInput.getReplaceInstruction()
                                .withSearch(null)
                                .withReplace(null)
                                .getOptions()
                                .withReplacementMode(GdReplacementMode.REPLACE_ALL)
                                .withSitelinkOrderNumsToUpdateTitle(FULL_HOUSE)
                                .withSitelinkOrderNumsToUpdateHref(FULL_HOUSE)));

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        assertThat(result.getSuccessCount(), is(0));
        assertThat(result.getValidationResult(), notNullValue());
    }

    @Test
    public void replaceSpecificSitelinkTitle() {
        validInput.withTargetTypes(Set.of(SITELINK_TITLE));
        validInput.getReplaceInstruction().getOptions().setSitelinkOrderNumsToUpdateTitle(Set.of(0L));

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerForUpdate.getBanner();
        var sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        var sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);
        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceSitelinkChangeItem()
                        .withTarget(SITELINK_TITLE)
                        .withSitelinkId(sitelink1.getId())
                        .withOrderNum(sitelink1.getOrderNum())
                        .withOldValue(sitelink1.getTitle())
                        .withNewValue(sitelink1.getTitle().replace(SEARCH, REPLACEMENT))
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var bannerIdsToSitelinkSetIds = bannerRelationsRepository.getBannerIdsToSitelinkSetIds(clientInfo.getShard(),
                Set.of(banner.getId()));

        var sitelinkSetId = bannerIdsToSitelinkSetIds.get(banner.getId());
        var updatedSitelinks = sitelinkSetRepository.getSitelinksBySetIds(clientInfo.getShard(), Set.of(sitelinkSetId));

        assertThat(updatedSitelinks.get(sitelinkSetId), hasSize(2));

        var expectedSitelinks = List.of(
                new Sitelink()
                        .withTitle(sitelink1.getTitle().replace(SEARCH, REPLACEMENT))
                        .withDescription(sitelink1.getDescription())
                        .withHref(sitelink1.getHref())
                        .withOrderNum(sitelink1.getOrderNum()),
                new Sitelink()
                        .withTitle(sitelink2.getTitle())
                        .withDescription(sitelink2.getDescription())
                        .withHref(sitelink2.getHref())
                        .withOrderNum(sitelink2.getOrderNum())
        );
        assertThat(updatedSitelinks.get(sitelinkSetId),
                beanDiffer(expectedSitelinks).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void removeValue() {
        TextBannerInfo secondBanner = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitleExtension("Custom title extension"), clientInfo);

        List<OldTextBanner> banners = List.of(bannerForUpdate.getBanner(), secondBanner.getBanner());

        validInput.setAdIds(mapList(banners, OldTextBanner::getId));
        validInput.setTargetTypes(Set.of(TITLE_EXTENSION));
        validInput.getReplaceInstruction().setReplace(null);
        validInput.getReplaceInstruction().getOptions().setReplacementMode(GdReplacementMode.DELETE);

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE_EXTENSION)
                        .withOldValue(banners.get(0).getTitleExtension())
                        .withNewValue(null)
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(2)
                .withSuccessCount(2)
                .withRowset(List.of(
                        new GdFindAndReplaceTextPayloadItem()
                                .withAdId(banners.get(0).getId())
                                .withChanges(expectedChanges),
                        new GdFindAndReplaceTextPayloadItem()
                                .withAdId(banners.get(1).getId())
                                .withChanges(emptyList())
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void fullReplaceValue() {
        validInput.setTargetTypes(Set.of(TITLE));
        validInput.getReplaceInstruction().setSearch(null);
        validInput.getReplaceInstruction().getOptions().setReplacementMode(GdReplacementMode.REPLACE_ALL);

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerForUpdate.getBanner();
        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE)
                        .withOldValue(banner.getTitle())
                        .withNewValue(REPLACEMENT)
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void setMissingHref() {
        var bannerInfo = steps.bannerSteps().createBanner(activeTextBanner().withHref(null), clientInfo);
        var newHref = "https://yandex.ru/";

        validInput
                .withAdIds(List.of(bannerInfo.getBannerId()))
                .withTargetTypes(Set.of(HREF));
        validInput.getReplaceInstruction().setSearch(null);
        validInput.getReplaceInstruction().setReplace(newHref);
        validInput.getReplaceInstruction().getOptions().setReplacementMode(GdReplacementMode.REPLACE_ALL);

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerInfo.getBanner();
        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(HREF)
                        .withOldValue(null)
                        .withNewValue(newHref)
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void findAndReplaceMissingTitleExtension_DontChange() {
        var bannerInfo = steps.bannerSteps().createBanner(activeTextBanner().withTitleExtension(null), clientInfo);
        var newTitleExtension = "Brand new title extension";

        validInput
                .withAdIds(List.of(bannerInfo.getBannerId()))
                .withTargetTypes(Set.of(TITLE_EXTENSION));
        validInput.getReplaceInstruction().setSearch(SEARCH);
        validInput.getReplaceInstruction().setReplace(newTitleExtension);
        validInput.getReplaceInstruction().getOptions().setReplacementMode(GdReplacementMode.FIND_AND_REPLACE);

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerInfo.getBanner();
        List<GdFindAndReplaceChangeItem> expectedChanges = emptyList();
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void setMissingTitleExtension() {
        var bannerInfo = steps.bannerSteps().createBanner(activeTextBanner().withTitleExtension(null), clientInfo);
        var newTitleExtension = "Wow! New title extension!";

        validInput
                .withAdIds(List.of(bannerInfo.getBannerId()))
                .withTargetTypes(Set.of(TITLE_EXTENSION));
        validInput.getReplaceInstruction().setSearch(null);
        validInput.getReplaceInstruction().setReplace(newTitleExtension);
        validInput.getReplaceInstruction().getOptions().setReplacementMode(GdReplacementMode.REPLACE_ALL);

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var banner = bannerInfo.getBanner();
        List<GdFindAndReplaceChangeItem> expectedChanges = List.of(
                new GdFindAndReplaceBannerChangeItem()
                        .withTarget(TITLE_EXTENSION)
                        .withOldValue(null)
                        .withNewValue(newTitleExtension)
        );
        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceTextPayloadItem()
                        .withAdId(banner.getId())
                        .withChanges(expectedChanges)
                ));
        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void appendToAllBannersTitle() {
        TextBannerInfo secondBannerForUpdate = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("Default title 2"), clientInfo);

        List<OldTextBanner> banners = List.of(bannerForUpdate.getBanner(), secondBannerForUpdate.getBanner());

        validInput
                .withAdIds(List.of(banners.get(0).getId(), banners.get(1).getId()))
                .withTargetTypes(Set.of(TITLE))
                .withReplaceInstruction(new GdFindAndReplaceTextInstruction()
                        .withSearch(null)
                        .withReplace("END")
                        .withOptions(new GdFindAndReplaceOptions()
                                .withCaseSensitive(true)
                                .withReplacementMode(GdReplacementMode.APPEND)
                                .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL)));

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var newBannerTitle = banners.get(0).getTitle() + "END";
        var newSecondBannerTitle = banners.get(1).getTitle() + "END";

        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(2)
                .withSuccessCount(2)
                .withRowset(List.of(
                        new GdFindAndReplaceTextPayloadItem()
                                .withAdId(banners.get(0).getId())
                                .withChanges(List.of(new GdFindAndReplaceBannerChangeItem()
                                        .withTarget(TITLE)
                                        .withOldValue(banners.get(0).getTitle())
                                        .withNewValue(newBannerTitle)
                                )),
                        new GdFindAndReplaceTextPayloadItem()
                                .withAdId(banners.get(1).getId())
                                .withChanges(List.of(new GdFindAndReplaceBannerChangeItem()
                                        .withTarget(TITLE)
                                        .withOldValue(banners.get(1).getTitle())
                                        .withNewValue(newSecondBannerTitle)
                                ))
                ));

        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var updatedBanners = getBanners(clientInfo.getShard(), mapList(banners, OldAbstractBanner::getId));

        var updatedBanner = updatedBanners.get(0);
        var secondUpdatedBanner = updatedBanners.get(1);

        assertThat(updatedBanner.getTitle(), is(newBannerTitle));
        assertThat(secondUpdatedBanner.getTitle(), is(newSecondBannerTitle));
    }

    @Test
    public void appendToMatchingBannersTitle() {
        TextBannerInfo secondBannerForUpdate = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("Default title 2"), clientInfo);

        List<OldTextBanner> banners = List.of(bannerForUpdate.getBanner(), secondBannerForUpdate.getBanner());

        validInput
                .withAdIds(List.of(banners.get(0).getId(), banners.get(1).getId()))
                .withTargetTypes(Set.of(TITLE))
                .withReplaceInstruction(new GdFindAndReplaceTextInstruction()
                        .withSearch("title 2")
                        .withReplace("END")
                        .withOptions(new GdFindAndReplaceOptions()
                                .withCaseSensitive(true)
                                .withReplacementMode(GdReplacementMode.APPEND)
                                .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL)
                                .withSitelinkOrderNumsToUpdateDescription(emptySet())
                                .withSitelinkOrderNumsToUpdateHref(emptySet())
                                .withSitelinkOrderNumsToUpdateTitle(emptySet())));

        var result = service.replace(validInput, operatorUid, clientId, REPLACE_TEXT_ARG_NAME);

        var newSecondBannerTitle = banners.get(1).getTitle() + "END";

        var expected = new GdResult<GdFindAndReplaceTextPayloadItem>()
                .withTotalCount(2)
                .withSuccessCount(2)
                .withRowset(List.of(
                        new GdFindAndReplaceTextPayloadItem()
                                .withAdId(banners.get(0).getId())
                                .withChanges(emptyList()),
                        new GdFindAndReplaceTextPayloadItem()
                                .withAdId(banners.get(1).getId())
                                .withChanges(List.of(new GdFindAndReplaceBannerChangeItem()
                                        .withTarget(TITLE)
                                        .withOldValue(banners.get(1).getTitle())
                                        .withNewValue(newSecondBannerTitle)
                                ))
                ));

        assertThat(result, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        var updatedBanners = getBanners(clientInfo.getShard(), mapList(banners, OldAbstractBanner::getId));

        var updatedBanner = updatedBanners.get(0);
        var secondUpdatedBanner = updatedBanners.get(1);

        assertThat(updatedBanner.getTitle(), is(banners.get(0).getTitle()));
        assertThat(secondUpdatedBanner.getTitle(), is(newSecondBannerTitle));
    }

    private String createBannerImageHash() {
        return steps.bannerSteps()
                .createBannerImageFormat(clientInfo, defaultMcBannerImageFormat(null))
                .getImageHash();
    }

    private TextBanner getBanner(int shard, long bannerId) {
        return getBanners(shard, singletonList(bannerId)).get(0);
    }

    private List<TextBanner> getBanners(int shard, Collection<Long> bannerIds) {
        return bannerRepository.getStrictly(shard, bannerIds, TextBanner.class);
    }
}
