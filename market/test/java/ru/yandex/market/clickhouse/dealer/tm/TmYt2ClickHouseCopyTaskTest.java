package ru.yandex.market.clickhouse.dealer.tm;

import com.google.gson.Gson;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 07/06/2018
 */
public class TmYt2ClickHouseCopyTaskTest {

    private static final String PASSWORD = "secret42";

    @Test
    public void testNoPasswordOrDbaasTokenLogging() {
        TmYt2ClickHouseCopyTask task = TmYt2ClickHouseCopyTask.newBuilder()
            .withCredentials("user", PASSWORD)
            .withDbaasToken(PASSWORD)
            .build();

        Assert.assertFalse(task.toString().contains(PASSWORD));
        Assert.assertFalse(task.getCredentials().toString().contains(PASSWORD));
    }

    @Test
    public void mdbClusterNameTest() {
        TmYt2ClickHouseCopyTask task = TmYt2ClickHouseCopyTask.newBuilder()
            .withCredentials("user", PASSWORD)
            .withDbaasToken(null, "dbaas-mdb-token")
            .withDbaasClusterAddress("dbaas-mdb-clickhouse-cluster-id")
            .withLabel("clickhouse-cluster-name")
            .build();


        DocumentContext jsonTask = JsonPath.parse(new Gson().toJson(task));
        Assert.assertThat(jsonTask.read("$.destination_cluster"), is("mdb-clickhouse"));
        Assert.assertThat(jsonTask.read("$.clickhouse_copy_options.labels[0]"), is("clickhouse-cluster-name"));
        Assert.assertThat(jsonTask.read("$.mdb_auth.oauth_token"), is("dbaas-mdb-token"));
        Assert.assertThat(jsonTask.read("$.mdb_cluster_address.cluster_id"), is("dbaas-mdb-clickhouse-cluster-id"));
        Assert.assertFalse(jsonTask.read("$.mdb_auth").toString().contains("organization_id"));
    }
}
