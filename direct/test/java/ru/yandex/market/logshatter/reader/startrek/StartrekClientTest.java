package ru.yandex.market.logshatter.reader.startrek;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.fileupload.util.Streams;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Event;
import ru.yandex.startrek.client.model.FieldRef;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueRef;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Как запускать в деве https://wiki.yandex-team.ru/users/imelnikov/startrek/
 *
 * @author imelnikov
 */
@Ignore
public class StartrekClientTest {

    private StartrekClient getClient() {
        StartrekClient startrekClient = new StartrekClient("<token>");
        startrekClient.setApiUrl("https://st-api.yandex-team.ru");
        return startrekClient;
    }

    @Test
    public void testSimpleEventDeserializer() throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(SimpleClient.SimpleHistoryEvent.class, new SimpleClient.SimpleEventDeserializer());

        String json = Streams.asString(StartrekClientTest.class.getResourceAsStream("/startrekIssues/MBO-16051.json"));

        List<SimpleClient.SimpleHistoryEvent> events = gsonBuilder.create()
            .fromJson(json, new TypeToken<List<SimpleClient.SimpleHistoryEvent>>() {
            }.getType());

        Assert.assertEquals("4", events.get(0).getFieldChangesMap().get("status"));
        Assert.assertEquals("inReview", events.get(1).getFieldChangesMap().get("status"));
        Assert.assertEquals("5b1810f5427e9a001b42b253", events.get(2).getFieldChangesMap().get("sla"));
    }

    @Test
    public void getIssuesFromQueue() {
        StartrekClient startrekClient = getClient();

        Date date = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        List<Issue> list = startrekClient.getIssues("MARKETINFRA", date, 1000).collect(Collectors.toList());

        System.out.println(list.stream().map(IssueRef::getKey).collect(Collectors.joining(", ")));

        int size = list.size();
        System.out.println(size);

        assertTrue(size > 0);
    }

    @Test
    public void getIssueById() {
        StartrekClient startrekClient = getClient();

        Session session = startrekClient.getSession();
        System.out.println(session.issues().get("MARKETPERS-2449").getAssignee().get().getLogin());
    }

    @Test
    public void getIssueHistory() {
        StartrekClient startrekClient = getClient();

        Session session = startrekClient.getSession();
        session.issues().get("MARKETOUT-7593").getEvents()
            .stream().forEach(e -> {
            StringBuilder st = new StringBuilder();
            st.append(new Date(e.getUpdatedAt().getMillis())).append(" ");

            if (!(e.getFields().length() > 0)) {
                return;
            }

            Event.FieldChange fieldChange = e.getFields().get(0);
            FieldRef field = fieldChange.getField();
            if ("status".equals(field.getId())) {
                if ("status".equals(field.getId())) {
//                    Status status = (Status) ;
                    Map<String, String> obj = (Map) fieldChange.getTo().get();
                    String status = obj.get("key");
                    st.append(status).append(" ");
                }
                System.out.println(st.toString());
            }


        });
    }

    @Test
    public void checkQueueOrder() {
        StartrekClient startrekClient = getClient();
        long prevUpdateMillis = 0;

        List<Issue> list;
        do {
            list = startrekClient
                .getIssues("MARKETINFRA", new Date(prevUpdateMillis), 1000)
                .collect(Collectors.toList());
            for (Issue issue : list) {
                long updateMillis = issue.getUpdatedAt().getMillis();
                if (updateMillis < prevUpdateMillis) {
                    fail();
                }
                prevUpdateMillis = updateMillis;
            }
        } while (list.size() > 0);
    }
}
