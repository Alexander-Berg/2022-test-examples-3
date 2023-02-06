package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpStatus;
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
import ru.yandex.test.util.TestBase;

public class IexProxyBounceTest extends TestBase {
    private static final String UID = "&uid=";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String FILTER_SEARCH = "/filter_search?" + IexProxy.FILTER_SEARCH_PARAMS + UID;
    private static final String NOTIFY = "/notify?mdb=";
    private static final String SPAM4U = "spam4u";
    //private static final String URI_PATTERN = "/notify?*";
    private static final String USER_EMAIL = "user@ya.ru";

    private static String filterSearch(
        final String receivedDate,
        final String mid)
    {
        return "{\"envelopes\":[{\"stid\":\"1.stid\",\"fid\":1,"
            + "\"folder\":{\"type\":{\"title\":\"user\"},\"name\":\"fld\"},"
            + "\"from\":[{\"displayName\":\"" + SPAM4U
            + "\",\"domain\":\"hotmail.com\",\"local\":\"spammer\"}],"
            + "\"to\":[{\"displayName\":\"yandex user\","
            + "\"domain\":\"ya.ru\",\"local\":\"user\"}],"
            + "\"receiveDate\":" + receivedDate
            + ",\"mid\":" + mid
            + ",\"subject\":" + "\"777\""
            + ",\"types\": [8]"
            + ",\"threadId\":" + mid + '}' + ']' + '}';
    }

    //CSOFF: MultipleStringLiterals
    @Test
    public void testPgBounce() throws Exception {
        try (IexProxyCluster cluster =
                new IexProxyCluster(
                    this,
                    null,
                    "entities.message-type-8 = bounce\n"
                    + "postprocess.message-type-8 = "
                    + "bounce:http://localhost:" + IexProxyCluster.IPORT
                    + "/bounce",
                    true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String mdb = "pg";
            HttpPost post = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + mdb);
            final long uid = 9109L;
            cluster.msal().add("/get-mid-by-message-id?&uid=" + uid + "&message-id=789987", "160159261748369035");
            String bbUri = IexProxyCluster.blackboxUri(UID + uid);
            cluster.blackbox().add(bbUri, IexProxyCluster.blackboxResponse(uid, USER_EMAIL));
            cluster.onlineDB().add("/online?uid=" + uid, new StaticHttpResource(new OnlineHandler(true)));
            cluster.msearch().add(
                "/api/async/enlarge/your?uid=" + uid,
                new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));
            cluster.freemail().add("/api/v1", new StaticHttpItem(HttpStatus.SC_OK, "{}"));
            //String to = "\"Test \" <a861@ya.ru>";
            //String uri = URI_PATTERN;
            String mid = "119";
            post.setEntity(
                new StringEntity(
                    "{\"lcn\":19,\"operation_id\":19,\"uid\":" + uid + ",\"operation_date\": \""
                    + "1446810748.043122\",\"change_type\":\"store\",\"changed\":[" + "{\"mid\":" + mid + "}]}"));
            cluster.filterSearch().add(FILTER_SEARCH + uid + '*', filterSearch("1244667893", mid));
            cluster.producerAsyncClient().add("/add*", new ProxyHandler(cluster.testLucene().indexerPort()));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().getResource("response_from_coke_bounce.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add("/process?*", new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            String tikaiteResponse =
                "{\"prefix\":" + uid
                + ",\"docs\":[{\"hid\":\"1\",\"uid\":\"" + uid
                + "\",\"url\":\"" + uid + '_' + mid
                + "/1\",\"body_text\":\"hello\"}]}";
            logger.info("tikaiteResponse = " + tikaiteResponse);
            cluster.tikaite().add("/tikaite*", tikaiteResponse);
            cluster.axis().add("/v1*", new StaticHttpItem(HttpStatus.SC_OK));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            String checkUri = "/search?prefix=" + uid + "&asc=true"
                 + "&service=change_log&get=fact_name,fact_data&text=fact_mid:(" + mid + ')';
            String stringToCompare = stringToLucene("bounce_lucene.json");
            logger.info("expecting that lucene will contain:\n" + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
        }
    }

    @Test
    public void testPgBounceMsalNotFound() throws Exception {
        try (IexProxyCluster cluster =
                new IexProxyCluster(
                    this,
                    null,
                    "entities.message-type-8 = bounce\n"
                    + "postprocess.message-type-8 = "
                    + "bounce:http://localhost:" + IexProxyCluster.IPORT
                    + "/bounce",
                    true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String mdb = "pg";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port()
                + NOTIFY + mdb);
            final long uid = 9109L;
            String bbUri = IexProxyCluster.blackboxUri(UID + uid);
            cluster.blackbox().add(
                bbUri,
                IexProxyCluster.blackboxResponse(uid, USER_EMAIL));
            //String to = "\"Test \" <a861@ya.ru>";
            //String uri = URI_PATTERN;
            post.setEntity(
                new StringEntity(
                    "{\"lcn\":19,\"operation_id\":19,\"uid\":9109,"
                        + "\"operation_date\": \""
                        + "1446810748.043122\",\"change_type\":\"store\","
                        + "\"changed\":["
                        + "{\"mid\":11"
                        + "9}]}"));
            String fsUri = FILTER_SEARCH + uid + '*';
            String mid = "119";
            String fsResponse =
                filterSearch("1244667893", mid);
            cluster.filterSearch().add(fsUri, fsResponse);
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke_bounce.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            String tikaiteResponse =
                "{\"prefix\":" + uid
                + ",\"docs\":[{\"hid\":\"1\",\"uid\":\"" + uid
                + "\",\"url\":\"" + uid + '_' + mid
                + "/1\",\"body_text\":\"hello\"}]}";
            System.out.println("tikaiteResponse = " + tikaiteResponse);
            cluster.tikaite().add("/tikaite?*", tikaiteResponse);
            cluster.axis().add(
                "/v1*",
                new StaticHttpItem(HttpStatus.SC_OK));
            cluster.msal().add(
                "/get-mid-by-message-id*",
                new StaticHttpItem(HttpStatus.SC_NOT_FOUND));
            cluster.freemail().add("/api/v1*", new StaticHttpItem(HttpStatus.SC_OK));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            String checkUri = "/search?prefix=" + uid + "&asc=true"
                 + "&service=change_log&get=fact_name,fact_data&text=fact_mid:("
                 + mid + ')';
            String stringToCompare = stringToLucene(
                    "bounce_lucene_mid_not_found.json");
            logger.info("expecting that lucene will contain:\n"
                    + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
        }
    }
    //CSON: MultipleStringLiterals

    private String stringToLucene(final String fileName) {
        try {
            Path path = Paths.get(getClass().getResource(fileName).toURI());
            return java.nio.file.Files.readString(path);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
