package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullTextBannerForAdd;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddVcardTest extends ComplexTextAddTestBase {

    @Test
    public void oneAdGroupWithBannerWithVcard() {
        ComplexTextAdGroup complexAdGroup = adGroupWithBanners(singletonList(bannerWithVcard()));
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void oneAdGroupWithSeveralBannersWithAndWithoutVcards() {
        ComplexTextAdGroup complexAdGroup =
                adGroupWithBanners(asList(bannerWithVcard(), emptyBannerForAdd(), bannerWithVcard()));
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void adGroupsWithOneAndSeveralBannersWithAndWithoutVcards() {
        ComplexTextAdGroup severalBannersAdGroup =
                adGroupWithBanners(asList(emptyBannerForAdd(), bannerWithVcard(), bannerWithVcard()));
        ComplexTextAdGroup oneBannerAdGroup = adGroupWithBanners(singletonList(emptyBannerForAdd()));
        addAndCheckComplexAdGroups(asList(severalBannersAdGroup, oneBannerAdGroup));
    }

    @Test
    public void validAdGroupAndAdGroupWithVcardWithValidationError() {
        ComplexTextAdGroup validAdGroup = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withComplexBanners(asList(fullTextBannerForAdd(), emptyBannerForAdd()));
        ComplexBanner bannerWithInvalidVcard = fullTextBannerForAdd();
        bannerWithInvalidVcard.getVcard().withPhone(null);
        ComplexTextAdGroup invalidAdGroup = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withComplexBanners(asList(fullTextBannerForAdd(), bannerWithInvalidVcard));
        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(validAdGroup, invalidAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(1),
                        field(ComplexBanner.VCARD), field(Vcard.PHONE)), notNull())));
    }

    protected ComplexBanner bannerWithVcard() {
        return ComplexTextAdGroupTestData.bannerWithVcard(null);
    }
}
