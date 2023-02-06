package ru.yandex.mail.so.spampkin;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.TestBase;

public class SpampkinTest extends TestBase {
    private static final String SO_ACCEPT = "OK ACCEPT";
    private static final String SO_REJECT = "OK REJECT";
    private static final String SO_SPAM = "OK SPAM";
    private static final String SO_UNKNOWN = "OK UNKNOWN";

    @Test
    public void testIntranet() throws Exception {
        try (SpampkinCluster cluster = new SpampkinCluster();
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();

            String uri =
                "/antispam?CONNECT=server.liner-tour.ru+%5B77.88.47.34%5D+FRNR"
                + "+QID%3D&HELO=server.liner-tour.ru&MAILFROM=root%40server."
                + "local+SIZE%3D648+frm%3Droot%40server.local&RCPTTO=shev%40"
                + "liner-tour.ru+ID%3D1130000030210774+UID%3D1130000013010799"
                + "+COUNTRY%3Dru";
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_ACCEPT,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testNoShinglers() throws Exception {
        try (SpampkinCluster cluster = new SpampkinCluster();
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();

            String uri =
                "/antispam?CONNECT=server.liner-tour.ru+%5B77.88.48.34%5D";
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_UNKNOWN,
                    CharsetUtils.toString(response.getEntity()));
            }

            uri = "/antispam?CONNECT=%5B777.88.48.34%5D&MAILFROM=";
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_UNKNOWN,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testShinglers() throws Exception {
        try (SpampkinCluster cluster = new SpampkinCluster();
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            cluster.shingler().add(
                "/shinrqst?shget=ff8cddf34344d3c2-14=&50a5d75e5a5eafa5-17=&",
                "<SH: 50a5d75e5a5eafa5-17 empty='1' ti='1,20000,0,0,0,0.000000"
                + "' yi='0,0,0,0,0,0.000000' ak='0.426586' pr='on' lmt='"
                + "1559682004' >");
            cluster.shingler().add(
                "/shinrqst?shget=f158844ba9c317da-14=&50a5d75e5a5eafa4-17=&",
                "<SH: 50a5d75e5a5eafa4-17 empty='1' ti='0,7000,6000,0,0,0.0000"
                + "' yi='0,0,4,0,0,0.000000' ak='0.426586' pr='on' lmt='"
                + "1559682005' >");

            String uri =
                "/antispam?CONNECT=mail.jmtc.ru+%5B217.16.28.120%5D+QID%3D"
                + "&HELO=zimbra.jewish-museum.ru&MAILFROM=anton.o%40jewish-mus"
                + "eum.ru+SIZE%3D298593&RCPTTO=ale%40orz-design.ru+ID%3D113000"
                + "0011583288+UID%3D1130000004238833+COUNTRY%3Dru+TEL%3D1";
            String uri2 =
                "/antispam?CONNECT=mail.j1tc.ru+%5B217.16.28.120%5D+QID%3D"
                + "&HELO=zimbra.jew1sh-museum.ru&MAILFROM=anton.o%40jew1sh-mus"
                + "eum.ru+SIZE%3D298593&RCPTTO=ale%40orz-design.ru9+COUNTRY%3D"
                + "ru+TEL%3D1+UID%3D1130000004238834+ID%3D1130000011583289";
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_REJECT,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_SPAM,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private static String jrbldUri(final String ip) {
        return "/check?ip=" + ip
            + "&check=fast-ham&check=fast-spam&check=fast-reject";
    }

    private static String jrbldResponse(
        final boolean ham,
        final boolean spam,
        final boolean reject)
    {
        return "{\"infos\":{},\"checks\":{\"fast-ham\":" + ham
            + ",\"fast-spam\":" + spam + ",\"fast-reject\":" + reject + "}}";
    }

    @Test
    public void testJrbld() throws Exception {
        try (SpampkinCluster cluster = new SpampkinCluster();
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.jrbld().add(
                jrbldUri("217.16.33.23"),
                jrbldResponse(false, false, false));
            cluster.jrbld().add(
                jrbldUri("217.16.33.24"),
                jrbldResponse(true, false, true));
            cluster.jrbld().add(
                jrbldUri("217.16.33.25"),
                jrbldResponse(false, true, true));
            cluster.jrbld().add(
                jrbldUri("217.16.33.26"),
                jrbldResponse(false, false, true));
            cluster.start();

            String uri1 =
                "/antispam?CONNECT=mail.jmtc.ru+%5B217.16.33.23%5D+QID%3D";
            String uri2 =
                "/antispam?CONNECT=mail.jmtc.ru+%5B217.16.33.24%5D+QID%3D";
            String uri3 =
                "/antispam?CONNECT=mail.jmtc.ru+%5B217.16.33.25%5D+QID%3D";
            String uri4 =
                "/antispam?CONNECT=mail.jmtc.ru+%5B217.16.33.26%5D+QID%3D";
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_UNKNOWN,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_ACCEPT,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri3)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_SPAM,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpPost(cluster.spampkin().host() + uri4)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    SO_REJECT,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

