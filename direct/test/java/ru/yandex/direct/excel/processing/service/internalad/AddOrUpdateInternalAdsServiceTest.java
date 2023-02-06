package ru.yandex.direct.excel.processing.service.internalad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.banner.container.InternalBannerAddOrUpdateItem;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo;
import ru.yandex.direct.core.testing.steps.InternalBannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.excel.processing.configuration.ExcelProcessingTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ExcelProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddOrUpdateInternalAdsServiceTest {

    private static final DefaultCompareStrategy INTERNAL_BANNER_COMPARE_STRATEGY =
            DefaultCompareStrategies.allFieldsExcept(newPath("\\d+", "lastChange"));

    @Autowired
    private Steps steps;

    @Autowired
    private InternalBannerSteps internalBannerSteps;

    @Autowired
    private BannerTypedRepository newBannerTypedRepository;

    @Autowired
    private AddOrUpdateInternalAdsService service;

    private ClientInfo clientInfo;
    private InternalBanner banner1;
    private InternalBanner banner2;
    private InternalBanner banner3;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(clientInfo);

        banner1 =
                internalBannerSteps.createInternalBanner(new NewInternalBannerInfo().withAdGroupInfo(adGroupInfo)).getBanner();
        banner2 =
                internalBannerSteps.createInternalBanner(new NewInternalBannerInfo().withAdGroupInfo(adGroupInfo)).getBanner();
        banner3 =
                internalBannerSteps.createInternalBanner(new NewInternalBannerInfo().withAdGroupInfo(adGroupInfo)).getBanner();
    }

    @Test
    public void empty_AddAndUpdate() {
        MassResult<Long> result = addOrUpdate(emptyList());

        assertThat(result, isFullySuccessful());
    }

    @Test
    public void oneAdd_AddAndUpdate() {
        MassResult<Long> result = addOrUpdate(asList(banner1.withId(null)));

        assertThat(result, isFullySuccessful());
        checkBannersInDb(List.of(banner1));
    }

    @Test
    public void oneUpdate_AddAndUpdate() {
        MassResult<Long> result = addOrUpdate(asList(banner2.withDescription(banner2.getDescription() + "smth")));

        assertThat(result, isFullySuccessful());
        checkBannersInDb(List.of(banner2));
    }

    @Test
    public void threeValid_AddAndUpdate() {
        MassResult<Long> result = addOrUpdate(asList(banner1.withId(null),
                banner2.withDescription(banner2.getDescription() + "smth"), banner3.withId(null)));

        assertThat(result, isFullySuccessful());
        checkBannersInDb(List.of(banner1, banner2, banner3));
    }

    @Test
    public void threeValid_AddAndUpdateValidationOnly() {
        var oldDescription = banner2.getDescription();
        MassResult<Long> result = addOrUpdateValidationOnly(asList(banner1.withId(null),
                banner2.withDescription(oldDescription + "smth"), banner3.withId(null)));

        assertThat(result, isSuccessful(false, false, false));
        checkBannersInDb(List.of(banner2.withDescription(oldDescription)));
    }

    @Test
    public void twoAddValidOneUpdateInvalid_AddAndUpdate() {
        var oldTemplateVariables = banner2.getTemplateVariables();
        MassResult<Long> result = addOrUpdate(asList(banner1.withId(null),
                banner2.withTemplateVariables(List.of(new TemplateVariable().withTemplateResourceId(700L))),
                banner3.withId(null)));

        assertThat(result, isSuccessful(false, false, false));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(path(field("templateVariables")))));
        checkBannersInDb(List.of(banner2.withTemplateVariables(oldTemplateVariables)));
    }

    @Test
    public void twoValidOneAddInvalid_AddAndUpdate() {
        var oldTemplateVariables = banner2.getTemplateVariables();
        MassResult<Long> result = addOrUpdate(asList(banner1.withId(null),
                banner2.withTemplateVariables(emptyList()),
                banner3.withId(null).withTemplateVariables(List.of(new TemplateVariable()))));

        assertThat(result, isSuccessful(true, false, false));
        assertThat(result.get(2).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(path(field("templateVariables")))));
        checkBannersInDb(List.of(banner2.withTemplateVariables(oldTemplateVariables)));
    }

    private MassResult<Long> addOrUpdateValidationOnly(List<InternalBanner> addItems) {
        return addOrUpdate(addItems, true);
    }

    private MassResult<Long> addOrUpdate(List<InternalBanner> addItems) {
        return addOrUpdate(addItems, false);
    }

    private MassResult<Long> addOrUpdate(List<InternalBanner> addItems, boolean validationOnly) {
        var internalBannerAddOrUpdateItems = mapList(addItems,
                banner -> new InternalBannerAddOrUpdateItem()
                        .withBanner(banner)
                        .withAdGroup(new InternalAdGroup()
                                .withId(adGroupInfo.getAdGroupId())
                                .withType(AdGroupType.INTERNAL)
                                .withCampaignId(adGroupInfo.getCampaignId())));
        return service.addOrUpdateInternalAds(internalBannerAddOrUpdateItems, clientInfo.getUid(),
                clientInfo.getClientId(), false, validationOnly);
    }

    private void checkBannersInDb(List<InternalBanner> banners) {

        List<Long> bannerIds = mapList(banners, InternalBanner::getId);
        List<InternalBanner> existentBanners = newBannerTypedRepository.getSafely(
                clientInfo.getShard(), bannerIds, InternalBanner.class);

        existentBanners.sort(Comparator.comparingLong(InternalBanner::getId));
        List<InternalBanner> sortedBanners = new ArrayList<>(banners);
        sortedBanners.sort(Comparator.comparingLong(InternalBanner::getId));
        assertThat("число баннеров в базе соответствует ожидаемому",
                sortedBanners.size() == existentBanners.size());
        Assertions.assertThat(sortedBanners)
                .is(matchedBy(beanDiffer(existentBanners)
                        .useCompareStrategy(INTERNAL_BANNER_COMPARE_STRATEGY)));
    }
}
