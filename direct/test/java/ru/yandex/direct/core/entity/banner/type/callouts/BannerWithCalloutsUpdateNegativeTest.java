package ru.yandex.direct.core.entity.banner.type.callouts;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithCallouts.CALLOUT_IDS;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.duplicatedAdExtensionId;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCalloutsUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithCallouts> {

    @Test
    public void containsDuplicateCallout() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Long calloutId = steps.calloutSteps().createDefaultCallout(clientInfo).getId();
        bannerInfo = createBanner(clientInfo, List.of(calloutId));

        List<Long> newCallouts = List.of(calloutId, calloutId);
        ModelChanges<TextBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                        .process(newCallouts, CALLOUT_IDS);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CALLOUT_IDS)),
                duplicatedAdExtensionId(calloutId))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private TextBannerInfo createBanner(ClientInfo clientInfo, List<Long> callouts) {
        // степы кривоваты, покороче не получилось написать. кажется не критично в свете скорого перехода
        // на новые степы
        OldTextBanner banner = activeTextBanner().withCalloutIds(callouts);
        return steps.bannerSteps().createBanner(banner, clientInfo);
    }

}
