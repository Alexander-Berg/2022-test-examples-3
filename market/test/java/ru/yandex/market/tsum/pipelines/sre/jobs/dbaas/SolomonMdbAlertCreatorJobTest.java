package ru.yandex.market.tsum.pipelines.sre.jobs.dbaas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.solomon.models.SolomonAlert;
import ru.yandex.market.tsum.clients.solomon.models.SolomonAssociatedChannel;
import ru.yandex.market.tsum.pipelines.common.resources.SolomonAlertsConfigResource;
import ru.yandex.market.tsum.pipelines.common.resources.SolomonNotificationChannelIdResource;
import ru.yandex.market.tsum.pipelines.sre.resources.DbaasPipelineConfig;

@RunWith(MockitoJUnitRunner.class)
public class SolomonMdbAlertCreatorJobTest {
    private static final Gson GSON = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")
        .create();

    @Mock
    private SolomonAlertsConfigResource solomonAlertsConfig;
    @Mock
    private DbaasPipelineConfig dbaasPipelineConfig;
    @Mock
    protected List<SolomonNotificationChannelIdResource> notificationChannels;
    @InjectMocks
    private SolomonMdbAlertCreatorJob solomonMdbAlertCreatorJob;

    @Before
    public void setupMocks() {
        Mockito.when(solomonAlertsConfig.getMdbDiskFreeCritPercent()).thenReturn(10);
        Mockito.when(solomonAlertsConfig.getMdbDiskFreeCritPercent()).thenReturn(10);
        Mockito.when(solomonAlertsConfig.getMdbDiskFreeWarnPercent()).thenReturn(20);
        Mockito.when(dbaasPipelineConfig.getFolderId()).thenReturn("folderId");
        Mockito.when(dbaasPipelineConfig.getDatabaseName()).thenReturn("some_real_long_service_name_production");
    }

    @Test
    public void testChannel() throws IOException {
        SolomonAlert solomonAlert = solomonMdbAlertCreatorJob.generateMdbDiskQuotaAlert(
            "some-real-long-service-name",
            "market-service",
            "robot-market-infra",
            SolomonMdbAlertCreatorJob.DbType.POSTGRESQL,
            "stable",
            new SolomonMdbAlertCreatorJob.ClusterInfo(
                "clusterId",
                "folderId"
            ),
            List.of(new SolomonAssociatedChannel("juggler_spok"))
        );

        JsonElement expected = GSON.fromJson(
            IOUtils.toString(
                this.getClass().getResourceAsStream(
                    "/alertCreatorTests/SolomonMdbAlertCreatorJobTest-disk_quota-test.json"
                ),
                StandardCharsets.UTF_8
            ),
            JsonElement.class
        );

        Assert.assertEquals(expected, GSON.toJsonTree(solomonAlert));
    }
}
