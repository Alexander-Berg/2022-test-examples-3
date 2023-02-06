package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalS2sTrackingForbidden;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainerImpl;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CampaignWithOptionalS2sTrackingForbiddenAddValidationTypeSupportTest {

    private CampaignWithOptionalS2sTrackingForbiddenAddValidationTypeSupport validationTypeSupport =
            new CampaignWithOptionalS2sTrackingForbiddenAddValidationTypeSupport();

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());

    @Parameterized.Parameters(name = "valueReceived = {1}, expecting defect = {2}")
    public static Object[][] params() {
        return new Object[][]{
                {null, null},
                {false, CommonDefects.isNull()},
                {true, CommonDefects.isNull()}
        };
    }

    @Test
    @Parameters(method = "params")
    public void validateTextCampaign(Boolean valueReceived,
                                     @Nullable Defect expectedDefect) {
        var campaign = new McBannerCampaign()
                .withClientId(clientId.asLong())
                .withIsS2sTrackingEnabled(valueReceived);

        var vr = new ValidationResult<List<CampaignWithOptionalS2sTrackingForbidden>, Defect>(List.of(campaign));

        var container = new CampaignValidationContainerImpl(0, 0L, clientId, null, new CampaignOptions(), null,
                emptyMap());
        var result = validationTypeSupport.validate(container, vr);

        if (expectedDefect == null) {
            assertThat(result, hasNoDefectsDefinitions());
        } else {
            assertThat(result, hasDefectWithDefinition(validationError(
                    path(index(0), field(CampaignWithOptionalS2sTrackingForbidden.IS_S2S_TRACKING_ENABLED)),
                    expectedDefect
            )));
        }
    }

}
