package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.crm.core.jackson.CustomObjectMapperFactory;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.pers.MarketUtilsClient;
import ru.yandex.market.crm.core.services.pers.MarketUtilsService;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.json.serialization.JsonDeserializerImpl;
import ru.yandex.market.crm.json.serialization.JsonSerializerImpl;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageDaoFactory;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.correlation.PendingMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.PendingMessageSerializer;
import ru.yandex.market.crm.triggers.services.bpm.correlation.PendingMessagesDAO;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewAnswerOnModel;
import ru.yandex.market.mcrm.http.Http;
import ru.yandex.market.mcrm.http.HttpResponse;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NewAnswerConsumerTest {

    // fake input/output
    private final FakeMarketUtilsClient marketUtilsClient = new FakeMarketUtilsClient();
    private final FakeMessageDaoFactory fakeMessageDao = new FakeMessageDaoFactory();
    private final PendingMessageSerializer pendingMessageSerializer = preparePendingMessageSerializer();

    // system under test
    private final NewAnswerConsumer consumer = new NewAnswerConsumer(
            new MessageSender(fakeMessageDao, pendingMessageSerializer),
            new MarketUtilsService(marketUtilsClient, null),
            new FakeLogTypesResolver()
    );

    private static PendingMessageSerializer preparePendingMessageSerializer() {
        ObjectMapper objectMapper = CustomObjectMapperFactory.INSTANCE.getJsonObjectMapper();
        return new PendingMessageSerializer(
                new JsonSerializerImpl(objectMapper),
                new JsonDeserializerImpl(objectMapper)
        );
    }

    private static class FakeHttpResponse extends HttpResponse {
        private final String body;

        public FakeHttpResponse(String body) {
            super(null);
            this.body = body;
        }

        @Override
        public InputStream getBodyAsStream() {
            try {
                return new StringInputStream(body);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getBodyAsString() {
            return body;
        }

        @Override
        public byte[] getBodyAsBytes() {
            return body.getBytes();
        }
    }

    private static class FakeMarketUtilsClient extends MarketUtilsClient {
        private final List<HttpResponse> responses = new ArrayList<>();

        public FakeMarketUtilsClient() {
            super(null, (s) -> null);
        }

        @NotNull
        @Override
        protected HttpResponse execute(Http http) {
            return responses.get(0);
        }

        public void setResponse(String body) {
            this.responses.add(new FakeHttpResponse(body));
        }
    }

    private static class FakePendingMessageDAO extends PendingMessagesDAO {
        private final List<PendingMessage> savedMessages;

        public FakePendingMessageDAO(List<PendingMessage> savedMessages) {
            super(null, 0);
            this.savedMessages = savedMessages;
        }

        @Override
        public void insertMessages(List<PendingMessage> messages) {
            savedMessages.addAll(messages);
        }
    }

    private static class FakeMessageDaoFactory extends MessageDaoFactory {
        private final List<PendingMessage> savedMessages = new ArrayList<>();

        public FakeMessageDaoFactory() {
            super(null);
        }

        @Override
        public PendingMessagesDAO create(int partition) {
            return new FakePendingMessageDAO(savedMessages);
        }

        public List<PendingMessage> getSavedMessages() {
            return savedMessages;
        }
    }

    private static class FakeLogTypesResolver extends LogTypesResolver {
        public FakeLogTypesResolver() {
            super(null);
        }

        @Override
        public LogIdentifier getLogIdentifier(String logPropName) {
            return new LogIdentifier(null, null, LBInstallation.LOGBROKER);
        }
    }

    /**
     * Проверяем, что событие будет отправлено
     */
    @Test
    public void oneMessageIsSent() {
        NewAnswerOnModel answer = randomAnswer();
        marketUtilsClient.setResponse("[{\"uid\":" + answer.getQuestionAuthorPuid() + "}]");

        // вызов системы
        consumer.accept(Collections.singletonList(answer));

        assertThat(fakeMessageDao.getSavedMessages(), hasSize(1));
    }

    /**
     * Проверяем, что только одно событие будет отправлено при дублировании PUID-ов
     */
    @Test
    public void oneMessageIsSentForEmailDuplications() {
        NewAnswerOnModel answer = randomAnswer();
        long puid = answer.getQuestionAuthorPuid();
        marketUtilsClient.setResponse(String.format(
                "[{\"uid\":%d,\"email\":\"hello@example.com\"},{\"uid\":%d,\"email\":\"goodbye@example.com\"}]",
                puid, puid
        ));

        // вызов системы
        consumer.accept(Collections.singletonList(answer));

        assertThat(fakeMessageDao.getSavedMessages(), hasSize(1));
    }

    /**
     * Проверяем, что несколько событий будет отправлено, если подписчиков несколько
     */
    @Test
    public void severalMessagesAreSentForSeveralSubscribers() {
        NewAnswerOnModel answer = randomAnswer();
        marketUtilsClient.setResponse(String.format("[{\"uid\":%d},{\"uid\":656534},{\"uid\":23422}]",
                answer.getQuestionAuthorPuid()));

        // вызов системы
        consumer.accept(Collections.singletonList(answer));

        assertThat(fakeMessageDao.getSavedMessages(), hasSize(3));
    }

    /**
     * Проверяем, что событие не будет перенаправлено в {@link TriggerService}, если автор вопроса и автор ответа
     * совпадают
     */
    @Test
    public void checkMessageIsNotSentIfQuestionerAndAnswererEqual() {
        NewAnswerOnModel answer = new NewAnswerOnModel(1, 2, 3, 2);

        // вызов системы
        consumer.accept(Collections.singletonList(answer));

        assertThat(fakeMessageDao.getSavedMessages(), hasSize(0));
    }

    @Test
    public void bpmMessage() {
        NewAnswerOnModel answer = randomAnswer();

        UidBpmMessage result = consumer.asBpmMessage(answer, Uid.of(UidType.PUID,
                String.valueOf(answer.getQuestionAuthorPuid())));

        assertThat(result.getType(), is(MessageTypes.NEW_ANSWER_ON_QUESTION_MODEL));

        Uid uid = result.getUid();
        assertThat(uid.getType(), is(UidType.PUID));
        assertThat(uid.getValue(), is(String.valueOf(answer.getQuestionAuthorPuid())));

        Map<String, Object> variables = result.getVariables();
        assertThat(variables.get(ProcessVariablesNames.Event.NEW_ANSWER_ON_MODEL), is(answer));
    }

    @NotNull
    private NewAnswerOnModel randomAnswer() {
        return new NewAnswerOnModel(
                randomLong(),
                randomLong(),
                randomLong(),
                randomLong()
        );
    }

    private long randomLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Test
    public void transform() {
        String line = "tskv\tquestion_id=284759\tquestion_uid=13245928475\tanswer_id=2458729457\tanswer_uid=457298475";

        List<NewAnswerOnModel> result = consumer.transform(line.getBytes());

        assertThat(
                "Должны получить только один объект т.к. исходное сообщение содержало одну строку",
                result, hasSize(1)
        );

        NewAnswerOnModel answer = Iterables.get(result, 0);
        assertThat(answer.getQuestionId(), is(284759L));
        assertThat(answer.getQuestionAuthorPuid(), is(13245928475L));
        assertThat(answer.getAnswerId(), is(2458729457L));
        assertThat(answer.getAnswerAuthorPuid(), is(457298475L));
    }
}
