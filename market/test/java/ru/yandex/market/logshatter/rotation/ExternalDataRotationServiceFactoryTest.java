package ru.yandex.market.logshatter.rotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.clickhouse.ddl.ClickHouseDdlService;
import ru.yandex.market.health.configs.clickhouse.config.ClickHouseClusterConfig;
import ru.yandex.market.health.configs.clickhouse.service.ClickHouseClusterConfigurationService;
import ru.yandex.market.logshatter.config.ddl.ClickHouseDdlServiceWithConfig;
import ru.yandex.market.logshatter.config.ddl.UpdateDdlClusterService;
import ru.yandex.market.rotation.DataRotationService;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExternalDataRotationServiceFactoryTest {
    @Mock
    private UpdateDdlClusterService updateDdlClusterService;
    @Mock
    private ClickHouseDdlServiceWithConfig ddlServiceWithConfig;
    @Mock
    private ClickHouseDdlService ddlService;
    @Mock
    private ClickHouseClusterConfigurationService configurationService;
    @Mock
    private ClickHouseClusterConfig clusterConfig;

    private ExternalDataRotationServiceFactory externalDataRotationServiceFactory;

    @Before
    public void setUp() {
        externalDataRotationServiceFactory = new ExternalDataRotationServiceFactory(updateDdlClusterService);
    }

    @Test
    public void createServiceWhenDdlServiceAlreadyExists() {
        Map<String, ClickHouseDdlServiceWithConfig> ddlServiceStorage = new HashMap<>();
        ddlServiceStorage.put("test_cluster_id", ddlServiceWithConfig);

        when(ddlServiceWithConfig.getDdlService()).thenReturn(ddlService);
        when(updateDdlClusterService.getDdlServiceStorage()).thenReturn(ddlServiceStorage);

        Optional<DataRotationService> dataRotationService =
            externalDataRotationServiceFactory.create("test_cluster_id");

        verify(ddlServiceWithConfig).getDdlService();
        verify(updateDdlClusterService).getDdlServiceStorage();
        verify(updateDdlClusterService, times(0)).getClusterConfigurationService();

        assertNotNull(dataRotationService.get());
    }

    @Test
    public void createServiceWhenDdlServiceHasNotBeenCreatedFailed() {
        Map<String, ClickHouseDdlServiceWithConfig> ddlServiceStorage = new HashMap<>();

        when(updateDdlClusterService.getDdlServiceStorage()).thenReturn(ddlServiceStorage);
        when(updateDdlClusterService.getClusterConfigurationService()).thenReturn(configurationService);
        when(configurationService.getClusterConfigByClusterId("test_cluster_id")).thenReturn(Optional.empty());

        externalDataRotationServiceFactory.create("test_cluster_id");

        verify(updateDdlClusterService).getDdlServiceStorage();
        verify(updateDdlClusterService).getClusterConfigurationService();
        verify(updateDdlClusterService, times(0)).getClickHouseDdlService(any());
        verify(configurationService).getClusterConfigByClusterId("test_cluster_id");
    }

    @Test
    public void createServiceWhenDdlServiceHasNotBeenCreatedSuccessful() {
        Map<String, ClickHouseDdlServiceWithConfig> ddlServiceStorage = new HashMap<>();

        when(updateDdlClusterService.getDdlServiceStorage()).thenReturn(ddlServiceStorage);
        when(updateDdlClusterService.getClusterConfigurationService()).thenReturn(configurationService);
        when(configurationService.getClusterConfigByClusterId("test_cluster_id"))
            .thenReturn(Optional.of(clusterConfig));
        when(updateDdlClusterService.getClickHouseDdlService(clusterConfig)).thenReturn(ddlService);

        Optional<DataRotationService> dataRotationService =
            externalDataRotationServiceFactory.create("test_cluster_id");

        verify(updateDdlClusterService).getDdlServiceStorage();
        verify(updateDdlClusterService).getClusterConfigurationService();
        verify(updateDdlClusterService).getClickHouseDdlService(any());
        verify(configurationService).getClusterConfigByClusterId("test_cluster_id");

        assertNotNull(dataRotationService.get());
    }

}
