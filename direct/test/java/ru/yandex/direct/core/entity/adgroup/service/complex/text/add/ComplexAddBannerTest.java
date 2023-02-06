package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.bannerWithSitelinkSet;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.bannerWithVcard;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyBannerForAdd;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.stringShouldNotBeBlank;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddBannerTest extends ComplexTextAddTestBase {

    @Test
    public void oneAdGroupWithBanner() {
        ComplexTextAdGroup complexAdGroup = adGroupWithBanners(singletonList(emptyBannerForAdd()));
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void oneAdGroupWithSeveralBanners() {
        ComplexTextAdGroup complexAdGroup = adGroupWithBanners(asList(
                emptyBannerForAdd(), emptyBannerForAdd(), emptyBannerForAdd()));
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void adGroupsWithOneAndSeveralBanners() {
        ComplexTextAdGroup severalBannersAdGroup =
                adGroupWithBanners(asList(emptyBannerForAdd(), emptyBannerForAdd(), emptyBannerForAdd()));
        ComplexTextAdGroup oneBannerAdGroup = adGroupWithBanners(singletonList(emptyBannerForAdd()));
        addAndCheckComplexAdGroups(asList(severalBannersAdGroup, oneBannerAdGroup));
    }

    @Test
    public void oneAdGroupWithMaxBannersInAdGroup() {
        List<ComplexBanner> complexBanners = new ArrayList<>();
        for (int i = 0; i < MAX_BANNERS_IN_ADGROUP; ++i) {
            complexBanners.add(emptyBannerForAdd());
        }
        ComplexTextAdGroup adGroup = adGroupWithBanners(complexBanners);
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("максимальное количество баннеров добавлено успешно", result, isFullySuccessful());
    }

    @Test
    public void oneAdGroupWithTooManyBannersInAdGroup() {
        List<ComplexBanner> complexBanners = new ArrayList<>();
        for (int i = 0; i < MAX_BANNERS_IN_ADGROUP + 1; ++i) {
            complexBanners.add(emptyBannerForAdd());
        }
        ComplexTextAdGroup adGroup = adGroupWithBanners(complexBanners);
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("превышено максимальное количество баннеров в группе", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("complexBanners")),
                        maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
    }

    @Test
    public void oneAdGroupWithBannerWithValidationError() {
        ComplexTextAdGroup adGroup = adGroupWithBanners(singletonList(new ComplexBanner()
                .withBanner(clientTextBanner().withTitle(""))));
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("должна быть ошибка валидации баннера", result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(0), field("complexBanners"), index(0), field("title")),
                                stringShouldNotBeBlank())));
    }

    @Test
    public void phoneFlagIsNewWhenHasNoVcard() {
        var actual = addComplexAdGroupWithBannerAndGetActualBanner(emptyBannerForAdd());
        assertThat(actual.getVcardStatusModerate(), is(BannerVcardStatusModerate.NEW));
    }

    @Test
    public void phoneFlagIsReadyWhenHasVcard() {
        var actual = addComplexAdGroupWithBannerAndGetActualBanner(bannerWithVcard(null));
        assertThat(actual.getVcardStatusModerate(), is(BannerVcardStatusModerate.READY));
    }

    @Test
    public void statusSitelinksModerateIsNewWhenHasNoSitelinks() {
        var actual = addComplexAdGroupWithBannerAndGetActualBanner(emptyBannerForAdd());
        assertThat(actual.getStatusSitelinksModerate(), is(BannerStatusSitelinksModerate.NEW));
    }

    @Test
    public void statusSitelinksModerateIsReadyWhenHasSitelinks() {
        var actual = addComplexAdGroupWithBannerAndGetActualBanner(bannerWithSitelinkSet(null));
        assertThat(actual.getStatusSitelinksModerate(), is(BannerStatusSitelinksModerate.READY));
    }

    private TextBanner addComplexAdGroupWithBannerAndGetActualBanner(ComplexTextBanner banner) {
        ComplexTextAdGroup complexAdGroup = adGroupWithBanners(singletonList(banner));
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup), false);
        addOperation.prepareAndApply();
        Long bannerId = complexAdGroup.getComplexBanners().get(0).getBanner().getId();
        return bannerTypedRepository
                .getStrictly(campaign.getShard(), singletonList(bannerId), TextBanner.class).get(0);
    }
}
