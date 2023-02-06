package ru.yandex.market.statface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.statface.model.StatfaceComment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author kukabara
 */
@Ignore
public class StatfaceClientTest {
    private static final String USER = "robot_robot-market-infra";
    private static final String PASSWORD = ""; //deprecated
    private static final boolean PREFER_O_AUTH_TOKEN_AUTHORIZATION = true;
    private static final String TOKEN = ""; //see yav
    private static final String HOST = "https://stat-beta.yandex-team.ru";
    private static final String UPLOAD_HOST = "https://upload.stat-beta.yandex-team.ru";
    private static final String STABLE_HOST = "https://stat.yandex-team.ru";
    private static final String STABLE_UPLOAD_HOST = "https://upload.stat.yandex-team.ru";
    private static final int TIMEOUT_SECONDS = 60;
    private static final int MAX_CONNECTIONS = 20;
    private StatfaceClient statfaceClient;

    @Before
    public void init() {
        statfaceClient = new StatfaceClient(HOST, UPLOAD_HOST, PREFER_O_AUTH_TOKEN_AUTHORIZATION, TOKEN, USER, PASSWORD,
            TIMEOUT_SECONDS, MAX_CONNECTIONS);
    }

    @Test
    public void testSendAnyData() throws Exception {
        String name = "Market/Infrastructure/Events/Conductor/minutely";
        String jsonData = "{\"values\": [{\"fielddate\": \"2015-11-18 09:20:00\", \"value\": 1 }]}";
        statfaceClient.sendAnyData(name, jsonData);
        System.out.println("End");
    }

    @Test
    public void testJsonComments() throws Exception {
        String result = "{\n" +
            "    \"comments\": [{\n" +
            "            \"date\": \"2015-08-05T03:00:00\", \n" +
            "            \"fields\": [\n" +
            "                \"value\"\n" +
            "            ], \n" +
            "            \"id\": 11424828, \n" +
            "            \"meta_data\": \"{\\\"position\\\":\\\"3\\\",\\\"visible\\\":0}\", \n" +
            "            \"path\": \"/Market/Infrastructure/KPI/Kpi3/daily\", \n" +
            "            \"subject\": \"\\u0443\\u043f\\u0430\\u043b \\u0441\\u0435\\u0440\\u0432\\u0438\\u0441 " +
            "\\u043a\\u043e\\u043c\\u043c\\u0435\\u043d\\u0442\\u043e\\u0432 \\u0438 " +
            "\\u043f\\u0440\\u0430\\u0439\\u0441 \\u0447\\u0430\\u0440\\u0442\", \n" +
            "            \"tags\": [], \n" +
            "            \"timestamp_ms\": 1438743600000, \n" +
            "            \"user\": \"mrwolfe\"\n" +
            "        }]}";
        List<StatfaceComment> comments = statfaceClient.deserializeComments(result);
        assertNotNull(comments);
        assertEquals(1, comments.size());
        printComments(comments);

        result = " [{\"id\": 7211104, \"date\": \"2015-11-10T11:26:26.487000\", \"subject\": \"test comment\", " +
            "\"path\": \"/Market/Infrastructure/KPI/Kpi3/daily\", \"fields\": [], \"tags\": [], \"user\": " +
            "\"robot_robot-market-infra\", \"timestamp_ms\": 1447154786487, \"meta_data\": " +
            "\"{\\\"position\\\":\\\"3\\\",\\\"visible\\\":1}\"}]";
        comments = statfaceClient.deserializeCommentsArray(result);
        printComments(comments);
    }

    private void printComments(List<StatfaceComment> comments) {
        for (StatfaceComment comment : comments) {
            System.out.println(comment);
        }
    }

    // curl -H "StatRobotAuth: robot_robot-market-infra:eom2Tr2be6auSti" \
    // "https://stat-beta.yandex-team.ru/_v3/comments/?path=/Market/Infrast\
    // ructure/KPI/Kpi3/daily&date_min=1444470618342"
    @Test
    public void testGetComments() throws Exception {
        List<StatfaceComment> comments = statfaceClient.getComments("/Market/Infrastructure/KPI/Kpi3/daily",
            getMonthAgo(), null, null, null);
        assertNotNull(comments);
        for (StatfaceComment c : comments) {
            System.out.println(c.getDate() + " " + c.getSubject());
        }
    }

