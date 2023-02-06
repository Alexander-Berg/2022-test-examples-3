package ru.yandex.direct.internaltools.tools.bs.export.queue;

import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.entity.bs.export.queue.service.BsExportQueueService;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.bsresyncqueue.model.CampaignIdsAndQueueType;
import ru.yandex.direct.internaltools.utils.ToolParameterUtils;
import ru.yandex.direct.testing.matchers.validation.Matchers;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class    MoveCampaignsToSpecialBsExportQueueToolTest {

    private static final String STRING_OF_CIDS = "1, 12, ,,123,,";

    @Mock
    private CampaignService campaignService;

    @Mock
    private BsExportQueueService bsExportQueueService;

    private MoveCampaignsToSpecialBsExportQueueTool tool;
    private CampaignIdsAndQueueType request;
    private Set<Long> cids;
    private QueueType queueType;

    @Before
    public void before() {
        initMocks(this);

        cids = ToolParameterUtils.getLongIdsFromString(STRING_OF_CIDS);
        Map<Long, CampaignType> cidsAndTypes = StreamEx.of(cids).mapToEntry(c -> CampaignType.TEXT).toMap();

        when(campaignService.getCampaignsTypes(cids)).thenReturn(cidsAndTypes);

        tool = new MoveCampaignsToSpecialBsExportQueueTool(campaignService, bsExportQueueService);
        request = new CampaignIdsAndQueueType()
                .withCampaignIds(STRING_OF_CIDS)
                .withQueueType(QueueType.BUGGY)
                .withResetTime(false);


        queueType = request.getQueueType();
    }

    @Test
    public void checkValidationErrorsInvalidCids() {
        String invalidCid1 = "12321asd";
        String invalidCid2 = "a123";

        ValidationResult<CampaignIdsAndQueueType, Defect> result =
                tool.validate(request.withCampaignIds(String.format("1, %s, 123, %s", invalidCid1, invalidCid2)));

        assertThat(result, hasDefectDefinitionWith(Matchers.validationError(DefectIds.MUST_BE_VALID_ID)));
    }

    @Test
    public void checkValidation() {
        ValidationResult<CampaignIdsAndQueueType, Defect> result =
                tool.validate(request);
        assertThat(result, Matchers.hasNoErrors());
    }

    @Test
    public void checkProcessResultWithResetTime() {
        tool.process(request);

        verify(bsExportQueueService).setQueueType(eq(queueType), eq(cids));
        verify(bsExportQueueService, never()).resetTimeFields(anySet());
    }

    @Test
    public void checkProcessResultWithoutResetTime() {
        tool.process(request.withResetTime(true));

        verify(bsExportQueueService).setQueueType(eq(queueType), eq(cids));
        verify(bsExportQueueService).resetTimeFields(eq(cids));
    }
}
