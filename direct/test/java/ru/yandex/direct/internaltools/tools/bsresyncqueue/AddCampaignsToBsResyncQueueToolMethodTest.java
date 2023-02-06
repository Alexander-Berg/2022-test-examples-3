package ru.yandex.direct.internaltools.tools.bsresyncqueue;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.tools.bsresyncqueue.container.AddedCampaignInfo;
import ru.yandex.direct.internaltools.tools.bsresyncqueue.model.AddCampaignsToBsResyncQueueParameters;
import ru.yandex.direct.internaltools.tools.bsresyncqueue.model.ResyncPriority;
import ru.yandex.direct.internaltools.utils.ToolParameterUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddCampaignsToBsResyncQueueToolMethodTest {

    private AddCampaignsToBsResyncQueueTool tool;
    private BsResyncService bsResyncService;
    private AddCampaignsToBsResyncQueueParameters request;

    @Before
    public void initTestData() {
        bsResyncService = mock(BsResyncService.class);
        tool = new AddCampaignsToBsResyncQueueTool(bsResyncService);
        request = new AddCampaignsToBsResyncQueueParameters()
                .withCampaignIds("123, 1234")
                .withPriority(ResyncPriority.LAZY_RESYNC);
    }


    @Test
    public void checkValidation() {
        ValidationResult<AddCampaignsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withCampaignIds("    1    12,   123,,,,  "));
        assertThat("нет ошибок валидации", validate.hasAnyErrors(), is(false));
    }

    @Test
    public void checkValidationErrors() {
        String invalidId = "1234b";
        String invalidId2 = "a12";
        ValidationResult<AddCampaignsToBsResyncQueueParameters, Defect> validate =
                tool.validate(request.withCampaignIds(format("1, %s, 123, %s", invalidId, invalidId2)));

        assertThat(validate, hasDefectDefinitionWith(
                validationError(path(field("campaignIds")), CommonDefects.validId())));
    }

    @Test
    public void checkCallAddWholeCampaignsToResync() {
        Set<Long> campaignIds = ToolParameterUtils.getLongIdsFromString(request.getCampaignIds());

        tool.process(request);
        verify(bsResyncService)
                .addWholeCampaignsToResync(eq(campaignIds), eq(request.getPriority().getBsResyncPriority()));
    }

    @Test
    public void checkProcessResult() {
        Set<Long> campaignIds = ToolParameterUtils.getLongIdsFromString(request.getCampaignIds());
        Map<Long, Long> data = campaignIds.stream()
                .collect(toMap(Function.identity(), v -> RandomNumberUtils.nextPositiveLong()));
        doReturn(data).when(bsResyncService).addWholeCampaignsToResync(any(), any());

        InternalToolMassResult<AddedCampaignInfo> result = tool.process(request);

        InternalToolMassResult<AddedCampaignInfo> expectedResult = new InternalToolMassResult<>();
        data.forEach((key, value) -> expectedResult.addItem(new AddedCampaignInfo(key, value)));
        assertThat("результат соответствуют ожиданиям", result, beanDiffer(expectedResult));
    }
}
