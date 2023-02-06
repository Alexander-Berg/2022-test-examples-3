package ru.yandex.market.tsum.clients.dbaas;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int64Value;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import yandex.cloud.api.iam.v1.IamTokenServiceGrpc;
import yandex.cloud.api.iam.v1.IamTokenServiceOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.ClusterOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceGrpc;
import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.DatabaseOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.UserOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.config.Postgresql13;
import yandex.cloud.api.operation.OperationOuterClass;
import yandex.cloud.api.resourcemanager.v1.FolderOuterClass;
import yandex.cloud.api.resourcemanager.v1.FolderServiceGrpc;
import yandex.cloud.api.resourcemanager.v1.FolderServiceOuterClass;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.clients.iam.IamClientAuthInterceptor;
import ru.yandex.market.tsum.grpc.trace.TraceClientInterceptor;

@Ignore
public class GrpcMdbClientTest {
    private static final String IAM_GRPC_URL = "gw.db.yandex-team.ru";
    private static final String OAUTH_TOKEN = "...";

    private IamTokenServiceGrpc.IamTokenServiceBlockingStub getIamClient() {
        Channel channel = NettyChannelBuilder.forTarget(IAM_GRPC_URL)
            .intercept(new TraceClientInterceptor(Module.IAM_YANDEX_CLOUD))
            .build();
        return IamTokenServiceGrpc.newBlockingStub(channel);
    }

    private String getToken() {
        IamTokenServiceGrpc.IamTokenServiceBlockingStub iamClient = getIamClient();
        IamTokenServiceOuterClass.CreateIamTokenRequest iamTokenRequest = IamTokenServiceOuterClass.
            CreateIamTokenRequest.newBuilder().setYandexPassportOauthToken(OAUTH_TOKEN).build();
        return iamClient.create(iamTokenRequest).getIamToken();
    }

    @Test
    public void testIamClient() {
        Assert.assertTrue(!Strings.isNullOrEmpty(getToken()));
    }

    @Test
    public void testMdbClient() {
        Channel channel = NettyChannelBuilder.forTarget(IAM_GRPC_URL)
            .intercept(new TraceClientInterceptor(Module.IAM_YANDEX_CLOUD), new IamClientAuthInterceptor(OAUTH_TOKEN,
                getIamClient()))
            .build();
        ClusterServiceGrpc.ClusterServiceBlockingStub clusterClient = ClusterServiceGrpc.newBlockingStub(channel);
        ClusterServiceOuterClass.ListClustersRequest request =
            ClusterServiceOuterClass.ListClustersRequest.newBuilder().setFolderId("foo4b5i8ioau23qpc3bu").build();
        List<ClusterOuterClass.Cluster> clustersList = clusterClient.list(request).getClustersList();
        Assert.assertTrue(clustersList.size() > 0);

    }

