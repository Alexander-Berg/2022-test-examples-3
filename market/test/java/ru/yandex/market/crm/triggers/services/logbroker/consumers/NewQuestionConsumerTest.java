package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.platform.PlatformApiClient;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.models.ProductByIdPurchaseAntifraud;
import ru.yandex.market.crm.platform.models.PurchaseInfo;
import ru.yandex.market.crm.platform.profiles.Facts;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewQuestionOnModel;
import ru.yandex.market.crm.triggers.services.logbroker.consumers.NewQuestionConsumer.PurchaseInfoSource;
import ru.yandex.market.crm.triggers.services.logbroker.consumers.NewQuestionConsumer.Question;
import ru.yandex.market.crm.triggers.services.saas.SaasService;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.platform.commons.UidType.PUID;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.NewQuestionConsumer.PurchaseInfoSource.ANTRIFRAUD;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.NewQuestionConsumer.PurchaseInfoSource.REVIEW;

@RunWith(MockitoJUnitRunner.class)
public class NewQuestionConsumerTest {

    private NewQuestionConsumer consumer;

    @Mock
    private MessageSender messageSender;

    @Mock
    private SaasService saasService;

    @Mock
    private PlatformApiClient platformApiClient;

    @Before
    public void before() {
        LogTypesResolver logTypes = mock(LogTypesResolver.class);
        when(logTypes.getLogIdentifier("pers.newQuestionLog"))
                .thenReturn(new LogIdentifier(null, null, LBInstallation.LOGBROKER));
        consumer = new NewQuestionConsumer(messageSender, platformApiClient, saasService, logTypes);
    }

    @Test
    public void testTransform() {
        String line = "tskv\tmodel_id=1\tquestion_id=2\tquestion_uid=3";
        List<Question> transformed = consumer.transform(line.getBytes());
        assertThat(transformed, hasSize(1));

        Question mes = transformed.get(0);
        assertEquals("1", mes.modelId);
        assertEquals(2, mes.questionId);
        assertEquals(3, mes.questionAuthorUid);
    }

    @Test
    public void testBpmMessageConvert() {
        Question question = new Question("1", 2, 3);

        UidBpmMessage bpmMessage = consumer.createNewQuestionOnReviewedModelBpmMessage(3L, Collections.emptySet(),
                question);
        assertEquals(MessageTypes.NEW_QUESTION_ON_REVIEWED_MODEL, bpmMessage.getType());
        assertEquals(Uid.of(UidType.PUID, "3"), bpmMessage.getUid());

        Map<String, Object> variables = bpmMessage.getVariables();
        assertEquals(new NewQuestionOnModel(2, "1", 3),
                variables.get(ProcessVariablesNames.Event.NEW_QUESTION_ON_MODEL));
    }

    @Test
    public void testMessageSentWithCorrectSources() {
        when(saasService.getModelAllReviewersUids("1")).thenReturn(new LongArraySet(Set.of(111L, 222L)));

        long timestamp = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
        ProductByIdPurchaseAntifraud antifraudInfo = ProductByIdPurchaseAntifraud.newBuilder()
                .addPurchaseInfo(PurchaseInfo.newBuilder().addUid(Uids.create(PUID, 222)).setTimestamp(timestamp))
                .addPurchaseInfo(PurchaseInfo.newBuilder().addUid(Uids.create(PUID, 333)).setTimestamp(timestamp))
                .build();

        Facts platformResponse = Facts.newBuilder().addProductByIdPurchaseAntifraud(antifraudInfo).build();
        when(platformApiClient.getGenericFact(anyObject(), eq("1")))
                .thenReturn(CompletableFuture.completedFuture(platformResponse));

        consumer.accept(Collections.singletonList(new Question("1", 10, 20)));

        ArgumentCaptor<List<UidBpmMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageSender).send(captor.capture());

        List<UidBpmMessage> messages = captor.getValue();
        hasMessageWithPuidAndSources(messages, 111L, Set.of(REVIEW));
        hasMessageWithPuidAndSources(messages, 222L, Set.of(REVIEW, ANTRIFRAUD));
        hasMessageWithPuidAndSources(messages, 333L, Set.of(ANTRIFRAUD));
    }

    @Test
    public void testBpmMessageSent() {
        Question question = new Question("1", 2, 3);

        // текущая модель содержит отзыв от двух пользователей
        LongArraySet reviewers = new LongArraySet(Sets.newHashSet(1L, 2L));
        when(saasService.getModelAllReviewersUids(question.modelId)).thenReturn(reviewers);

        consumer.accept(Collections.singletonList(question));

        // должно было послаться по одному событию для каждого автора обзора на модель
        verify(messageSender).send(argThat(messages -> messages.size() == 2));
    }

    private void hasMessageWithPuidAndSources(List<UidBpmMessage> messages,
                                              long puid,
                                              Set<PurchaseInfoSource> sources) {
        Set<String> sourceValue = sources.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        boolean contains = messages.stream()
                .filter(m -> m.getUid().equals(Uid.asPuid(puid)))
                .map(m -> (Set<String>) m.getVariables()
                        .getOrDefault(ProcessVariablesNames.PRODUCT_PURCHASE_SOURCES, new HashSet<>())
                )
                .anyMatch(s -> s.containsAll(sourceValue));

        assertTrue(contains);
    }
}
