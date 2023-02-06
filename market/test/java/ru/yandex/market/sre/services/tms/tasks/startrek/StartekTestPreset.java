package ru.yandex.market.sre.services.tms.tasks.startrek;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.joda.time.Instant;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.market.sre.services.tms.dao.ActionItemProcessingDao;
import ru.yandex.market.sre.services.tms.tasks.startrek.model.CommentExt;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Field;
import ru.yandex.startrek.client.utils.LocalDateModule;
import ru.yandex.startrek.client.utils.StartrekClientModule;

public class StartekTestPreset {
    protected static final String TEST_INCIDENT_QUEUE_NAME = "TESTMARKETINCID";
    protected static final String TEST_STARTREK_INC_ISSUE = "TESTMARKETINCID-1";
    protected static final String TEST_STARTREK_REL_ISSUE2 = "TESTMARKETREL-2";
    protected static final String TEST_STARTREK_REL_ISSUE3 = "TESTMARKETREL-3";
    protected static final String TEST_STARTEK_UI_LINK = "https://st.yandex-team.ru/";
    protected static final String UPDATE_OBJECTS_FOLDER_PATH = "tms/tasks/startrek/";
    protected static final String TEST_USER_LOGIN = "asivolapovTest";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public WireMockRule mockStAPIServer = new WireMockRule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Session mockStartrekSession;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ActionItemProcessingDao actionItemProcessingDao;

    protected ListF<Comment> prepareCommentBatch (int batchSize){
        return addCommentBatch(Cf.arrayList(), batchSize);
    }

    protected ListF<Comment> addCommentBatch (ListF <Comment> comments, int batchSize){
        for (int i = 0; i < batchSize; i++) {
            Comment comment = new CommentExt(i, "Some text", Instant.now(), Instant.now());
            comments.add(comment);
        }
        return comments;
    }

    protected String loadTextResource(String fileName) throws IOException {
        String name = UPDATE_OBJECTS_FOLDER_PATH + fileName;
        URL url = Resources.getResource(name);
        return Resources.toString(url, Charsets.UTF_8);
    }

    protected ObjectMapper createMapper() {
        MapF<String, Field.Schema> customFields = Cf.map();
        return new ObjectMapper()
                .registerModule(new JodaModule())
                .registerModule(new LocalDateModule())
                .registerModule(new StartrekClientModule(customFields))
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setInjectableValues(new InjectableValues() {
                    @Override
                    public Object findInjectableValue(Object valueId, DeserializationContext deserializationContext,
                                                      BeanProperty forProperty, Object beanInstance) {
                        return null;
                    }
                });
    }
}