    @Test
    public void createPostgresRequest() {
        ClusterOuterClass.Resources.Builder resources = ClusterOuterClass.Resources.newBuilder().setResourcePresetId(
            "s2.nano").setDiskSize(10L << 30).setDiskTypeId("local-ssd");
        ClusterOuterClass.ConnectionPoolerConfig.Builder poolingMode =
            ClusterOuterClass.ConnectionPoolerConfig.newBuilder()
                .setPoolingMode(ClusterOuterClass.ConnectionPoolerConfig.PoolingMode.SESSION);

        Postgresql13.PostgresqlConfig13.Builder postgresConfig =
            Postgresql13.PostgresqlConfig13.newBuilder().setMaxConnections(Int64Value.newBuilder().setValue(30));
        DatabaseOuterClass.DatabaseSpec.Builder dbSpec = DatabaseOuterClass.DatabaseSpec.newBuilder()
            .setOwner("kemsta")
            .setName("kemsta_db");
        UserOuterClass.UserSpec.Builder userSpec = UserOuterClass.UserSpec.newBuilder()
            .setName("kemsta")
            .setConnLimit(Int64Value.newBuilder().setValue(10).build())
            .setPassword("12312341241");
        ClusterServiceOuterClass.HostSpec.Builder sas = ClusterServiceOuterClass.HostSpec.newBuilder()
            .setZoneId("sas");
        ClusterServiceOuterClass.HostSpec.Builder sas2 = ClusterServiceOuterClass.HostSpec.newBuilder()
            .setZoneId("sas");
        ClusterServiceOuterClass.HostSpec.Builder sas3 = ClusterServiceOuterClass.HostSpec.newBuilder()
            .setZoneId("sas");

        ClusterServiceOuterClass.ConfigSpec.Builder configSpec = ClusterServiceOuterClass.ConfigSpec.newBuilder()
            .setVersion("13")
            .setPoolerConfig(poolingMode)
            .setResources(resources)
            .setAutofailover(BoolValue.newBuilder().setValue(true))
            .setPostgresqlConfig13(postgresConfig);
        ClusterServiceOuterClass.CreateClusterRequest.Builder createRequest =
            ClusterServiceOuterClass.CreateClusterRequest.newBuilder()
                .setFolderId("mdb-junk")
                .setName("kemsta_db")
                .putLabels("tsum", "some_id")
                .setEnvironment(ClusterOuterClass.Cluster.Environment.PRESTABLE)
                .setConfigSpec(configSpec)
                .addDatabaseSpecs(dbSpec)
                .addUserSpecs(userSpec)
                .addHostSpecs(sas2)
                .addHostSpecs(sas3)
                .addHostSpecs(sas)
                .setNetworkId("");
        Channel channel = NettyChannelBuilder.forTarget(IAM_GRPC_URL)
            .intercept(new TraceClientInterceptor(Module.IAM_YANDEX_CLOUD), new IamClientAuthInterceptor(OAUTH_TOKEN,
                getIamClient()))
            .build();
        ClusterServiceGrpc.ClusterServiceBlockingStub clusterClient = ClusterServiceGrpc.newBlockingStub(channel);
        OperationOuterClass.Operation operation = clusterClient.create(createRequest.build());
        System.out.println(operation);
    }

    @Test
    public void createClickhouseRequest() {
        yandex.cloud.api.mdb.clickhouse.v1.ClusterOuterClass.Resources.Builder resources =
            yandex.cloud.api.mdb.clickhouse.v1.ClusterOuterClass.Resources.newBuilder().setResourcePresetId("s2.nano")
                .setDiskSize(10L << 30).setDiskTypeId("local-ssd");
        yandex.cloud.api.mdb.clickhouse.v1.DatabaseOuterClass.DatabaseSpec.Builder dbSpec =
            yandex.cloud.api.mdb.clickhouse.v1.DatabaseOuterClass.DatabaseSpec.newBuilder()
                .setName("kemsta_ch");
        yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.ConfigSpec.Clickhouse.Builder ch =
            yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.ConfigSpec.Clickhouse.newBuilder()
                .setResources(resources);
        yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.ConfigSpec.Builder configSpec =
            yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.ConfigSpec.newBuilder()
                .setClickhouse(ch);
        yandex.cloud.api.mdb.clickhouse.v1.UserOuterClass.UserSpec.Builder userSpec =
            yandex.cloud.api.mdb.clickhouse.v1.UserOuterClass.UserSpec.newBuilder()
                .setName("kemsta")
                .setPassword("123123123");
        yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.HostSpec.Builder sas1 =
            yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.HostSpec.newBuilder()
                .setZoneId("sas")
                .setType(yandex.cloud.api.mdb.clickhouse.v1.ClusterOuterClass.Host.Type.CLICKHOUSE);
        yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.HostSpec.Builder sas2 =
            yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.HostSpec.newBuilder()
                .setZoneId("sas")
                .setType(yandex.cloud.api.mdb.clickhouse.v1.ClusterOuterClass.Host.Type.CLICKHOUSE);
        yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.CreateClusterRequest.Builder createRequest =
            yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass.CreateClusterRequest.newBuilder()
                .setFolderId("mdb-junk")
                .setName("kemsta_ch")
                .setConfigSpec(configSpec)
                .addDatabaseSpecs(dbSpec)
                .setEnvironment(yandex.cloud.api.mdb.clickhouse.v1.ClusterOuterClass.Cluster.Environment.PRESTABLE)
                .addUserSpecs(userSpec)
                .addHostSpecs(sas1)
                .addHostSpecs(sas2)
                .setNetworkId("");

        Channel channel = NettyChannelBuilder.forTarget(IAM_GRPC_URL)
            .intercept(new TraceClientInterceptor(Module.IAM_YANDEX_CLOUD), new IamClientAuthInterceptor(OAUTH_TOKEN,
                getIamClient()))
            .build();
        yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceGrpc.ClusterServiceBlockingStub clusterService =
            yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceGrpc.newBlockingStub(channel);
        OperationOuterClass.Operation operation = clusterService.create(createRequest.build());
        System.out.println(operation);
    }


