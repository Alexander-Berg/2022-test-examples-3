package ru.yandex.iex.proxy;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.iex.proxy.complaints.ImmutableComplaintsConfig;
import ru.yandex.iex.proxy.complaints.Route;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.TestBase;

public class RulesDictUpdateTest extends TestBase {
    private static final String HTTP_LOCALHOST = "http://localhost:";
    //private static final int BUFFER_SIZE = 32768;

    @Test
    public void testRulesDictLoading() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this, true, true)) {
            cluster.iexproxy().start();

            checkRulesDictionaries(cluster, 10183, 8162, 10407);
        }
    }

    @Test
    public void testRulesDictReloading() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this, true, true)) {
            cluster.iexproxy().start();
            final ImmutableComplaintsConfig complaintsConfig = cluster.iexproxy().config().complaintsConfig();
            final Map<Route, File> rulesDictFiles = complaintsConfig.rulesDictFiles();
            final Map<Route, String> appendRows = Map.of(
                Route.IN, "10184\tHBF_MUSIC_STABLE_BACK\t1\t1\n10185\tHBF_MAILNETS\t1\t1",
                Route.OUT, "8163\tPDF_ATT_MIX_A\t1\t0\n8164\tPDF_ATT_NAME_A\t1\t0",
                Route.CORP, "10408\tHBF_MAILNETS\t1\t1\n10409\tHBF_PASSPORT\t1\t1"
            );
            for (final Route route : Route.values()) {
                File f = File.createTempFile("rules_dict_" + route.name(), "");
                Files.copy(rulesDictFiles.get(route).toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.write(
                    f.toPath(),
                    appendRows.get(route).getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                rulesDictFiles.put(route, f);
            }
            QueryConstructor query =
                new QueryConstructor(
                    new StringBuilder(HTTP_LOCALHOST + cluster.iexproxy().port() + "/load-rules-dict"));
            HttpGet get = new HttpGet(query.toString());
            try (CloseableHttpClient client = Configs.createDefaultClient()) {
                try (CloseableHttpResponse httpResponse = client.execute(get)) {
                    logger.info("testRulesDictReloading response: " + httpResponse.getStatusLine().getStatusCode());
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
                    checkRulesDictionaries(cluster, 10185, 8164, 10409);
                }
            }
        }
    }

    private static void checkRulesDictionaries(
        final IexProxyCluster cluster,
        final int rulesInCount,
        final int rulesOutCount,
        final int rulesCorpCount)
    {
        final ImmutableComplaintsConfig complaintsConfig = cluster.iexproxy().config().complaintsConfig();
        Assert.assertNotNull(complaintsConfig.rulesDictFiles());
        Assert.assertEquals(3, complaintsConfig.rulesDictFiles().size());

        final Map<Route, Map<Integer, String>> rulesDictionaries =
                complaintsConfig.rulesDictionaries();
        Assert.assertNotNull(rulesDictionaries);
        Assert.assertEquals(3, rulesDictionaries.size());
        Assert.assertEquals(rulesInCount, rulesDictionaries.get(Route.IN).size());
        Assert.assertEquals(rulesOutCount, rulesDictionaries.get(Route.OUT).size());
        Assert.assertEquals(rulesCorpCount, rulesDictionaries.get(Route.CORP).size());
    }
}
