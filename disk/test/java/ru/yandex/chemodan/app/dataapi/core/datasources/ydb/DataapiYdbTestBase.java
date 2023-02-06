package ru.yandex.chemodan.app.dataapi.core.datasources.ydb;

import java.util.concurrent.ExecutionException;

import com.yandex.ydb.core.auth.TokenAuthProvider;
import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.core.rpc.RpcTransport;
import com.yandex.ydb.scheme.SchemeOperationProtos;
import com.yandex.ydb.table.SchemeClient;
import com.yandex.ydb.table.Session;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.rpc.grpc.GrpcSchemeRpc;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;
import org.junit.After;
import org.junit.Before;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.dao.support.DataApiRandomValueGenerator;
import ru.yandex.chemodan.app.dataapi.core.datasources.ydb.dao.DataRecordsYdbDao;
import ru.yandex.chemodan.app.dataapi.core.datasources.ydb.dao.DatabasesYdbDao;
import ru.yandex.chemodan.app.dataapi.core.datasources.ydb.dao.DeletedDatabasesYdbDao;
import ru.yandex.chemodan.app.dataapi.core.datasources.ydb.dao.DeltasYdbDao;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.chemodan.ydb.dao.OneTableYdbDao;
import ru.yandex.chemodan.ydb.dao.ThreadLocalYdbTransactionManager;
import ru.yandex.chemodan.ydb.dao.YdbTimeoutSettings;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.devtools.test.YaTest;
import ru.yandex.misc.ExceptionUtils;

/**
 * @author tolmalev
 */
public class DataapiYdbTestBase {
    protected ThreadLocalYdbTransactionManager transactionManager;
    protected DataApiUserId uid;
    protected RpcTransport transport;
    protected TableClient tableClient;
    protected SchemeClient schemeClient;

    protected String database;
    protected String endpoint;

    static {
        TestHelper.initialize();
    }

    protected DataApiUserId randomUser() {
        return new DataApiRandomValueGenerator().createDataApiUserId(Option.empty());
    }

    @Before
    public void init() throws ExecutionException, InterruptedException {
        if (YaTest.insideYaTest) {
            endpoint = System.getenv("YDB_ENDPOINT");
            database = System.getenv("YDB_DATABASE");

            transport = GrpcTransport.forEndpoint(endpoint, database).build();
        } else {
            String user = System.getenv("USER");

            endpoint = "ydb-ru-prestable.yandex.net:2135";
            database = "/ru-prestable/home/" + user + "/mydb";

            String token = System.getenv("YDB_TOKEN");
            if (token == null) {
                throw new IllegalStateException("You should set env variable YDB_TOKEN to run tests");
            }
            transport = GrpcTransport.forEndpoint(endpoint, database)
                    .withAuthProvider(new TokenAuthProvider(token))
                    .build();
        }

        tableClient = TableClient.newClient(GrpcTableRpc.useTransport(transport))
                .sessionPoolSize(10, 100)
                .build();

        schemeClient = SchemeClient.newClient(GrpcSchemeRpc.useTransport(transport))
                .build();

        dropAllTables();
        createAllTables();

        uid = randomUser();
        transactionManager = new ThreadLocalYdbTransactionManager(tableClient, YdbTestUtils.getTestTimeoutSettings());
    }

    @After
    public void after() {
        checkAllSessionsIdle(transactionManager.getTableClientForTests());
        schemeClient.close();
        tableClient.close();
        transport.close();
    }

    public void createAllTables() throws ExecutionException, InterruptedException {
        YdbTimeoutSettings timeoutSettings = YdbTestUtils.getTestTimeoutSettings();
        ThreadLocalYdbTransactionManager transactionManager = new ThreadLocalYdbTransactionManager(tableClient, timeoutSettings);
        ListF<OneTableYdbDao> daos = Cf.list(
                new DatabasesYdbDao(transactionManager),
                new DeletedDatabasesYdbDao(transactionManager),
                new DataRecordsYdbDao(transactionManager),
                new DeltasYdbDao(transactionManager)
        );

        for (OneTableYdbDao dao : daos) {
            getSession().createTable(database + "/" + dao.getTableName(), dao.getTableDescription()).get()
                    .expect("Failed to create table");
        }
    }

    public void dropAllTables() throws ExecutionException, InterruptedException {
        Session session = getSession();

        schemeClient.listDirectory(database).get().expect("").getChildren().forEach(entty -> {
            if (entty.getType() == SchemeOperationProtos.Entry.Type.TABLE) {
                try {
                    session.dropTable(database + "/" + entty.getName()).get().expect("Failed to drop table");
                } catch (Exception e) {
                    throw ExceptionUtils.translate(e);
                }
            }
        });
    }

    protected Session getSession() throws InterruptedException, ExecutionException {
        return tableClient.createSession().get().expect("Can't create session");
    }

    protected void checkAllSessionsIdle(TableClient tableClient) {
        // please rely only on public interface
        //Object sessionPool = ReflectionUtils.getField(tableClient, "sessionPool");
        //Object idleSessions = ReflectionUtils.getField(sessionPool, "idleSessions");
        //for (Object idleSession : (ArrayList) idleSessions) {
        //    Object state = ReflectionUtils.getField(idleSession, "state");
        //    Assert.equals("IDLE", state.toString(), "Found not idle session!");
        //}
    }
}
