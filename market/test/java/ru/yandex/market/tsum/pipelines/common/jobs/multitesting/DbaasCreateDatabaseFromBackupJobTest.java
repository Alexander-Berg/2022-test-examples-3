package ru.yandex.market.tsum.pipelines.common.jobs.multitesting;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceGrpc;
import yandex.cloud.api.operation.OperationServiceGrpc;
import ru.yandex.market.tsum.clients.iam.YcDatabaseType;
import ru.yandex.market.tsum.clients.iam.YcFolderId;
import ru.yandex.market.tsum.multitesting.MultitestingDbaasService;
import ru.yandex.market.tsum.multitesting.MultitestingPostgresqlDbaasService;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.datasource.DataSourceProperty;
import ru.yandex.market.tsum.pipelines.common.resources.DbaasClusterId;
import ru.yandex.market.tsum.pipelines.common.resources.PipelineEnvironment;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 22/11/2018
 */

@RunWith(MockitoJUnitRunner.class)
public class DbaasCreateDatabaseFromBackupJobTest {

    private static final String HOST_VALUE_TEMPLATE = "${host}/database1";
    private static final String HOSTS_VALUE_TEMPLATE = "${hosts}/database2";
    private static final String PORT_VALUE_TEMPLATE = "${port}";
    private static final String HOSTS_AND_PORTS_VALUE_TEMPLATE = "${hostsAndPorts}/database2";

    private static final String PROPERTY_KEY1 = "mdb.pgaas.host.database1";
    private static final String PROPERTY_KEY2 = "mdb.pgaas.host.database2";
    private static final String PROPERTY_KEY3 = "mdb.pgaas.host.port";
    private static final String PROPERTY_KEY4 = "mdb.pgaas.hosts";

    private static final String ENV_ID = "env-id--test";
    private static final String CLUSTER_ID = "mdb102030303clusterid";
    private static final String BACKUP_ID = "mdb102030303clusterid:mdb102030303backupid";


    @Mock
    private ClusterServiceGrpc.ClusterServiceBlockingStub clusterService;
    @Mock
    private OperationServiceGrpc.OperationServiceBlockingStub operationService;
    private final String ycUrl = "https://fake.url.yandex-team.ru";

    @Mock
    private PipelineEnvironment environment;
    @Mock
    private JobContext jobContext;
    @Mock
    private ResourcesJobContext resourcesJobContext;
    @Spy
    private MultitestingDbaasService dbaasService = new MultitestingPostgresqlDbaasService(clusterService,
        operationService, ycUrl);
    @Spy
    private DbaasCreateDatabaseFromBackupJobConfig config = DbaasCreateDatabaseFromBackupJobConfig.newBuilder()
        .withDatabaseType(YcDatabaseType.POSTGRESQL)
        .withFolderId(YcFolderId.MULTI_TESTING)
        .withSourceClusterId(CLUSTER_ID)
        .withDatasourcePropertyTemplates(
            new DataSourceProperty.Template(
                DataSourceProperty.Type.JAVA,
                DataSourceProperty.COMMON_SECTION,
                PROPERTY_KEY1,
                HOST_VALUE_TEMPLATE
            ), new DataSourceProperty.Template(
                DataSourceProperty.Type.JAVA,
                DataSourceProperty.COMMON_SECTION,
                PROPERTY_KEY2,
                HOSTS_VALUE_TEMPLATE
            ),
            new DataSourceProperty.Template(
                DataSourceProperty.Type.JAVA,
                DataSourceProperty.COMMON_SECTION,
                PROPERTY_KEY3,
                PORT_VALUE_TEMPLATE
            ),
            new DataSourceProperty.Template(
                DataSourceProperty.Type.JAVA,
                DataSourceProperty.COMMON_SECTION,
                PROPERTY_KEY4,
                HOSTS_AND_PORTS_VALUE_TEMPLATE
            )
        )
        .build();

    @InjectMocks
    private DbaasCreateDatabaseFromBackupJob dbaasCreateDatabaseFromBackupJob;

    @Before
    public void setup() {
        Mockito.when(environment.getId()).thenReturn(ENV_ID);
        Mockito.when(jobContext.resources()).thenReturn(resourcesJobContext);
        dbaasCreateDatabaseFromBackupJob.setDbaasService(dbaasService);
    }

    @Test
    public void checkProducedDatasourceResourcesTest() {
        List<String> hosts = ImmutableList.of("superhost.vla.net", "superhost.iva.net");
        int port = 1234;

        String expectedPropertyValue1 = HOST_VALUE_TEMPLATE.replace("${host}", hosts.get(0));
        String expectedPropertyValue2 = HOSTS_VALUE_TEMPLATE.replace("${hosts}", String.join(",", hosts));
        String expectedPropertyValue3 = PORT_VALUE_TEMPLATE.replace("${port}", String.valueOf(port));
        String expectedPropertyValue4 = HOSTS_AND_PORTS_VALUE_TEMPLATE.replace("${hostsAndPorts}",
            String.join(":" + port + ",", hosts) + ":" + port);

        Mockito.doAnswer(invocation -> {
            DataSourceProperty property = invocation.getArgument(0);
            String propertyValue = property.getValue();

            switch (property.getKey()) {
                case PROPERTY_KEY1:
                    Assert.assertEquals(expectedPropertyValue1, propertyValue);
                    break;
                case PROPERTY_KEY2:
                    Assert.assertEquals(expectedPropertyValue2, propertyValue);
                    break;
                case PROPERTY_KEY3:
                    Assert.assertEquals(expectedPropertyValue3, propertyValue);
                    break;
                case PROPERTY_KEY4:
                    Assert.assertEquals(expectedPropertyValue4, propertyValue);
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + property.getKey());
            }
            return null;
        }).when(resourcesJobContext).produce(Mockito.any(DataSourceProperty.class));

