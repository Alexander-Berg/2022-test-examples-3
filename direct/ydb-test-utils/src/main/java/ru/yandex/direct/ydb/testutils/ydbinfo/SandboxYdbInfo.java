package ru.yandex.direct.ydb.testutils.ydbinfo;

import java.io.File;
import java.io.IOException;

import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;
import org.apache.commons.io.FileUtils;

import ru.yandex.direct.ydb.YdbPath;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SandboxYdbInfo implements YdbInfo {
    private YdbPath db;
    private TableClient tableClient;

    @Override
    public void init() {
        this.db = readDb();
        var rpcTransportBuilder = GrpcTransport.forEndpoint(readEndpoint(), db.getPath());
        tableClient = TableClient.newClient(GrpcTableRpc.useTransport(rpcTransportBuilder.build())).build();
    }

    @Override
    public TableClient getClient() {
        return tableClient;
    }

    @Override
    public YdbPath getDb() {
        return db;
    }

    private String readEndpoint() {
        File endpointFile = new File("ydb_endpoint.txt");
        try {
            return FileUtils.readLines(endpointFile, UTF_8).get(0);
        } catch (IOException e) {
            throw new IllegalStateException("Error while read endpoint file");
        }
    }

    private YdbPath readDb() {
        File dbFile = new File("ydb_database.txt");
        try {
            var db = FileUtils.readLines(dbFile, UTF_8).get(0);
            return YdbPath.of(db);
        } catch (IOException e) {
            throw new IllegalStateException("Error while read db file");
        }
    }
}
