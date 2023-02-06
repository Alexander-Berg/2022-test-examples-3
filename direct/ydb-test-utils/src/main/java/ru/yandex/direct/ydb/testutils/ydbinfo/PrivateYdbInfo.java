package ru.yandex.direct.ydb.testutils.ydbinfo;

import com.yandex.ydb.auth.tvm.TvmAuthContext;
import com.yandex.ydb.auth.tvm.YdbClientId;
import com.yandex.ydb.core.auth.AuthProvider;
import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;

import ru.yandex.direct.ydb.YdbPath;

import static ru.yandex.direct.utils.io.FileUtils.expandHome;
import static ru.yandex.direct.utils.io.FileUtils.readToken;

class PrivateYdbInfo implements YdbInfo {
    private static final String YDB_DATABASE = "/ru-prestable/home/ppalex/mydb";
    private static final String YDB_ENDPOINT = "ydb-ru-prestable.yandex.net:2135";

    private TableClient tableClient;

    @Override
    public void init() {
        TvmAuthContext ctx = TvmAuthContext.useTvmApi(2000767, readSecret()); // direct_scripts_test
        AuthProvider authProvider = ctx.authProvider(YdbClientId.YDB);

        GrpcTransport rpcTransport = GrpcTransport
                .forEndpoint(YDB_ENDPOINT, getDb().getPath())
                .withAuthProvider(authProvider)
                .build();

        tableClient = TableClient.newClient(GrpcTableRpc.useTransport(rpcTransport)).build();
    }

    @Override
    public TableClient getClient() {
        return tableClient;
    }

    @Override
    public YdbPath getDb() {
        return YdbPath.of(YDB_DATABASE);
    }

    private static String readSecret() {
        return readToken(expandHome("~/.direct-tokens/tvm2_direct-scripts-test"));
    }
}