        Assertions.assertThatCode(
            () -> DbaasUtils.produceDataSourceResources(jobContext, hosts, port,
                config.getDatasourcePropertyTemplates())
        ).doesNotThrowAnyException();
    }

    @Test
    public void checkExceptionsOfProduceDatasourceResourcesTest() {
        Mockito.when(config.getDatasourcePropertyTemplates()).thenReturn(Collections.emptyList());
        Assertions.assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(
                () -> DbaasUtils.produceDataSourceResources(jobContext, Collections.emptyList(), -1,
                    config.getDatasourcePropertyTemplates())
            );

        Mockito.when(config.getDatasourcePropertyTemplates()).thenCallRealMethod();
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> DbaasUtils.produceDataSourceResources(jobContext, Collections.emptyList(), -1,
                    config.getDatasourcePropertyTemplates())
            );
    }

    @Test
    public void checkProduceDbaasClusterIdResourceTest() {
        Mockito.doAnswer(invocation -> {
            DbaasClusterId dbaasClusterId = invocation.getArgument(0);
            Assert.assertEquals(config.getDatabaseType(), dbaasClusterId.getDatabaseType());
            Assert.assertEquals(dbaasService.getClusterName(ENV_ID), dbaasClusterId.getClusterName());
            return null;
        }).when(resourcesJobContext).produce(Mockito.any(DbaasClusterId.class));

        Assertions.assertThatCode(() -> jobContext.resources().produce(new DbaasClusterId(config.getDatabaseType(),
                DbaasUtils.getClusterName(dbaasService, environment))))
            .doesNotThrowAnyException();
    }


    @Test
    public void getDbaasClusterTest() {
        String clusterName = dbaasService.getClusterName(ENV_ID);
        Mockito.doReturn(Optional.of(CLUSTER_ID))
            .when(dbaasService).getClusterId(clusterName, YcFolderId.MULTI_TESTING);

        Mockito.doNothing().when(dbaasService).checkClusterOperations(CLUSTER_ID);

        Assertions.assertThatCode(
            () -> Assert.assertEquals(
                CLUSTER_ID,
                DbaasUtils.getOrCreateDbaasCluster(
                    dbaasService, clusterName, config.getFolderId(),
                    () -> dbaasCreateDatabaseFromBackupJob.createNewDbaasCluster(jobContext)
                )
            )
        ).doesNotThrowAnyException();
    }

    @Test
    public void createDbaasClusterTest() {
        String clusterName = dbaasService.getClusterName(ENV_ID);
        Mockito.doReturn(Optional.empty()).when(dbaasService).getClusterId(clusterName, YcFolderId.MULTI_TESTING);
        Mockito.doReturn(BACKUP_ID).when(dbaasService).getEarliestBackupId(clusterName, CLUSTER_ID);

        String expectedClusterId = "mdb0000023402clusterId";
        Mockito.doReturn(Optional.of(expectedClusterId))
            .when(dbaasService).createClusterFromBackup(CLUSTER_ID, BACKUP_ID, clusterName,
                YcFolderId.MULTI_TESTING.toString(), jobContext);

        Assertions.assertThatCode(
            () -> Assert.assertEquals(
                expectedClusterId,
                DbaasUtils.getOrCreateDbaasCluster(
                    dbaasService, clusterName, config.getFolderId(),
                    () -> dbaasCreateDatabaseFromBackupJob.createNewDbaasCluster(jobContext)
                )
            )
        ).doesNotThrowAnyException();
    }

    @Test
    public void exceptionsOfCreateDbaasClusterTest() {
        String clusterName = dbaasService.getClusterName(ENV_ID);
        Mockito.doReturn(Optional.empty()).when(dbaasService).getClusterId(clusterName, YcFolderId.MULTI_TESTING);

        Mockito.when(config.getFolderId()).thenReturn(null);

        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> DbaasUtils.getOrCreateDbaasCluster(
                dbaasService, clusterName, config.getFolderId(),
                () -> dbaasCreateDatabaseFromBackupJob.createNewDbaasCluster(jobContext)
            ));

        Mockito.when(config.getFolderId()).thenCallRealMethod();
        Mockito.when(config.getSourceClusterId()).thenReturn(null);

        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> DbaasUtils.getOrCreateDbaasCluster(
                dbaasService, clusterName, config.getFolderId(),
                () -> dbaasCreateDatabaseFromBackupJob.createNewDbaasCluster(jobContext)
            ));
    }
}
