package ru.yandex.market.tsum.multitesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.rpc.Status;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import yandex.cloud.api.iam.v1.IamTokenServiceGrpc;
import yandex.cloud.api.mdb.postgresql.v1.BackupOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.ClusterOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceGrpc;
import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceOuterClass;
import yandex.cloud.api.operation.OperationOuterClass;
import yandex.cloud.api.operation.OperationServiceGrpc;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.clients.iam.IamClientAuthInterceptor;
import ru.yandex.market.tsum.clients.iam.YcDatabaseType;
import ru.yandex.market.tsum.grpc.trace.TraceClientInterceptor;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl;
import ru.yandex.market.tsum.pipelines.common.jobs.multitesting.CreateMultitestingDatabaseConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.multitesting.DbaasUtils;

import static ru.yandex.market.tsum.clients.iam.YcFolderId.MULTI_TESTING;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 15/11/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class MultitestingDbaasServiceTest {

    private static final String DEFAULT_CLUSTER_NAME = "clusterNameTest";
    private static final String DEFAULT_CLUSTER_ID = "mdb1230321232clusterId2";
    private static final String SOURCE_CLUSTER_ID = "mdb1230321232clusterId1";
    private static final String DEFAULT_FOLDER_ID = MULTI_TESTING.toString();

    private static final String THE_EARLIEST_BACKUP_ID = "mdb00000001backupId";
    private static final String DEFAULT_NETWORK_ID = "";
    private static final List<String> GEOS = ImmutableList.of("vla", "sas");

    @Mock
    private ClusterServiceGrpc.ClusterServiceBlockingStub clusterService;
    @Mock
    private OperationServiceGrpc.OperationServiceBlockingStub operationService;
    private String ycUrl = "https://fake.url.yandex-team.ru";

    @Mock
    private JobContext jobContext;

    @Spy
    @InjectMocks
    private MultitestingPostgresqlDbaasService dbaasService;


    @Before
    public void setup() {
        ClusterServiceOuterClass.ListClustersResponse listClustersResponse = Mockito.mock(
            ClusterServiceOuterClass.ListClustersResponse.class
        );

        ClusterServiceOuterClass.ListClusterBackupsResponse listBackupsResponse = Mockito.mock(
            ClusterServiceOuterClass.ListClusterBackupsResponse.class
        );

        ClusterServiceOuterClass.ListClusterHostsResponse listHostsResponse = Mockito.mock(
            ClusterServiceOuterClass.ListClusterHostsResponse.class
        );

        Mockito.when(clusterService.list(Mockito.any())).thenReturn(listClustersResponse);
        Mockito.when(listClustersResponse.getClustersList())
            .thenReturn(Arrays.asList(
                getCluster(DEFAULT_CLUSTER_ID, DEFAULT_CLUSTER_NAME),
                getCluster("stub_id", "stub_name")
            ));

        Mockito.when(clusterService.listBackups(Mockito.any())).thenReturn(listBackupsResponse);
        Mockito.when(listBackupsResponse.getBackupsList())
            .thenReturn(Arrays.asList(
                getBackup("stub_id_1", 100_000_002),
                getBackup(THE_EARLIEST_BACKUP_ID, 100_000_001),
                getBackup("stub_id_1", 100_000_003)
            ));

        Mockito.when(clusterService.listHosts(Mockito.any())).thenReturn(listHostsResponse);
        Mockito.when(listHostsResponse.getHostsList())
            .thenReturn(Arrays.asList(
                getHost(GEOS.get(0)),
                getHost(GEOS.get(1))
            ));

        Mockito.when(jobContext.progress()).thenReturn(Mockito.mock(JobProgressContextImpl.class));
    }

    private ClusterOuterClass.Host getHost(String zoneId) {
        return ClusterOuterClass.Host.newBuilder()
            .setZoneId(zoneId)
            .build();
    }

    private BackupOuterClass.Backup getBackup(String id, int nanos) {
        return BackupOuterClass.Backup.newBuilder()
            .setId(id)
            .setStartedAt(Timestamp.newBuilder()
                .setNanos(nanos)
                .build()
            ).build();
    }

    @Test
    public void clusterFoundTest() {
        Optional<String> clusterId = dbaasService.getClusterId(DEFAULT_CLUSTER_NAME, MULTI_TESTING);
        Assert.assertTrue(clusterId.isPresent());
        clusterId.ifPresent(cId -> Assert.assertEquals(DEFAULT_CLUSTER_ID, cId));
    }

    @Test
    public void clusterNotFoundTest() {
        Assertions.assertThatCode(() -> {
            Optional<String> clusterId = dbaasService.getClusterId("no-name-cluster", MULTI_TESTING);
            Assert.assertFalse(clusterId.isPresent());
        }).doesNotThrowAnyException();
    }

    private ClusterOuterClass.Cluster getCluster(String id, String name) {
        return ClusterOuterClass.Cluster.newBuilder()
            .setName(name)
            .setId(id)
            .build();
    }

    @Test
    public void removeClustersProvideTheSameOperationTest() {
        Assertions.assertThatCode(() -> {
            OperationOuterClass.Operation operation = dbaasService.removeCluster(
                DEFAULT_CLUSTER_ID, this::getOkOperation
            );
            Assert.assertEquals(getOkOperation(), operation);
        }).doesNotThrowAnyException();
    }

    private OperationOuterClass.Operation getFailedOperation() {
        return OperationOuterClass.Operation.newBuilder()
            .setDone(false)
            .setError(Status.newBuilder()
                .setCode(100500)
                .setMessage("Operation was failed for some reason")
                .build()
            )
            .setId("mdb1231231operationId")
            .build();
    }

    private OperationOuterClass.Operation getOkOperation() {
        return OperationOuterClass.Operation.newBuilder()
            .setDone(true)
            .setId("mdb1231232operationId")
            .build();
    }

    @Test
    public void getEarliestBackupTest() {
        Assert.assertEquals(
            THE_EARLIEST_BACKUP_ID, dbaasService.getEarliestBackupId(DEFAULT_CLUSTER_NAME, DEFAULT_CLUSTER_ID)
        );
    }

    @Test
    public void noBackupsTest() {
        ClusterServiceOuterClass.ListClusterBackupsResponse listBackupsResponse = Mockito.mock(
            ClusterServiceOuterClass.ListClusterBackupsResponse.class
        );
        Mockito.when(clusterService.listBackups(Mockito.any())).thenReturn(listBackupsResponse);
        Mockito.when(listBackupsResponse.getBackupsList()).thenReturn(Collections.emptyList());

        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> dbaasService.getEarliestBackupId(DEFAULT_CLUSTER_NAME, DEFAULT_CLUSTER_ID));
    }

    @Test
    public void databaseNameGenerationTest() {
        String envId = "testEnvId--name";
        String clusterName = dbaasService.getClusterName(envId);

        Assert.assertFalse(clusterName.contains(envId));
        Assert.assertTrue(clusterName.contains(getPreparedEnvId(envId)));
        Assert.assertEquals(
            String.format(MultitestingDbaasService.CLUSTER_NAME_TEMPLATE, getPreparedEnvId(envId)), clusterName
        );
    }

    private String getPreparedEnvId(String envId) {
        return envId.replace("-", "_");
    }

    @Test
    public void getClusterGeoTest() {
        Assert.assertEquals(GEOS, dbaasService.getClusterGeo(DEFAULT_CLUSTER_ID));
        Assert.assertNotEquals(Collections.emptyList(), dbaasService.getClusterGeo(DEFAULT_CLUSTER_ID));
    }

    @Test
    public void createClusterTest() throws InvalidProtocolBufferException {
        Mockito.when(clusterService.get(Mockito.any())).thenReturn(getCluster(SOURCE_CLUSTER_ID, DEFAULT_CLUSTER_NAME));
        OperationOuterClass.Operation operation = Mockito.mock(OperationOuterClass.Operation.class);
        Any any = Mockito.mock(Any.class);
        ClusterServiceOuterClass.RestoreClusterMetadata meta = Mockito.mock(
            ClusterServiceOuterClass.RestoreClusterMetadata.class
        );

        Mockito.doAnswer(
            invocation -> {
                ClusterServiceOuterClass.RestoreClusterRequest request = invocation.getArgument(0);
                Assert.assertEquals(THE_EARLIEST_BACKUP_ID, request.getBackupId());
                Assert.assertEquals(ClusterOuterClass.Cluster.Environment.PRODUCTION, request.getEnvironment());
                Assert.assertEquals(DEFAULT_CLUSTER_NAME, request.getName());
                Assert.assertEquals(DEFAULT_NETWORK_ID, request.getNetworkId());
                return operation;
            }
        ).when(clusterService).restore(Mockito.any());

        Mockito.doNothing().when(dbaasService).waitForDbaasOperationToFinish(operation);
        Mockito.when(operation.getMetadata()).thenReturn(any);
        Mockito.when(any.is(ClusterServiceOuterClass.RestoreClusterMetadata.class)).thenReturn(true);
        Mockito.when(any.unpack(ClusterServiceOuterClass.RestoreClusterMetadata.class)).thenReturn(meta);
        Mockito.when(meta.getClusterId()).thenReturn(DEFAULT_CLUSTER_ID);

        Optional<String> clusterFromBackup = dbaasService.createClusterFromBackup(
            SOURCE_CLUSTER_ID, THE_EARLIEST_BACKUP_ID, DEFAULT_CLUSTER_NAME, DEFAULT_FOLDER_ID, jobContext
        );
        Assert.assertTrue(clusterFromBackup.isPresent());
        clusterFromBackup.ifPresent(cId -> Assert.assertEquals(DEFAULT_CLUSTER_ID, cId));
    }

    @Test
    @Ignore
    public void integrationCreateDbTest() {
        Channel channelForIamToken = ManagedChannelBuilder.forTarget("gw.db.yandex-team.ru")
            .intercept(new TraceClientInterceptor(Module.IAM_YANDEX_CLOUD))
            .build();
        IamTokenServiceGrpc.IamTokenServiceBlockingStub iamClient =
            IamTokenServiceGrpc.newBlockingStub(channelForIamToken);

        ManagedChannel channel = ManagedChannelBuilder.forTarget("gw.db.yandex-team.ru")
            .intercept(
                new TraceClientInterceptor(Module.DBAAS),
                new IamClientAuthInterceptor("put-your-token-here", iamClient))
            .build();

        MultitestingPostgresqlDbaasService service = new MultitestingPostgresqlDbaasService(
            yandex.cloud.api.mdb.postgresql.v1.ClusterServiceGrpc.newBlockingStub(channel),
            OperationServiceGrpc.newBlockingStub(channel),
            ycUrl
        );

        String cluster = service.createCluster(
            "test",
            CreateMultitestingDatabaseConfig.newBuilder()
                .withDbName("test")
                .withUserName("test")
                .withUserPassword("test")
                .withZoneIds(Collections.singletonList(DbaasUtils.ZoneId.VLA))
                .withDatabaseType(YcDatabaseType.POSTGRESQL)
                .build()
        ).get();

        System.out.println(cluster);
        System.out.println("printing null: " + null);
    }
}
