package ru.yandex.direct.core.entity.banner.type.tns;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithTns;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTns;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTnsUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTns> {

    private static final String CORRECT_TNS_ID = "abc";
    private static final String BLANK_TNS_ID = " ";

    @Test
    public void invalidTnsIdForCpmBanner() {
        bannerInfo = createCpmBanner();

        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(BLANK_TNS_ID, BannerWithTns.TNS_ID);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithTns.TNS_ID)),
                notEmptyString())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private CpmBannerInfo createCpmBanner() {
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(defaultClient);
        return steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId())
                        .withTnsId(CORRECT_TNS_ID),
                defaultClient);
    }
}
