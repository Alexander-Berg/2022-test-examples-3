package ru.yandex.market.tsum.api.onetime;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.sqlite.SQLiteDataSource;
import ru.yandex.market.tsum.api.config.TsumApiConfig;
import ru.yandex.market.tsum.api.events.MongoEventDao;
import ru.yandex.market.tsum.event.Event;
import ru.yandex.market.tsum.event.EventStatus;
import ru.yandex.market.tsum.event.MicroEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 03/10/16
 *
 *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * Снимать @Ignore перед запуском
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *
 *
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TsumApiConfig.class)
//@ContextConfiguration(classes = TestConfig.class)
public class EventLoader {
    @Autowired
    private MongoEventDao mongoEventDao;

    private JdbcTemplate sqliteJdbcTemplate;

    @Before
    public void setUp() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:/Users/andreevdm/tmp/timeline/events.db");
        sqliteJdbcTemplate = new JdbcTemplate(dataSource);
        sqliteJdbcTemplate.update("CREATE INDEX IF NOT EXISTS ei ON events (name, timestamp)");
    }


    @Test
    public void load() throws Exception {

        List<Event.Builder> events = new ArrayList<>();
        sqliteJdbcTemplate.query(
            "SELECT * FROM event_groups",
            rs -> {
                Event.Builder builder = Event.newBuilder()
                    .setProject("report")
                    .setSource("reportDb")
                    .setStartTimeSeconds(rs.getInt("begin"))
                    .setEndTimeSeconds(rs.getInt("end"))
                    .setStatus(toStatus(rs.getInt("defcon")))
                    .setType(rs.getString("name"));

                events.add(builder);
            });

        for (int i = 0; i < events.size(); i++) {
            if (i % 100 == 0) {
                System.out.println(i + " of " + events.size());
            }
            Event.Builder event = events.get(i);
//            event.setId("event-report-" + i);
            Multiset<String> tags = HashMultiset.create();
            appendMicroEvens(event, tags);
            Set<String> uniqTags = new HashSet<>();
            for (String tag : tags) {
                if (tags.count(tag) >= Math.max(event.getMicroEventsCount() / 5, 2)) {
                    uniqTags.add(tag);
                }
            }
            event.addAllTags(uniqTags);
        }

        mongoEventDao.addEvents(events.stream().map(Event.Builder::build).collect(Collectors.toList()));
    }

    public void appendMicroEvens(Event.Builder event, Collection<String> allTags) {
        sqliteJdbcTemplate.query(
            "SELECT * FROM events WHERE name = ? AND timestamp >= ? AND timestamp <= ?",
            new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    List<String> tags = EventLoader.this.toTags(rs.getString("features_json"));
                    allTags.addAll(tags);
                    MicroEvent.Builder builder = MicroEvent.newBuilder()
                        .setProject("report")
                        .setSource(rs.getString("obj_id"))
                        .setTimeSeconds(rs.getInt("timestamp"))
                        .setStatus(EventLoader.this.toStatus(rs.getInt("defcon")))
                        .setType(rs.getString("name"))
                        .addAllTags(tags);
                    event.addMicroEvents(builder);
                }
            },
            event.getType(), event.getStartTimeSeconds(), event.getEndTimeSeconds()
        );
    }

    public EventStatus toStatus(int defcon) {
        switch (defcon) {
            case 1:
            case 2:
                return EventStatus.INFO;
            case 3:
                return EventStatus.WARN;
            case 4:
            case 5:
                return EventStatus.ERROR;
            default:
                throw new IllegalStateException();
        }
    }

    public List<String> toTags(String json) {
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        List<String> tags = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement element = entry.getValue();
            if (element.isJsonNull()) {
                continue;
            }
            if (element.isJsonPrimitive()) {
                String tag = entry.getKey() + ":" + element.getAsString();
                tags.add(tag);
            } else {
                System.gc();
            }
        }
        return tags;
    }

}
