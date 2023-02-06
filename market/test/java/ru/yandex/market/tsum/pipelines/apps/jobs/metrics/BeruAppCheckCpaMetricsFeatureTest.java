package ru.yandex.market.tsum.pipelines.apps.jobs.metrics;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.yql.DataFormat;
import ru.yandex.market.tsum.clients.yql.Status;
import ru.yandex.market.tsum.clients.yql.YqlApiClient;
import ru.yandex.market.tsum.clients.yql.YqlOperationResponse;
import ru.yandex.market.tsum.clients.yql.YqlQueryResponse;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipelines.apps.MobilePlatform;
import ru.yandex.market.tsum.pipelines.apps.jobs.crashfree.version.ProductionAppVersionName;
import ru.yandex.market.tsum.pipelines.apps.resources.MobilePlatformResource;
import ru.yandex.market.tsum.pipelines.common.jobs.yql.YqlScriptResource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BeruAppCheckCpaMetricsFeatureTest {

    private static final String TEMPLATE_UD = "2";
    private static final String OPERATION_ID = "operation_id";
    private static final String OPERATION_CONTENT = "content";

    @InjectMocks
    private BeruAppCheckCpaMetricsFeature feature = new BeruAppCheckCpaMetricsFeature();
    @Mock
    private YqlApiClient yqlApiClient;
    @Mock
    private YqlScriptResource yqlScriptResource;
    @Mock
    private MobilePlatformResource platformResource;
    @Mock
    private ProductionAppVersionName appVersionName;
    @Mock
    private JobContext context;
    @Mock
    private JobActionsContext actionsContext;
    @Mock
    private JobProgressContext progressContext;

    @Mock
    private YqlOperationResponse response;

    @Before
    public void setUp() {
        when(context.actions()).thenReturn(actionsContext);
        when(context.progress()).thenReturn(progressContext);
        when(yqlScriptResource.getOperationId()).thenReturn(TEMPLATE_UD);
        when(yqlScriptResource.getTimeoutInSecs()).thenReturn(3);
        when(platformResource.getPlatform()).thenReturn(MobilePlatform.ANDROID);
        when(appVersionName.getVersionName()).thenReturn("111");
    }

    @Test
    public void testPositive() throws Exception {
        executeFeature("{\"clicks\":78979,\"matched_ware_md5\":48685,\"matched_uuid\":66054}");
        verify(actionsContext, never()).failJob(any(), eq(SupportType.NONE));
    }

    @Test
    public void testZeroClicks() throws Exception {
        executeFeature("{\"clicks\":0,\"matched_ware_md5\":48685,\"matched_uuid\":66054}");
        verify(actionsContext).failJob("Clicks not found", SupportType.NONE);
    }

    @Test
    public void testLowUuidMatches() throws Exception {
        executeFeature("{\"clicks\":78979,\"matched_ware_md5\":48685,\"matched_uuid\":1000}");
        verify(actionsContext).failJob("uuid not matched", SupportType.NONE);
    }

    @Test
    public void testLowWareMd5Matches() throws Exception {
        executeFeature("{\"clicks\":78979,\"matched_ware_md5\":10000,\"matched_uuid\":66054}");
        verify(actionsContext).failJob("wareMd5 not matched", SupportType.NONE);
    }

    private void executeFeature(String expectedQueryResult) throws Exception {
        when(yqlApiClient.getQuery(TEMPLATE_UD))
            .thenReturn(new YqlQueryResponse(TEMPLATE_UD, new YqlQueryResponse.Data(OPERATION_CONTENT)));

        when(response.getId()).thenReturn(OPERATION_ID);
        when(response.getStatus()).thenReturn(Status.COMPLETED);

        when(yqlApiClient.addOperation(any())).thenReturn(response);
        when(yqlApiClient.shareOperation(OPERATION_ID)).thenReturn("sharelink");
        when(yqlApiClient.waitOperation(eq(OPERATION_ID), anyInt(), anyInt())).thenReturn(response);

        when(yqlApiClient.getOperationResultData(
            eq(OPERATION_ID),
            eq(DataFormat.JSON),
            eq(0),
            ArgumentMatchers.intThat(val -> val > 1))
        ).thenReturn(Stream.of(expectedQueryResult));

        feature.execute(context);
    }
}
