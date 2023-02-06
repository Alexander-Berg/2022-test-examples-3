package ru.yandex.msearch.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class Am22Test extends TestBase {
    @Test
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.filterSearch().add(
                "/filter_search?mdb=pg&full_folders_and_labels=1"
                + "&mids=158751886864816470&uid=5598601",
                "{\"envelopes\":[{\"mid\":\"158751886864816470\",\"fid\":\"1\""
                + ",\"threadId\":\"158751886864816470\",\"revision\":1874,"
                + "\"date\":1463428221,\"receiveDate\":1463428183,\"from\":[{"
                + "\"local\":\"tapa\",\"domain\":\"pismorf.com\",\"displayName"
                + "\":\"Топтыгин Иван Михайлович\"}],\"replyTo\":[{\"local\":"
                + "\"tapa\",\"domain\":\"pismorf.com\",\"displayName\":\""
                + "tapa@pismorf.com\"}],\"subject\":\"Тест\",\"subjectInfo\":{"
                + "\"type\":\"\",\"prefix\":\"\",\"subject\":\"Тест\",\""
                + "postfix\":\"\",\"isSplitted\":true},\"cc\":[],\"bcc\":[],\""
                + "hdrStatus\":\"\",\"to\":[{\"local\":\"analizer\",\"domain\""
                + ":\"yandex.ru\",\"displayName\":\"analizer@yandex.ru\"}],\""
                + "hdrLastStatus\":\"\",\"uidl\":\"\",\"imapId\":\"6058\",\""
                + "ImapModSeq\":\"\",\"stid\":\""
                + "93457.12054080.4102218041160044341826540001797\",\""
                + "firstline\":\"some firstline here\",\"inReplyTo\":\"\","
                + "\"references\":\"\",\""
                + "rfcId\":\"<49e35e7c271d6225117774091629136d@xn--h1aigbl0e."
                + "xn--p1ai>\",\"size\":2740,\"threadCount\":0,\"extraData\":"
                + "\"analizer@yandex.ru\",\"newCount\":0,\"attachmentsCount\":"
                + "0,\"attachmentsFullSize\":0,\"attachments\":[],\"labels\":["
                + "\"1\",\"48\",\"90\",\"FAKE_SEEN_LBL\"],\"specialLabels\":[]"
                + ",\"types\":[4,46],\"folder\":{\"name\":\"Inbox\",\"isUser\""
                + ":false,\"isSystem\":true,\"type\":{\"code\":3,\"title\":\""
                + "system\"},\"symbolicName\":{\"code\":1,\"title\":\"inbox\"}"
                + ",\"bytes\":642498887,\"messagesCount\":6216,\""
                + "newMessagesCount\":0,\"recentMessagesCount\":0,\"unvisited"
                + "\":false,\"folderOptions\":{\"getPosition\":0},\"position\""
                + ":0,\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\",\""
                + "creationTime\":\"1158609600\",\"subscribed\":\"1\",\"shared"
                + "\":\"0\"},\"labelsInfo\":{\"1\":{\"name\":\"answered\",\""
                + "creationTime\":\"1458315065\",\"color\":\"\",\"isUser\":"
                + "false,\"isSystem\":true,\"type\":{\"code\":3,\"title\":\""
                + "system\"},\"symbolicName\":{\"code\":12,\"title\":\""
                + "answered_label\"},\"messagesCount\":37},\"48\":{\"name\":\""
                + "4\",\"creationTime\":\"1458315065\",\"color\":\"\",\"isUser"
                + "\":false,\"isSystem\":false,\"type\":{\"code\":9,\"title\":"
                + "\"so\"},\"symbolicName\":{\"code\":0,\"title\":\"\"},\""
                + "messagesCount\":155},\"90\":{\"name\":\"46\",\"creationTime"
                + "\":\"1458315065\",\"color\":\"\",\"isUser\":false,\""
                + "isSystem\":false,\"type\":{\"code\":9,\"title\":\"so\"},\""
                + "symbolicName\":{\"code\":0,\"title\":\"\"},\"messagesCount"
                + "\":1962},\"FAKE_SEEN_LBL\":{\"name\":\"FAKE_SEEN_LBL\",\""
                + "creationTime\":\"\",\"color\":\"\",\"isUser\":false,\""
                + "isSystem\":true,\"type\":{\"code\":3,\"title\":\"system\"},"
                + "\"symbolicName\":{\"code\":23,\"title\":\"seen_label\"},\""
                + "messagesCount\":0}},\"specialLabelsInfo\":{}}]}");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + "/api/am22/?connection_id="
                        + "iface-1464596588185-46838127&mdb=pg"
                        + "&mid=158751886864816470&uid=5598601")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"types\":[4,46],\"st_id\":"
                        + "\"93457.12054080.4102218041160044341826540001797\","
                        + "\"first_line\":\"some firstline here\","
                        + "\"msg_id\":\"<49e35e7c271d6225117774091629136d@xn--"
                        + "h1aigbl0e.xn--p1ai>\","
                        + "\"hdr_from\":\"\\\"Топтыгин Иван Михайлович\\\" <"
                        + "tapa@pismorf.com>\","
                        + "\"hdr_subject\":\"Тест\"}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

