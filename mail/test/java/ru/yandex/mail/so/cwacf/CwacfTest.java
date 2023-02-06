package ru.yandex.mail.so.cwacf;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class CwacfTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (CwacfCluster cluster = new CwacfCluster();
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    Configs.targetConfig(),
                    Configs.dnsConfig()))
        {
            cluster.upstream().add(
                "/check-json?service=GEOCHATS&so_login=5598601"
                + "&form_id=somerandomchatid&id=request-key&reqid=*",
                new ExpectingHttpItem(
                    "{\"client_ip\":\"10.100.0.14\",\"client_uid\":5598601,"
                    + "\"form_id\":\"somerandomchatid\","
                    + "\"form_type\":\"mssngr:commentator:news-demo-stand\","
                    + "\"form_fields\":{\"chat_name\":{\"type\":\"string\","
                    + "\"filled_by\":\"user\",\"value\":\"Bonus e-mail\"},"
                    + "\"message\":{\"type\":\"string\","
                    + "\"filled_by\":\"user\",\"value\":\"Поздравляем!\"},"
                    + "\"message_timestamp\":{\"type\":\"string\",\"filled_by"
                    + "\":\"automatic\",\"value\":\"1571804053352003\"}},"
                    + "\"subject_template\":\"{chat_name}\","
                    + "\"subject\":\"Bonus e-mail\","
                    + "\"body_template\":\"{message}\","
                    + "\"body\":\"Поздравляем!\"}",
                    "<spam>1</spam>"));
            cluster.start();

            HttpAssert.assertStat(
                "checkform-resolutions-true_ammm",
                "0",
                cluster.cwacf().port());

            HttpPost post = new HttpPost(cluster.cwacf().host().toString());
            post.setEntity(
                new StringEntity(
                    "{\"jsonrpc\":\"2.0\",\"method\":\"process\",\"params\":{"
                    + "\"service\":\"mssngr:commentator:news-demo-stand\","
                    + "\"type\":\"text\",\"key\":\"request-key\",\"body\":{"
                    + "\"UserIp\":\"10.100.0.14\",\"Uid\":5598601,\""
                    + "ChatId\":\"somerandomchatid\","
                    + "\"ChatName\":\"Bonus e-mail\""
                    + ",\"Message\":\"Поздравляем!\","
                    + "\"Timestamp\": 1571804053352003}},\"id\":100500}",
                    StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"jsonrpc\":\"2.0\",\"id\":100500,\"result\":"
                        + "[{\"name\":\"text_so_is_spam\","
                        + "\"value\":true,\"source\":\"checkform\","
                        + "\"subsource\":\"checkform\",\"key\":\"request-key\""
                        + ",\"entity\":\"text\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            Thread.sleep(100L);
            HttpAssert.assertStat(
                "checkform-resolutions-true_ammm",
                "1",
                cluster.cwacf().port());
        }
    }
}

