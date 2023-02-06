package ru.yandex.iex.proxy;

import java.io.File;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class MoveHandlerTrustedTest extends TestBase {
    @Test
    public void testFalsesAndSamples() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this, false, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            FileEntity entity = new FileEntity(
                new File(
                    getClass().getResource("trusted_folders.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.folders().add(
                "/folders?caller=msearch&mdb=pg&uid=991949281",
                new StaticHttpResource(HttpStatus.SC_OK, entity));

            entity = new FileEntity(
                new File(
                    getClass().getResource(
                        "trusted_move_fs_response.json")
                        .toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                "/filter_search?order=default&full_folders_and_labels=1"
                + "&uid=991949281&mdb=pg"
                + "&mids=171981210770333946&mids=171981210770328957"
                + "&mids=171981210770328958",
                new StaticHttpResource(HttpStatus.SC_OK, entity));

            cluster.tikaite().add(
                "/headers?json-type=dollar&stid=320.mail:991949281.E3038732:"
                + "193765804868993134528353288630",
                new StaticHttpResource(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        "{\"headers\":[{\"x-yandex-complainer-uid\":"
                        + "\"522058669\"}]}")));
            cluster.tikaiteMl().add(
                "/extract?json-type=dollar&extractor-name=nested-mail"
                + "&stid=320.mail:991949281."
                + "E3357098:2128831365182067077065233079254",
                new StaticHttpResource(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        "{\"docs\":["
                        + "{\"hid\":\"1.1\",\"pure_body\":\"hello\"},"
                        + "{\"hid\":\"1.2.2\",\"pure_body\":\"world\"}]}")));

            String deleteUri =
                "/delete?so_trusted_user&prefix=522058669&service=change_log"
                + "&url=so_trusted_complainer_522058669";
            cluster.producerAsyncClient().add(
                deleteUri,
                new StaticHttpResource(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            "{\"prefix\":522058669,\"docs\":[{\"url\":"
                            + "\"so_trusted_complainer_522058669\"}]}"),
                        HttpStatus.SC_OK)));

            String modifyUri1 =
                "/modify?spam-samples&prefix=991949281"
                + "&url=spam_samples_991949281_so_compains_171981210770328957"
                + "&operation-id=236165814";
            cluster.producerAsyncClient().add(
                modifyUri1,
                new StaticHttpResource(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            "{\"prefix\":991949281,\"docs\":["
                            + "{\"spam_sample_type\":\"so_compains\","
                            + "\"spam_sample_revision\":\"236165814\","
                            + "\"spam_sample_stid\":\"320.mail:991949281."
                            + "E3357098:2128831365182067077065233079254\","
                            + "\"url\":\"spam_samples_991949281_so_compains_"
                            + "171981210770328957\",\"spam_sample_data\":\""
                            + "{\\\"docs\\\":[{\\\"hid\\\":\\\"1.1\\\","
                            + "\\\"pure_body\\\":\\\"hello\\\"},{\\\"hid\\\":"
                            + "\\\"1.2.2\\\",\\\"pure_body\\\":\\\"world\\\"}]"
                            + "}\"}]}"),
                        HttpStatus.SC_OK)));
            String modifyUri2 =
                "/modify?spam-samples&prefix=991949281"
                + "&url=spam_samples_991949281_so_compains_171981210770328958"
                + "&operation-id=236165814";
            cluster.producerAsyncClient().add(
                modifyUri2,
                new StaticHttpResource(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            "{\"prefix\":991949281,\"docs\":["
                            + "{\"spam_sample_type\":\"so_compains\","
                            + "\"spam_sample_revision\":\"236165814\","
                            + "\"spam_sample_labels\":\"\nсам себе\n\","
                            + "\"spam_sample_stid\":\"320.mail:991949281."
                            + "E3357098:2128831365182067077065233079254\","
                            + "\"url\":\"spam_samples_991949281_so_compains_"
                            + "171981210770328958\",\"spam_sample_data\":\""
                            + "{\\\"docs\\\":[{\\\"hid\\\":\\\"1.1\\\","
                            + "\\\"pure_body\\\":\\\"hello\\\"},{\\\"hid\\\":"
                            + "\\\"1.2.2\\\",\\\"pure_body\\\":\\\"world\\\"}]"
                            + "}\"}]}"),
                        HttpStatus.SC_OK)));
            String removalUri =
                "/modify?spam-samples-removal&prefix=991949281"
                + "&operation-id=236165814";
            cluster.producerAsyncClient().add(
                removalUri,
                new StaticHttpResource(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            "{\"prefix\":991949281,\"docs\":["
                            + "{\"spam_sample_type\":\"so_compains\","
                            + "\"spam_sample_revision\":\"236165814\","
                            + "\"url\":\"spam_samples_991949281_so_compains_"
                            + "171981210770333935\"},"
                            + "{\"spam_sample_type\":\"so_compains\","
                            + "\"spam_sample_revision\":\"236165814\","
                            + "\"url\":\"spam_samples_991949281_so_compains_"
                            + "171981210770333936\"}]}"),
                        HttpStatus.SC_OK)));

            HttpPost post = new HttpPost(
                cluster.iexproxy().host()
                + "/notify?mdb=pg&pgshard=2764&operation-id=236165814"
                + "&operation-date=1586163732.344691&uid=991949281"
                + "&change-type=move&changed-size=333&salo-worker=pg2764:6"
                + "&transfer-timestamp=1586163732441&zoo-queue-id=12034025"
                + "&deadline=1586163742574");
            entity = new FileEntity(
                new File(getClass().getResource("trusted_move.json").toURI()),
                ContentType.APPLICATION_JSON);
            post.setEntity(entity);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    1,
                    cluster.producerAsyncClient().accessCount(modifyUri1));
                Assert.assertEquals(
                    1,
                    cluster.producerAsyncClient().accessCount(modifyUri2));
                Assert.assertEquals(
                    1,
                    cluster.producerAsyncClient().accessCount(deleteUri));
                Assert.assertEquals(
                    1,
                    cluster.producerAsyncClient().accessCount(removalUri));
            }
        }
    }
}

