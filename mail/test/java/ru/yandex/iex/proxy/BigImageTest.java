package ru.yandex.iex.proxy;

import java.io.File;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.TestBase;

public class BigImageTest extends TestBase {
    private static final String LOCALHOST = "http://localhost:";
    private static final Long UID = 588355978L;
    private static final String MID = "173670060630475394";

    @Test
    public void testBigImage() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );

            String blackboxUri = IexProxyCluster.blackboxUri("&uid=" + UID);
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID, "test@yandex.ru", "1"));

            FileEntity tikaiteJson = new FileEntity(
                new File(getClass().getResource(
                    "./bigimage/tikaite_response.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.tikaite().add(
                "/headers?json-type=dollar&stid=*",
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson));

            cluster.cokemulatorIexlib().add(
                "/process*",
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    "{\"contentline\":{\"weight\":1,\"text\":\"\"}}"));

            cluster.onlineDB().add("/online?uid=" + UID,
                new StaticHttpResource(new OnlineHandler(true)));

            cluster.axis().add("/v1/facts/store_batch?*", HttpStatus.SC_OK);

            cluster.msearch().add(
                "/api/async/enlarge/your?*",
                new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));

            FileEntity entity = new FileEntity(
                new File(getClass().getResource(
                    "./bigimage/fs_response.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                "/filter_search*",
                new StaticHttpItem(HttpStatus.SC_OK, entity));

            HttpPost post = new HttpPost(LOCALHOST + cluster.iexproxy().port()
                + "/notify?service=change_log&prefix=" + UID + "&mdb=pg");
            post.setEntity(new StringEntity(
                IexProxyTestMocks.pgNotifyPost(UID, MID),
                ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            HttpGet getFacts = new HttpGet(
                LOCALHOST + cluster.iexproxy().port() + "/facts?mdb=pg&uid="
                    + UID + "&cokedump&extract&mid=" + MID);
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                String facts = CharsetUtils.toString(response.getEntity());
                System.out.println("/facts returned:\n" + facts);
                cluster.compareJson("./bigimage/facts.json", facts, false);
            }
        }
    }
}
