package ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.add;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.AddComplexContentPromotionAdGroupValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.ComplexContentPromotionAdGroupTestData.emptyAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.clientDynamicBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AddComplexContentPromotionAdGroupValidationServiceTest {

    @Autowired
    private AddComplexContentPromotionAdGroupValidationService addValidationService;

    @Test
    public void invalidAdGroupTypeValidationErrorTest() {
        AdGroup adGroup = activeTextAdGroup();
        ComplexContentPromotionAdGroup complexAdGroup = new ComplexContentPromotionAdGroup().withAdGroup(adGroup);
        List<AdGroup> adGroups = singletonList(adGroup);
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexAdGroup));
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.TYPE)), adGroupTypeNotSupported())));
    }

    @Test
    public void invalidBannerTypeValidationErrorTest() {
        ComplexContentPromotionAdGroup complexAdGroup = emptyAdGroup(1L)
                .withBanners(singletonList(clientDynamicBanner()));
        List<AdGroup> adGroups = singletonList(complexAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService.validateAdGroups(
                ValidationResult.success(adGroups), singletonList(complexAdGroup));
        Path errPath =
                path(index(0), field(ComplexContentPromotionAdGroup.BANNERS), index(0));
        assertThat("баннер валидируется", vr, hasDefectDefinitionWith(
                validationError(errPath, inconsistentStateBannerTypeAndAdgroupType())));
    }
}
