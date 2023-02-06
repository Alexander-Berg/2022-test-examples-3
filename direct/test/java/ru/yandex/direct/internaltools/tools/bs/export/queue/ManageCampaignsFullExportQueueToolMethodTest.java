package ru.yandex.direct.internaltools.tools.bs.export.queue;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.internaltools.tools.bs.export.queue.model.ManageCampaignsFullExportQueueParameters;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
abstract class ManageCampaignsFullExportQueueToolMethodTest {

    private static final ManageCampaignsFullExportQueueParameters request = new ManageCampaignsFullExportQueueParameters();

    static void checkValidationErrors(ManageCampaignsFullExportQueueTool tool) {
        String invalidId = "1234b";
        String invalidId2 = "a12";

        ValidationResult<ManageCampaignsFullExportQueueParameters, Defect> validate =
                tool.validate(request.withCampaignIds(String.format("1, %s, 123, %s", invalidId, invalidId2)));

        assertThat(validate, hasDefectDefinitionWith(validationError(path(field("campaignIds")), CommonDefects.validId())));
    }

    static void checkValidation(ManageCampaignsFullExportQueueTool tool) {
        ValidationResult<ManageCampaignsFullExportQueueParameters, Defect> validate =
                tool.validate(request.withCampaignIds("    1    12,   123,,,,  "));
        assertThat("нет ошибок валидации", validate.hasAnyErrors(), is(false));
    }
}
