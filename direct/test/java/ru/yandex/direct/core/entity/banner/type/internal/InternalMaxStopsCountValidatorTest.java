package ru.yandex.direct.core.entity.banner.type.internal;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalCampaign;
import ru.yandex.direct.validation.result.Defect;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredCampaignOrAdGroupMaxStopsCountButEmpty;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class InternalMaxStopsCountValidatorTest {
    private static final Long REQUIRED_MAX_STOPS_COUNT_TEMPLATE_ID = 3131L;
    private static final Long NOT_REQUIRED_MAX_STOPS_COUNT_TEMPLATE_ID = 3197L;
    private static final Integer MAX_STOPS_COUNT = 1;

    private InternalCampaign campaign;
    private InternalAdGroup adGroup;
    private InternalBanner banner;

    @Before
    public void before() {
        campaign = new InternalAutobudgetCampaign();
        adGroup = new InternalAdGroup();
        banner = new InternalBanner();
    }

    @SuppressWarnings("unused")
    private Object[] test_params() {
        return new Object[][]{
                {"Both campaign and adgroup maxStopsCount are null, maxStopsCount is required",
                        null, null, REQUIRED_MAX_STOPS_COUNT_TEMPLATE_ID,
                        requiredCampaignOrAdGroupMaxStopsCountButEmpty()},
                {"Only campaign maxStopsCount is filled, maxStopsCount is required",
                        MAX_STOPS_COUNT, null, REQUIRED_MAX_STOPS_COUNT_TEMPLATE_ID, null},
                {"Only adgroup maxStopsCount is filled, maxStopsCount is required",
                        null, MAX_STOPS_COUNT, REQUIRED_MAX_STOPS_COUNT_TEMPLATE_ID, null},
                {"Both campaign and adgroup maxStopsCount are filled, maxStopsCount is required",
                        MAX_STOPS_COUNT, MAX_STOPS_COUNT, REQUIRED_MAX_STOPS_COUNT_TEMPLATE_ID, null},
                {"Both campaign and adgroup maxStopsCount are null, maxStopsCount is not required",
                        null, null, NOT_REQUIRED_MAX_STOPS_COUNT_TEMPLATE_ID, null},
        };
    }

    @Test
    @Parameters(method = "test_params")
    @TestCaseName("{0}")
    public void test(@SuppressWarnings("unused") String testName,
                     Integer campaignMaxStopsCount,
                     Integer adGroupMaxStopsCount,
                     Long templateId,
                     Defect defect) {
        campaign.setMaxStopsCount(campaignMaxStopsCount);
        adGroup.setMaxStopsCount(adGroupMaxStopsCount);
        banner.setTemplateId(templateId);

        var validationResult = createValidator().apply(banner);

        if (defect != null) {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(path(), defect)));
        } else {
            assertThat(validationResult, hasNoErrorsAndWarnings());
        }
    }

    private InternalMaxStopsCountValidator createValidator() {
        return new InternalMaxStopsCountValidator(campaign, adGroup);
    }
}
