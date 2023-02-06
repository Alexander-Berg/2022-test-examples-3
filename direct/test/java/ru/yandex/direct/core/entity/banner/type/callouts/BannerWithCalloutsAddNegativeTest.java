package ru.yandex.direct.core.entity.banner.type.callouts;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithCallouts.CALLOUT_IDS;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adExtensionNotFound;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCalloutsAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void calloutDoesntExist() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        long calloutId = 1L;
        var banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCalloutIds(List.of(calloutId));

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CALLOUT_IDS)),
                adExtensionNotFound(calloutId))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void calloutForAnotherClient() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        Long calloutId1 = steps.calloutSteps().createDefaultCallout(
                steps.clientSteps().createDefaultClient()).getId();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCalloutIds(List.of(calloutId1));

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field(CALLOUT_IDS)), adExtensionNotFound(calloutId1))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
