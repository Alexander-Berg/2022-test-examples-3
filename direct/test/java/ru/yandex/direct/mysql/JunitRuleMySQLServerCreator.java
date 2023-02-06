package ru.yandex.direct.mysql;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.rules.ExternalResource;

@ParametersAreNonnullByDefault
public class JunitRuleMySQLServerCreator extends ExternalResource {
    private Collection<TmpMySQLServerWithDataDir> servers = new ArrayList<>();


    @Override
    protected void after() {
        for (TmpMySQLServerWithDataDir server : servers) {
            server.close();
        }
    }

    public TmpMySQLServerWithDataDir create(String name, MySQLServerBuilder builder)
            throws InterruptedException {
        TmpMySQLServerWithDataDir server = TmpMySQLServerWithDataDir.create(name, builder);
        servers.add(server);
        return server;
    }

    public TmpMySQLServerWithDataDir createWithBinlog(
            String name, MySQLServerBuilder builder, MysqlBinlogRowImage rowImage
    ) throws InterruptedException {
        TmpMySQLServerWithDataDir server = TmpMySQLServerWithDataDir.createWithBinlog(name, builder, rowImage);
        servers.add(server);
        return server;
    }

    public TmpMySQLServerWithDataDir createWithBinlog(String name, MySQLServerBuilder builder)
            throws InterruptedException {
        return createWithBinlog(name, builder, MysqlBinlogRowImage.NOBLOB);
    }
}
