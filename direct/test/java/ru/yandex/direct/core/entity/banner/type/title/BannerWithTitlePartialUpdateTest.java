package ru.yandex.direct.core.entity.banner.type.title;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithTitle;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adNotFound;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.operation.Applicability.PARTIAL;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitlePartialUpdateTest extends BannerClientInfoUpdateOperationTestBase {

    private static final String NEW_TITLE = "some new title";

    @Test
    @Description("Более одного банера не прошли пре-валидацию, смотри DIRECT-123235")
    public void oneValidAndTwoInvalidBanners() {
        clientInfo = steps.clientSteps().createDefaultClient();
        TextBannerInfo banner1 = steps.bannerSteps().createBanner(activeTextBanner(), clientInfo);

        TextBannerInfo otherClientBanner1 = steps.bannerSteps().createActiveTextBanner();
        TextBannerInfo otherClientBanner2 = steps.bannerSteps().createActiveTextBanner();

        ModelChanges<TextBanner> otherModelChanges1 = createModelChanges(otherClientBanner1);
        ModelChanges<TextBanner> otherModelChanges2 = createModelChanges(otherClientBanner2);
        ModelChanges<TextBanner> modelChanges1 = createModelChanges(banner1);

        // ставим валидный modelChanges не первым, так мы зараз проверим внутренние maps от индекса
        MassResult<Long> result = createOperation(
                List.of(otherModelChanges1, otherModelChanges2, modelChanges1), PARTIAL, getModerationMode())
                .prepareAndApply();

        assumeThat(result, isSuccessful(false, false, true));

        checkValidationResult(result.get(0));
        checkValidationResult(result.get(1));
        checkTitle(result.get(2), banner1.getBannerId());
    }

    private ModelChanges<TextBanner> createModelChanges(TextBannerInfo bannerInfo) {
        return new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(NEW_TITLE, BannerWithTitle.TITLE);
    }

    private void checkTitle(Result<Long> result, Long expectedBannerId) {
        Long resultBannerId = result.getResult();
        assertThat(resultBannerId, equalTo(expectedBannerId));
        TextBanner actualBanner = getBanner(resultBannerId);
        assertThat(actualBanner.getTitle(), equalTo(NEW_TITLE));
    }

    private void checkValidationResult(Result<Long> result) {
        assertThat(result.getResult(), nullValue());
        ValidationResult<?, Defect> vr = result.getValidationResult();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithTitle.ID)), adNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
