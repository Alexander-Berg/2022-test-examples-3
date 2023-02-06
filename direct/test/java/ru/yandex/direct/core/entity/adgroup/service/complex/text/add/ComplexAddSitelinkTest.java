package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullTextBannerForAdd;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.invalidSitelinkHref;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.validation.defects.Defects.hrefOrTurboRequired;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddSitelinkTest extends ComplexTextAddTestBase {

    @Test
    public void oneAdGroupWithBannerWithSitelinkSet() {
        ComplexTextAdGroup complexAdGroup = adGroupWithBanners(singletonList(bannerWithSitelinkSet()));
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void oneAdGroupWithSeveralBannersWithAndWithoutSitelinks() {
        ComplexTextAdGroup complexAdGroup =
                adGroupWithBanners(asList(bannerWithSitelinkSet(), emptyBannerForAdd(), bannerWithSitelinkSet()));
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void adGroupsWithOneAndSeveralBannersWithAndWithoutSitelinks() {
        ComplexTextAdGroup severalBannersAdGroup =
                adGroupWithBanners(asList(emptyBannerForAdd(), bannerWithSitelinkSet(), bannerWithSitelinkSet()));
        ComplexTextAdGroup oneBannerAdGroup = adGroupWithBanners(singletonList(emptyBannerForAdd()));
        addAndCheckComplexAdGroups(asList(severalBannersAdGroup, oneBannerAdGroup));
    }

    //валидация

    @Test
    public void severalAdGroupsWithValidationErrorsInSitelinks() {
        ComplexBanner bannerWithInvalidSitelinkSet1 = fullTextBannerForAdd();
        bannerWithInvalidSitelinkSet1.getSitelinkSet().getSitelinks().get(0).setHref(null);
        ComplexBanner bannerWithInvalidSitelinkSet2 = fullTextBannerForAdd();
        bannerWithInvalidSitelinkSet2.getSitelinkSet().getSitelinks().get(2).setHref("");

        ComplexTextAdGroup adGroup1 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withComplexBanners(asList(fullTextBannerForAdd(), bannerWithInvalidSitelinkSet1));
        ComplexTextAdGroup adGroup2 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withComplexBanners(asList(bannerWithInvalidSitelinkSet2, emptyBannerForAdd()));

        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(adGroup1, adGroup2));
        MassResult<Long> result = addOperation.prepareAndApply();
        Path path1 = path(index(0), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(1),
                field(ComplexBanner.SITELINK_SET), field(SitelinkSet.SITELINKS), index(0), field(Sitelink.HREF));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path1, hrefOrTurboRequired())));
        Path path2 = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(0),
                field(ComplexBanner.SITELINK_SET), field(SitelinkSet.SITELINKS), index(2), field(Sitelink.HREF));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path2, invalidSitelinkHref())));
        assertThat("должно быть всего 2 ошибки", result.getValidationResult().flattenErrors(), hasSize(2));
    }

    protected ComplexBanner bannerWithSitelinkSet() {
        return ComplexTextAdGroupTestData.bannerWithSitelinkSet(null);
    }
}
