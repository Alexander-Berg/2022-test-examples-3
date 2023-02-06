package ru.yandex.direct.grid.core.entity.banner.service.internal;

import java.util.List;

import junitparams.converters.Nullable;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkSetService;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItem;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItemSitelink;
import ru.yandex.direct.grid.core.entity.banner.service.GridFindAndReplaceHrefTestBase;
import ru.yandex.direct.grid.core.entity.banner.service.internal.container.GridBannerUpdateInfo;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.invalidSitelinkHref;
import static ru.yandex.direct.grid.core.entity.banner.service.internal.GridBannerHrefWithSitelinksUpdate.SITELINKS_SET_FIELD_NAME;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridCoreTest
@RunWith(Parameterized.class)
public class GridBannerHrefWithSitelinksUpdateTest extends GridFindAndReplaceHrefTestBase {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public boolean previewOnly;

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][]{{true}, {false}};
    }

    @Autowired
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;
    @Autowired
    private SitelinkSetService sitelinkSetService;
    @Autowired
    private OldBannerRepository bannerRepository;
    @Autowired
    private SitelinkSetRepository sitelinkSetRepository;

    @Test
    public void oneBannerWithSitelinks_Successful() {
        OldBanner bannerWithReplace = createBannerWithReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink);

        GdiFindAndReplaceBannerHrefItem bannerInfo =
                getBannerPreviewItem(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink, emptyMap());

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();

        assertThat(result.getUpdatedBannerCount(), is(1));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo, sitelinkSetWithReplaceSecondSitelink, false);
        }
    }

    @Test
    public void twoBannerWithSitelinks_Successful() {
        OldBanner bannerWithReplace1 = createBannerWithReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink1 = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplace1, sitelinkSetWithReplaceSecondSitelink1);

        OldBanner bannerWithReplace2 = createBannerWithReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink2 = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplace2, sitelinkSetWithReplaceSecondSitelink2);

        GdiFindAndReplaceBannerHrefItem bannerInfo1 =
                getBannerPreviewItem(bannerWithReplace1, sitelinkSetWithReplaceSecondSitelink1, emptyMap());
        GdiFindAndReplaceBannerHrefItem bannerInfo2 =
                getBannerPreviewItem(bannerWithReplace2, sitelinkSetWithReplaceSecondSitelink2, emptyMap());

        GridBannerHrefWithSitelinksUpdate operation = createOperation(asList(bannerInfo1, bannerInfo2));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();

        assertThat(result.getUpdatedBannerCount(), is(2));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo1, sitelinkSetWithReplaceSecondSitelink1, false);
            checkBannerUpdated(bannerInfo2, sitelinkSetWithReplaceSecondSitelink2, false);
        }
    }

    @Test
    public void oneBannerForReplaceHrefAndOneForReplaceSitelink_Successful() {
        OldBanner bannerWithReplace = createBannerWithReplace();

        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithoutReplace, sitelinkSetWithReplaceSecondSitelink);

        GdiFindAndReplaceBannerHrefItem bannerInfo1 =
                getBannerPreviewItem(bannerWithReplace, null, emptyMap());
        GdiFindAndReplaceBannerHrefItem bannerInfo2 =
                getBannerPreviewItem(bannerWithoutReplace, sitelinkSetWithReplaceSecondSitelink, emptyMap());

        GridBannerHrefWithSitelinksUpdate operation = createOperation(asList(bannerInfo1, bannerInfo2));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(2));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo1, null, false);
            checkBannerUpdated(bannerInfo2, sitelinkSetWithReplaceSecondSitelink, false);
        }
    }

    @Test
    public void oneBannerForUpdateHref_Successful() {
        OldBanner bannerWithReplace = createBannerWithReplace();

        GdiFindAndReplaceBannerHrefItem bannerInfo = getBannerPreviewItem(bannerWithReplace, null, emptyMap());

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(1));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo, null, false);
        }
    }

    @Test
    public void oneBannerWithSitelinksForUpdateBannerHref_Successful() {
        OldBanner bannerWithReplace = createBannerWithReplace();
        SitelinkSet sitelinkSetWithoutReplace = createSitelinkSetWithoutReplace();
        linkSitelinkSetToBanner(bannerWithReplace, sitelinkSetWithoutReplace);

        GdiFindAndReplaceBannerHrefItem bannerInfo =
                getBannerPreviewItem(bannerWithReplace, sitelinkSetWithoutReplace, emptyMap());

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(1));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo, null, false);
        }
    }

    @Test
    public void onlySitelinksNeedUpdate_Successful() {
        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithoutReplace, sitelinkSetWithReplaceSecondSitelink);

        GdiFindAndReplaceBannerHrefItem bannerInfo = getBannerPreviewItem(bannerWithoutReplace, null, emptyMap());

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(1));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo, sitelinkSetWithReplaceSecondSitelink, false);
        }
    }

    @Test
    public void nothingNeedUpdate_Successful() {
        GridBannerHrefWithSitelinksUpdate operation = createOperation(emptyList());
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(0));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void oneBannerForUpdateHrefWithError_ResultHasError() {
        OldBanner bannerWithReplace = createBannerWithReplace();
        SitelinkSet sitelinkSetWithoutReplace = createSitelinkSetWithoutReplace();
        linkSitelinkSetToBanner(bannerWithReplace, sitelinkSetWithoutReplace);

        GdiFindAndReplaceBannerHrefItem bannerInfo =
                getBannerPreviewItem(bannerWithReplace, sitelinkSetWithoutReplace, emptyMap())
                        .withNewHref("invalid href");

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(0));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(OldBanner.HREF)), invalidHref())));

        if (!previewOnly) {
            checkBannerNotUpdated(bannerInfo, sitelinkSetWithoutReplace);
        }
    }

    @Test
    public void oneBannerForUpdateSitelinkWithError_ResultHasError() {
        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithoutReplace, sitelinkSetWithReplaceSecondSitelink);

        GdiFindAndReplaceBannerHrefItem bannerInfo =
                getBannerPreviewItem(bannerWithoutReplace, sitelinkSetWithReplaceSecondSitelink, emptyMap());
        bannerInfo.getSitelinks().get(1).getSitelink().withHref("invalid href");

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(0));
        Path errorPath =
                path(index(0), field(SITELINKS_SET_FIELD_NAME), field(SitelinkSet.SITELINKS), index(1),
                        field(Sitelink.HREF));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errorPath, invalidSitelinkHref())));

        if (!previewOnly) {
            checkBannerNotUpdated(bannerInfo, sitelinkSetWithReplaceSecondSitelink);
        }
    }

    @Test
    public void bannerForUpdateHrefAndSitelinksWithErrorInBannerHref_NothingUpdated() {
        OldBanner bannerWithReplace = createBannerWithReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink);

        GdiFindAndReplaceBannerHrefItem bannerInfo =
                getBannerPreviewItem(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink, emptyMap())
                        .withNewHref("invalid href");

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();
        assertThat(result.getUpdatedBannerCount(), is(0));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(OldBanner.HREF)), invalidHref())));

        if (!previewOnly) {
            checkBannerNotUpdated(bannerInfo, sitelinkSetWithReplaceSecondSitelink);
        }
    }

    @Test
    public void bannerForUpdateHrefAndSitelinksWithErrorInSitelink_OnlyBannerHrefUpdated() {
        OldBanner bannerWithReplace = createBannerWithReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink);

        GdiFindAndReplaceBannerHrefItem bannerInfo =
                getBannerPreviewItem(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink, emptyMap());
        bannerInfo.getSitelinks().get(1).getSitelink().withHref("invalid href");

        GridBannerHrefWithSitelinksUpdate operation = createOperation(singletonList(bannerInfo));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();

        //не учитываем такой баннер в updatedBannerCount, так как он был обновлен не полностью
        assertThat(result.getUpdatedBannerCount(), is(0));
        Path errorPath =
                path(index(0), field(SITELINKS_SET_FIELD_NAME), field(SitelinkSet.SITELINKS), index(1),
                        field(Sitelink.HREF));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errorPath, invalidSitelinkHref())));

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo, sitelinkSetWithReplaceSecondSitelink, true);
        }
    }

    @Test
    public void firstBannerWithoutSitelinksSecondBannerWithSitelinksWithError() {
        OldBanner bannerWithReplace = createBannerWithReplace();

        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithoutReplace, sitelinkSetWithReplaceSecondSitelink);

        GdiFindAndReplaceBannerHrefItem bannerInfo1 = getBannerPreviewItem(bannerWithReplace, null, emptyMap());
        GdiFindAndReplaceBannerHrefItem bannerInfo2 =
                getBannerPreviewItem(bannerWithoutReplace, sitelinkSetWithReplaceSecondSitelink, emptyMap());
        bannerInfo2.getSitelinks().get(1).getSitelink().withHref("invalid href");

        GridBannerHrefWithSitelinksUpdate operation = createOperation(asList(bannerInfo1, bannerInfo2));
        GridBannerUpdateInfo result = previewOnly ? operation.preview() : operation.update();

        assertThat(result.getUpdatedBannerCount(), is(1));
        Path errorPath =
                path(index(1), field(SITELINKS_SET_FIELD_NAME), field(SitelinkSet.SITELINKS), index(1),
                        field(Sitelink.HREF));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errorPath, invalidSitelinkHref())));

        if (!previewOnly) {
            checkBannerUpdated(bannerInfo1, null, false);
            checkBannerUpdated(bannerInfo2, sitelinkSetWithReplaceSecondSitelink, true);
        }
    }

    private GridBannerHrefWithSitelinksUpdate createOperation(List<GdiFindAndReplaceBannerHrefItem> bannersInfo) {
        return new GridBannerHrefWithSitelinksUpdate(bannersUpdateOperationFactory, sitelinkSetService, bannersInfo,
                clientInfo.getUid(), clientInfo.getClientId());
    }

    private void checkBannerUpdated(GdiFindAndReplaceBannerHrefItem bannerInfo,
                                    @Nullable SitelinkSet oldSitelinkSet, boolean sitelinksValidationError) {
        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(clientInfo.getShard(), singletonList(bannerInfo.getBannerId()))
                        .get(0);
        Long sitelinkSetOldId = ifNotNull(oldSitelinkSet, SitelinkSet::getId);
        boolean sitelinkSetShouldBeChanged = !bannerInfo.getSitelinks().isEmpty() && !sitelinksValidationError;
        OldTextBanner expectedBanner = new OldTextBanner()
                .withId(bannerInfo.getBannerId())
                .withHref(nvl(bannerInfo.getNewHref(), bannerInfo.getOldHref()))
                //не проверяем здесь id нового сета, если он поменялся
                .withSitelinksSetId(sitelinkSetShouldBeChanged ? null : sitelinkSetOldId);
        assertThat("поля баннера обновились неверно", actualBanner,
                beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));

        if (sitelinkSetShouldBeChanged) {
            assertThat("к баннеру должен быть привязан набор сайтлинков", actualBanner.getSitelinksSetId(),
                    notNullValue());
            assertThat("должен был привязаться другой набор сайтлинков", actualBanner.getSitelinksSetId(),
                    not(sitelinkSetOldId));

            Long newSitelinksSetId = actualBanner.getSitelinksSetId();
            List<Sitelink> sitelinks =
                    sitelinkSetRepository.getSitelinksBySetIds(clientInfo.getShard(), singletonList(newSitelinksSetId))
                            .get(newSitelinksSetId);
            List<Sitelink> expectedSitelinks =
                    mapList(bannerInfo.getSitelinks(), GdiFindAndReplaceBannerHrefItemSitelink::getSitelink);

            assertThat(sitelinks, beanDiffer(expectedSitelinks));
        }
    }

    private void checkBannerNotUpdated(GdiFindAndReplaceBannerHrefItem bannerInfo,
                                       @Nullable SitelinkSet oldSitelinkSet) {
        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(clientInfo.getShard(), singletonList(bannerInfo.getBannerId()))
                        .get(0);

        Long sitelinkSetOldId = ifNotNull(oldSitelinkSet, SitelinkSet::getId);
        OldTextBanner expectedBanner = new OldTextBanner()
                .withId(bannerInfo.getBannerId())
                .withHref(bannerInfo.getOldHref())
                .withSitelinksSetId(sitelinkSetOldId);
        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }
}
