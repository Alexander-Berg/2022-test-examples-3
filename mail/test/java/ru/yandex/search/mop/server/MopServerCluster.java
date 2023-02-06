package ru.yandex.search.mop.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.junit.Assert;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.search.mop.server.config.MopServerConfigBuilder;
import ru.yandex.search.mop.server.config.database.DatabaseConfigBuilder;
import ru.yandex.search.mop.server.pool.MockConnectionPool;

public class MopServerCluster implements GenericAutoCloseable<IOException> {
    private final MopServer mopServer;
    private final GenericAutoCloseableChain<IOException> chain;

    public MopServerCluster() throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
            new GenericAutoCloseableHolder<>(
                new GenericAutoCloseableChain<>()))
        {
            MopServerConfigBuilder mopServerConfigBuilder =
                new MopServerConfigBuilder();
            new BaseServerConfigBuilder(
                Configs.baseConfig("MopServer")).copyTo(mopServerConfigBuilder);
            DatabaseConfigBuilder databaseConfigBuilder =
                new DatabaseConfigBuilder()
                    .driver("org.postgresql.Driver")
                    .url("jdbc:postgresql://url.net:80/db1?")
                    .user("user")
                    .password("123");
            mopServerConfigBuilder.databaseConfig(databaseConfigBuilder);
            mopServerConfigBuilder.interval(300000000);

            mopServer = new MopServer(mopServerConfigBuilder.build());

            MockConnectionPool mockConnectionPool =
                new MockConnectionPool(databaseConfigBuilder);
            mopServer.connectionPool(mockConnectionPool);

            chain.get().add(mopServer);
            this.chain = chain.release();
        }
    }

    public void start() throws IOException {
        mopServer.start();
    }

    public MopServer mopServer() {
        return mopServer;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public void apply(final String sqlFile) throws SQLException, IOException {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(
                this.getClass().getResourceAsStream(sqlFile),
                StandardCharsets.UTF_8)))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("--") && !line.strip().isEmpty()) {
                    sb.append(line);
                }
            }
            try (Connection connection =
                mopServer.connectionPool().getConnection();
                PreparedStatement statement =
                    connection.prepareStatement(sb.toString()))
            {
                statement.execute();
            }
        }
    }

    public void checkResponse(
        final HttpResponse response,
        final String file,
        final String additional)
        throws IOException, HttpException
    {
        String responseStr = CharsetUtils.toString(response.getEntity());
        String[] responseLines = responseStr.split("\n");
        Arrays.sort(responseLines);
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                this.getClass().getResourceAsStream(file),
                StandardCharsets.UTF_8)))
        {
            String[] additionalArray = new String[0];
            if (additional != null && !additional.isEmpty()) {
                additionalArray = additional.split("\n");
            }
            String[] expectedLines =
                Stream.concat(reader.lines(), Arrays.stream(additionalArray))
                    .sorted()
                    .toArray(String[]::new);
            Assert.assertArrayEquals(expectedLines, responseLines);
        }
    }
}
