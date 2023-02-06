package ru.yandex.direct.internaltools.tools.bs.export.queue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.bs.export.queue.service.FullExportQueueService;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.tools.bs.export.queue.model.ManageCampaignsFullExportQueueParameters;
import ru.yandex.direct.internaltools.utils.ToolParameterUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@ParametersAreNonnullByDefault
public class RemoveCampaignsFromFullExportQueueToolMethodTest {

    private RemoveCampaignsFromFullExportQueueTool tool;
    private ManageCampaignsFullExportQueueParameters request;

    @Mock
    private FullExportQueueService service;

    @Before
    public void initTestData() {
        initMocks(this);
        tool = new RemoveCampaignsFromFullExportQueueTool(service);
        request = new ManageCampaignsFullExportQueueParameters().withCampaignIds(" 111,,,42, 89, 100500   11,");
    }

    @Test
    public void checkValidationErrors() {
        ManageCampaignsFullExportQueueToolMethodTest.checkValidationErrors(tool);
    }

    @Test
    public void checkValidation() {
        ManageCampaignsFullExportQueueToolMethodTest.checkValidation(tool);
    }

    @Test
    public void checkCallAddCampaignsToFullExportQueue() {
        Set<Long> expected = new HashSet<>(Arrays.asList(11L, 42L, 89L, 111L, 100500L));
        tool.process(request);
        verify(service).removeCampaignsFromFullExportQueue(eq(expected));
    }

    @Test
    public void checkProcessResult() {
        Set<Long> campaignIds = ToolParameterUtils.getLongIdsFromString(request.getCampaignIds());
        doReturn(campaignIds.size()).when(service).removeCampaignsFromFullExportQueue(any());

        InternalToolResult result = tool.process(request);

        InternalToolResult expectedResult = new InternalToolResult()
                .withMessage(String.format(ManageCampaignsFullExportQueueTool.RESULT_TEMPLATE, campaignIds.size()));
        assertThat("результат соответствуют ожиданиям", result, beanDiffer(expectedResult));
    }
}
