package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.banner.model.BannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.MobileAppBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCallouts;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCalloutsAction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCalloutsInstruction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCalloutsPayloadItem;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FindAndReplaceCalloutsServiceTest {

    private static final String REPLACE_CALLOUT_ARG_NAME = "input";

    @Autowired
    private Steps steps;

    @Autowired
    private FindAndReplaceCalloutsService service;

    @Autowired
    private BannerTypedRepository bannerRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ClientInfo clientInfo;
    private long operatorUid;
    private ClientId clientId;

    private GdFindAndReplaceCallouts validInput;

    private TextBannerInfo bannerForUpdate;
    private Callout firstCallout;
    private Callout secondCallout;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        firstCallout = steps.calloutSteps().createDefaultCallout(clientInfo);
        secondCallout = steps.calloutSteps().createDefaultCallout(clientInfo);

        bannerForUpdate = steps.bannerSteps().createBanner(activeTextBanner()
                        .withCalloutIds(List.of(firstCallout.getId())),
                clientInfo);

        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(secondCallout.getId())));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.RMP_CALLOUTS_ENABLED, false);
    }

    // preview

    @Test
    public void preview_AddSuccess_NoChange() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(firstCallout.getId())));

        GdCachedResult<GdFindAndReplaceCalloutsPayloadItem> preview = service.preview(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        var expected = new GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(0)
                .withSuccessCount(0)
                .withRowset(emptyList());
        assertThat(preview, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void preview_AddSuccess_WithChange() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(secondCallout.getId())));

        GdCachedResult<GdFindAndReplaceCalloutsPayloadItem> preview = service.preview(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(List.of(firstCallout.getId(), secondCallout.getId()))
                ));
        assertThat(preview, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void preview_RemoveByIds_Success() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.REMOVE_BY_IDS)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(List.of(firstCallout.getId()))
                        .withReplace(emptyList()));

        GdCachedResult<GdFindAndReplaceCalloutsPayloadItem> preview = service.preview(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(emptyList())
                ));
        assertThat(preview, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void preview_ReplaceByIds_Success() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.REPLACE_BY_IDS)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(List.of(firstCallout.getId()))
                        .withReplace(List.of(secondCallout.getId())));

        GdCachedResult<GdFindAndReplaceCalloutsPayloadItem> preview = service.preview(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(List.of(secondCallout.getId()))
                ));
        assertThat(preview, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void preview_InputValidationError() {
        validInput.withAction(null);

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_CALLOUT_ARG_NAME + "." + GdFindAndReplaceCallouts.ACTION.name(),
                        new Defect<>(DefectIds.CANNOT_BE_NULL)))));

        service.preview(validInput, operatorUid, clientId, REPLACE_CALLOUT_ARG_NAME);
    }

    @Test
    public void preview_ReplacementValidationError() {
        validInput.getReplaceInstruction().withReplace(List.of(12345L));

        var preview = service.preview(validInput, operatorUid, clientId, REPLACE_CALLOUT_ARG_NAME);

        assertThat(preview.getSuccessCount(), is(0));
        assertThat(preview.getValidationResult(), notNullValue());
    }

    // replace

    @Test
    public void replace_AddSuccess_NoChange() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(firstCallout.getId())));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(0)
                .withSuccessCount(0)
                .withRowset(emptyList());
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(List.of(firstCallout.getId())));
    }

    @Test
    public void replace_AddSuccess_WithChange() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(secondCallout.getId())));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(List.of(firstCallout.getId(), secondCallout.getId()))
                ));
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(List.of(firstCallout.getId(), secondCallout.getId())));
    }

    @Test
    public void replace_RemoveByIds_Success() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.REMOVE_BY_IDS)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(List.of(firstCallout.getId()))
                        .withReplace(emptyList()));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(emptyList())
                ));
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(emptyList()));
    }

    @Test
    public void replace_RemoveAll_Success() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.REMOVE_ALL)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(emptyList()));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(emptyList())
                ));
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(emptyList()));
    }

    @Test
    public void replace_ReplaceByIds_Success() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.REPLACE_BY_IDS)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(List.of(firstCallout.getId()))
                        .withReplace(List.of(secondCallout.getId())));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(List.of(secondCallout.getId()))
                ));
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(List.of(secondCallout.getId())));
    }

    @Test
    public void replace_ReplaceAll_Success() {
        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.REPLACE_ALL)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(secondCallout.getId(), firstCallout.getId())));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldTextBanner banner = bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(List.of(firstCallout.getId()))
                        .withNewCalloutIds(List.of(secondCallout.getId(), firstCallout.getId()))
                ));
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(List.of(secondCallout.getId(), firstCallout.getId())));
    }

    @Test
    public void replace_AddDynamicBanner_Success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        String imageHash = steps.bannerSteps().createRegularImageFormat(clientInfo).getImageHash();

        DynamicBannerInfo bannerForUpdate = steps.bannerSteps().createBanner(activeDynamicBanner()
                .withBannerImage(defaultBannerImage(null, imageHash)), adGroupInfo);

        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(firstCallout.getId())));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        OldDynamicBanner banner = (OldDynamicBanner) bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(emptyList())
                        .withNewCalloutIds(List.of(firstCallout.getId()))
                ));
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(List.of(firstCallout.getId())));
    }

    @Test
    public void replace_AddCalloutToMobileAppBannerWOFeature_Failed() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);

        MobileAppBannerInfo bannerForUpdate = steps.bannerSteps().createActiveMobileAppBanner(adGroupInfo);

        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(firstCallout.getId())));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        var banner = bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(0)
                .withSuccessCount(0)
                .withRowset(emptyList());
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(emptyList()));
    }

    @Test
    public void replace_AddCalloutToMobileAppBannerWFeature_Success() {

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);

        MobileAppBannerInfo bannerForUpdate = steps.bannerSteps().createActiveMobileAppBanner(adGroupInfo);

        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(firstCallout.getId())));

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.RMP_CALLOUTS_ENABLED, true);
        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.RMP_CALLOUTS_ENABLED, false);

        var banner = bannerForUpdate.getBanner();

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(1)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(banner.getId())
                        .withOldCalloutIds(emptyList())
                        .withNewCalloutIds(List.of(firstCallout.getId())))
                );
        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        BannerWithCallouts updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat(updatedBanner.getCalloutIds(), is(List.of(firstCallout.getId())));
    }

    private BannerWithCallouts getBanner(int shard, long bannerId) {
        return bannerRepository.getStrictly(shard, List.of(bannerId), BannerWithCallouts.class).get(0);
    }

    @Test
    public void replace_Add_IgnoreUnsupportedBanners() {
        BannerInfo bannerForUpdate =
                steps.bannerSteps().createBanner(
                        activeImageHashBanner(null, null), clientInfo);

        validInput = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerForUpdate.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(emptyList())
                        .withReplace(List.of(firstCallout.getId())));

        GdResult<GdFindAndReplaceCalloutsPayloadItem> replace = service.replace(validInput, operatorUid,
                clientId, REPLACE_CALLOUT_ARG_NAME);

        var expected = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(0)
                .withSuccessCount(0)
                .withRowset(emptyList());

        assertThat(replace, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void replace_InputValidationError() {
        validInput.getReplaceInstruction().withSearch(List.of(-1L));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_CALLOUT_ARG_NAME + "." +
                                GdFindAndReplaceCallouts.REPLACE_INSTRUCTION.name() + "." +
                                GdFindAndReplaceCalloutsInstruction.SEARCH.name() + "[0]",
                        new Defect<>(DefectIds.MUST_BE_VALID_ID)))));

        service.replace(validInput, operatorUid, clientId, REPLACE_CALLOUT_ARG_NAME);
    }

    @Test
    public void replace_ReplacementValidationError() {
        validInput.getReplaceInstruction().withReplace(List.of(12345L));

        var preview = service.replace(validInput, operatorUid, clientId, REPLACE_CALLOUT_ARG_NAME);

        assertThat(preview.getSuccessCount(), is(0));
        assertThat(preview.getValidationResult(), notNullValue());
    }
}
