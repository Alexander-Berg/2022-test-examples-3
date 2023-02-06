package ru.yandex.market.health.configs.clickhouse.parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.support.ResourcePropertySource;

import ru.yandex.market.health.configs.clickhouse.config.ClickHouseClusterConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClickHouseClusterParserTest {
    private static final int DEFAULT_HTTPS_PORT = 8443;
    private static final int DEFAULT_HTTP_PORT = 8123;
    private static final int CUSTOM_PORT = 8000;
    private static final String EMPTY_STRING = "";
    private static final String CH_USER_NAME = "logshatter";
    private static final String VAULT_SECRET_ID = "sec-01f9vcrw639fsdedxmw3pcyvzy";
    private static final String VAULT_PASSWORD_KEY = "test.password";
    private static final String NOT_MDB_CLUSTER_ID = "market_health_test";
    private static final String MDB_CLUSTER_ID = "mdbjrnm4dpnl4sbrm68o";
    private static final String CLUSTER_ALIAS = "test";
    private final ClickHouseClusterParser parser = buildClickHouseClusterParser("/test.properties");

    private static ClickHouseClusterParser buildClickHouseClusterParser(String propertiesPath) {
        ResourcePropertySource propertySource = null;
        try {
            propertySource = new ResourcePropertySource(propertiesPath);
        } catch (IOException skip) {
        }
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(propertySource);
        PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        return new ClickHouseClusterParser(propertyResolver::resolveRequiredPlaceholders);
    }

    @Test
    public void testNotCloudClusterConfigWithMultipleShards() throws Exception {
        ClickHouseClusterConfig expectedClusterConfig = buildExpectedClusterConfig(NOT_MDB_CLUSTER_ID,
            NOT_MDB_CLUSTER_ID,
            DEFAULT_HTTP_PORT, EMPTY_STRING, "health-house-test.market.yandex.net", false, false, false);

        ClickHouseClusterConfig actualClusterConfig = parser.parseConfig(readJsonObject("/clusterConfig" +
            "/notCloudClusterConfigDistributed.json"));

        assertEquals(expectedClusterConfig, actualClusterConfig);
    }

    @Test
    public void testNotCloudClusterConfigWithMultipleShardsAndNotUniqueCluster() throws Exception {
        ClickHouseClusterConfig expectedClusterConfig = buildExpectedClusterConfig("market_health_test@1234",
            "market_health_test",
            DEFAULT_HTTP_PORT, EMPTY_STRING, "health-house-test.market.yandex.net", false, false, false);

        ClickHouseClusterConfig actualClusterConfig = parser.parseConfig(readJsonObject("/clusterConfig" +
            "/notCloudClusterConfigDistributedWithNotUniqueCluster.json"));

        assertEquals(expectedClusterConfig, actualClusterConfig);
    }

    @Test
    public void testNotCloudClusterConfigWithSingleHostAndCustomPort() throws Exception {
        ClickHouseClusterConfig expectedClusterConfig = buildExpectedClusterConfig(NOT_MDB_CLUSTER_ID,
            NOT_MDB_CLUSTER_ID,
            CUSTOM_PORT, EMPTY_STRING, "health-house-test.market.yandex.net", false, false, true);

        ClickHouseClusterConfig actualClusterConfig = parser.parseConfig(readJsonObject("/clusterConfig" +
            "/notCloudClusterConfigSingleHostAndCustomPort.json"));

        assertEquals(expectedClusterConfig, actualClusterConfig);
    }

    @Test
    public void testMdbClusterConfig() throws Exception {
        String mdbUrl = "jdbc:clickhouse://sas-ku40lafzbuhdrigu.db.yandex.net:8443,vla-7rncpew7df7bon29.db.yandex" +
            ".net:8443,sas-8jjrjewlhtryf7ja.db.yandex.net:8443,vla-q1766n96gt0bb1iq.db.yandex.net:8443";
        ClickHouseClusterConfig expectedClusterConfig = buildExpectedClusterConfig(MDB_CLUSTER_ID, "cluster_mdb",
            DEFAULT_HTTPS_PORT, mdbUrl, EMPTY_STRING, true, true, false);

        ClickHouseClusterConfig actualClusterConfig = parser.parseConfig(readJsonObject("/clusterConfig" +
            "/mdbClusterConfig.json"));

        assertEquals(expectedClusterConfig, actualClusterConfig);
    }

    @Test
    public void testMdbClusterConfigWithSingleHostAndBalancer() throws Exception {
        String mdbUrl = "jdbc:clickhouse://sas-ku40lafzbuhdrigu.db.yandex.net:8443";
        ClickHouseClusterConfig expectedClusterConfig = buildExpectedClusterConfig(MDB_CLUSTER_ID, "cluster_mdb",
            CUSTOM_PORT, mdbUrl, "balancer.market.yandex.net", true, true, true);

        ClickHouseClusterConfig actualClusterConfig = parser.parseConfig(readJsonObject("/clusterConfig" +
            "/mdbClusterConfigWithSingleHostAndCustomPort.json"));

        assertEquals(expectedClusterConfig, actualClusterConfig);
    }

    @Test
    public void testMdbClusterConfigWithoutMdbFieldThrowsException() {
        Executable executable = () -> parser.parseConfig(readJsonObject("/clusterConfig" +
            "/incorrectMdbClusterConfigWithoutMdbField.json"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("No required parameters 'mdb' in config", illegalArgumentException.getMessage());
    }

    @Test
    public void testMdbClusterConfigWithWrongSingleHostFlagThrowsException() {
        Executable executable = () -> parser.parseConfig(readJsonObject("/clusterConfig" +
            "/incorrectMdbClusterConfigWithSingleHostFlag.json"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Incorrect config. 'singleHost' should be compatible with number of hosts in 'url'",
            illegalArgumentException.getMessage());
    }

    @Test
    public void testMdbClusterConfigWithoutClickHouseUserThrowsException() {
        Executable executable = () -> parser.parseConfig(readJsonObject("/clusterConfig" +
            "/incorrectMdbClusterConfigWithoutUserName.json"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("No required parameter 'clickHouseUser' in config", illegalArgumentException.getMessage());
    }

    @Test
    public void testMdbClusterConfigWithoutClickHousePasswordThrowsException() {
        Executable executable = () -> parser.parseConfig(readJsonObject("/clusterConfig" +
            "/incorrectMdbClusterConfigWithoutPassword.json"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("No required parameters 'clickHousePassword' in config", illegalArgumentException.getMessage());
    }

    @Test
    public void testMdbClusterConfigWithInvalidClusterNameThrowsException() {
        Executable executable = () -> parser.parseConfig(readJsonObject("/clusterConfig" +
            "/incorrectMdbClusterConfigWithInvalidClusterName.json"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Incorrect clusterName value: 12-cluster_mdb", illegalArgumentException.getMessage());
    }

    @Test
    public void testNotCloudClusterConfigWithInvalidClusterNameThrowsException() {
        Executable executable = () -> parser.parseConfig(readJsonObject("/clusterConfig" +
            "/incorrectNotCloudClusterConfigWithInvalidClusterName.json"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Incorrect clickHouseClusterId value: market_health_test-fn)",
            illegalArgumentException.getMessage());
    }

    @Test
    public void testCloudClusterConfigWithoutClusterAliasThrowsException() {
        Executable executable = () -> parser.parseConfig(readJsonObject("/clusterConfig" +
            "/incorrectMdbClusterConfigWithoutClusterAlias.json"));

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("No required parameter 'clusterAlias' in config", illegalArgumentException.getMessage());
    }

    @Test
    public void testMdbClusterConfigWithPlaceholders() throws Exception {
        String mdbUrl = "jdbc:clickhouse://sas-ku40lafzbuhdrigu.db.yandex.net:8443,vla-7rncpew7df7bon29.db.yandex" +
            ".net:8443,sas-8jjrjewlhtryf7ja.db.yandex.net:8443,vla-q1766n96gt0bb1iq.db.yandex.net:8443";
        ClickHouseClusterConfig expectedClusterConfig = buildExpectedClusterConfig(MDB_CLUSTER_ID, "cluster_mdb",
            DEFAULT_HTTPS_PORT, mdbUrl, EMPTY_STRING, true, true, false);

        ClickHouseClusterConfig actualClusterConfig = parser.parseConfig(readJsonObject("/clusterConfig" +
            "/mdbClusterConfigWithPlaceholders.json"));

        assertEquals(expectedClusterConfig, actualClusterConfig);
    }

    private JsonObject readJsonObject(String path) throws Exception {
        try (Reader confReader = new InputStreamReader(getClass().getResourceAsStream(path))) {
            return new Gson().fromJson(confReader, JsonObject.class);
        }
    }

    @SuppressWarnings("ParameterNumber")
    private ClickHouseClusterConfig buildExpectedClusterConfig(String clusterId, String clusterName, int port,
                                                               String mdbUrl,
                                                               String host, boolean isMdb, boolean isSsl,
                                                               boolean isSingleHost) {
        return ClickHouseClusterConfig.newBuilder()
            .setClickHouseClusterId(clusterId)
            .setClickHouseClusterName(clusterName)
            .setClickHousePort(port)
            .setClickHouseUser(CH_USER_NAME)
            .setClickHouseHost(host)
            .setMdbUrl(mdbUrl)
            .setMdb(isMdb)
            .setVaultSecretId(VAULT_SECRET_ID)
            .setVaultPasswordKey(VAULT_PASSWORD_KEY)
            .setClusterAlias(CLUSTER_ALIAS)
            .setSsl(isSsl)
            .setSingleHost(isSingleHost)
            .build();
    }

}