    public List<FolderOuterClass.Folder> getFolders() {
        Channel channel = NettyChannelBuilder.forTarget(IAM_GRPC_URL)
            .intercept(new TraceClientInterceptor(Module.IAM_YANDEX_CLOUD), new IamClientAuthInterceptor(OAUTH_TOKEN,
                getIamClient()))
            .build();
        final FolderServiceGrpc.FolderServiceBlockingStub folderService = FolderServiceGrpc.newBlockingStub(channel);
        FolderServiceOuterClass.ListFoldersResponse listFoldersResponse =
            folderService.list(FolderServiceOuterClass.ListFoldersRequest.newBuilder().setCloudId(
                "fooarnpv45hhmd3pogch").build());
        ArrayList<FolderOuterClass.Folder> folders = new ArrayList<>();
        while (listFoldersResponse.getFoldersCount() > 0) {
            folders.addAll(listFoldersResponse.getFoldersList());
            listFoldersResponse =
                folderService.list(FolderServiceOuterClass.ListFoldersRequest.newBuilder().setCloudId(
                    "fooarnpv45hhmd3pogch").setPageToken(listFoldersResponse.getNextPageToken()).build());
        }
        return folders;
    }

    @Test
    public void getPgClusters() {
        Channel channel = NettyChannelBuilder.forTarget(IAM_GRPC_URL)
            .intercept(new TraceClientInterceptor(Module.IAM_YANDEX_CLOUD), new IamClientAuthInterceptor(OAUTH_TOKEN,
                getIamClient()))
            .build();
        ClusterServiceGrpc.ClusterServiceBlockingStub clusterClient = ClusterServiceGrpc.newBlockingStub(channel);
        final List<ClusterOuterClass.Cluster> clusters =
            getFolders().stream().map((folder) -> getPgClustersForFolder(clusterClient, folder.getId()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        System.out.println(clusters);
    }

    public List<ClusterOuterClass.Cluster> getPgClustersForFolder(
        ClusterServiceGrpc.ClusterServiceBlockingStub client, String folderId
    ) {
        ArrayList<ClusterOuterClass.Cluster> clusters = new ArrayList<>();
        String nextPageToken = "";
        ClusterServiceOuterClass.ListClustersRequest.Builder request;
        ClusterServiceOuterClass.ListClustersResponse response;
        while (true) {
            request = ClusterServiceOuterClass.ListClustersRequest.newBuilder()
                .setPageToken(nextPageToken)
                .setFolderId(folderId);
            response = client.list(request.setPageToken(nextPageToken).build());
            clusters.addAll(response.getClustersList());
            nextPageToken = response.getNextPageToken();
            if (Strings.isNullOrEmpty(nextPageToken)) {
                return clusters;
            }
        }
    }
}