    // curl -H "StatRobotAuth: robot_robot-market-infra:eom2Tr2be6auSti" -X "POST" \
    // "https://stat-beta.yandex-team.ru/_v3/comments/?path=/Market/Infrastructure/KPI/K\
    // pi3/daily&date=1444470618342&subject=test_comment&tags=report,front"
    // или
    // curl -H "Content-Type: application/json" -H "StatRobotAuth: robot_robot-market-infra:eom2Tr2be6auSti" \
    // -X "POST" "https://stat-beta.yandex-team.ru/_v3/comments/"
    // --data @data.txt

    @Test
    public void testPostComments() throws Exception {
        String path = "/Market/Infrastructure/KPI/Kpi3/daily";
        Date date = new Date();
        String subject = "test comment";
        StatfaceComment comment = new StatfaceComment(path, date, subject);
        comment.setTags(Arrays.asList("report", "front"));
        List<StatfaceComment> comments = statfaceClient.setComment(comment);
        printComments(comments);

        comments = statfaceClient.getComments(path, date, null, null, null);
        AtomicBoolean found = new AtomicBoolean();
        for (StatfaceComment c : comments) {
            if (c.getSubject().equals(subject)) {
                found.set(true);
                return;
            }
        }
        assertTrue(found.get());
    }

    @Test
    public void testDeleteComments() throws Exception {
        String path = "/Market/Infrastructure/Money/Clicks/minutely";
        List<StatfaceComment> comments = statfaceClient.setComment(path, new Date(), "Test");
        assertEquals(1, comments.size());
        statfaceClient.deleteComment(comments.get(0).getId());
    }

    @Test
    public void testDeleteAllComments() throws Exception {
        String path = "/Market/Infrastructure/Money/Chips/minutely";
        List<StatfaceComment> comments = statfaceClient.getComments(path, DateUtil.addDay(new Date(), -60),
            DateUtil.addDay(new Date(), 1), null, null);
        final int[] count = {0};
        for (StatfaceComment c : comments) {
            try {
                statfaceClient.deleteComment(c.getId());
                count[0]++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Deleted " + count[0]);
    }

    public StatfaceClient getProdStatfaceClient() throws Exception {
        return new StatfaceClient(STABLE_HOST, STABLE_UPLOAD_HOST,
            PREFER_O_AUTH_TOKEN_AUTHORIZATION, TOKEN, USER, PASSWORD,
            TIMEOUT_SECONDS, MAX_CONNECTIONS);
    }

/*
    @Test
    public void truncate() throws Exception {
        statfaceClient.truncateReport("Market/IR/ClusterAge", StatfacePeriod.hourly);
    }

    @Test
    public void drop() throws Exception {
        StatfaceClient client = getProdStatfaceClient();
        client.deleteReport("Market/IR/Jxtym ");
        client.deleteReport("Market/Content/SuperController/MatchTimeNew");
        client.deleteReport("Market/Content/SuperController/CategoryStatNew");
    }
*/

    public Date getMonthAgo() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        return c.getTime();
    }

    @Test
    @Ignore
    public void testDictionary() throws Exception {
        Random rnd = new Random();
        String dictName = "testDictionary_000";
        Map<Object, Object> dict = new HashMap<>();
        dict.put("foo", "bar");
        dict.put(rnd.nextInt(), rnd.nextInt());
        List<Object> testList = new ArrayList<>();
        testList.add("bazz");
        testList.add(rnd.nextDouble());
        dict.put(rnd.nextBoolean(), testList);
        statfaceClient.createDictionary(dict, dictName);
        Map<Object, Object> dictFromStatface = statfaceClient.getDictionary(dictName);
        assertEquals(dict.size(), dictFromStatface.size());
        statfaceClient.deleteDictionary(dictName);
        dictFromStatface = statfaceClient.getDictionary(dictName);
        assertTrue(dictFromStatface.isEmpty());
    }
}
