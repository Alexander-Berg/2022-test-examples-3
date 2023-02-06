package ru.yandex.direct.jobs.freelancers.moderation.receiving;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCardModeration;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerCardRepository;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerCardService;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerClientFactoryService;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerConsumerProperties;
import ru.yandex.direct.jobs.freelancers.moderation.receiving.model.VerdictType;
import ru.yandex.direct.result.Result;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason.BAD_DESCRIPTION;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason.BAD_HREF;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason.BAD_IMAGE;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate.ACCEPTED;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate.DECLINED;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.FREELANCER_CARDS_MODERATION_RECEIVING_ENABLED;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerCard;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@JobsTest
@ExtendWith(SpringExtension.class)
class FreelancerCardReceivingJobTest {
    private static final String BRIEF_INFO = "Тестовое кирилическое описание.";
    private static final String ANSWER_EXAMPLE_PATH = "freelancers/moderation/receiving/logbroker_verdict_answer.txt";

    @Autowired
    Steps steps;
    @Autowired
    FreelancerCardRepository freelancerCardRepository;
    @Autowired
    FreelancerCardVerdictHandler freelancerCardVerdictHandler;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    DirectConfig directConfig;

    @BeforeEach
    void init() {
        String jobName = FREELANCER_CARDS_MODERATION_RECEIVING_ENABLED.getName();
        String trueAsString = Boolean.toString(true);
        ppcPropertiesSupport.set(jobName, trueAsString);
    }

    private String getJsonVerdict(Long clientId,
                                  Long freelancerCardId,
                                  FreelancersCardStatusModerate cardStatusModerate,
                                  Set<FreelancersCardDeclineReason> declineReasons) {
        String verdict;
        switch (cardStatusModerate) {
            case ACCEPTED:
                verdict = "Yes";
                break;
            case DECLINED:
                verdict = "No";
                break;
            default:
                throw new IllegalArgumentException(
                        "Argument cardStatusModerate must have 'ACCEPTED' or 'DECLINED' value.");
        }
        String reason = StreamEx.of(declineReasons)
                .map(FreelancersCardDeclineReason::getModerationName)
                .map(strReason -> "\"" + strReason + "\"")
                .joining(",");
        String verdictPattern =
                "{\n"
                        + "  \"service\": \"direct\", \n"
                        + "  \"type\": \"freelancer_card\", \n"
                        + "  \"meta\": {\n"
                        + "    \"freelancer_card_id\": %1$d, \n"
                        + "    \"client_id\": %2$d \n"
                        + "  },\n"
                        + "  \"result\": {\n"
                        + "    \"verdict\": \"%3$s\", \n"
                        + "    \"reasons\": [%4$s]\n"
                        + "  } \n"
                        + "}";
        return String.format(verdictPattern, freelancerCardId, clientId, verdict, reason);
    }

    /**
     * Возвращает джобу с замоканным хождением в лог-брокер. Все остальные операции выполняются честно.
     */
    private FreelancerCardReceivingJob getJob(String jsonResponse) throws InterruptedException, TimeoutException {
        byte[] requestBody = jsonResponse.getBytes(StandardCharsets.UTF_8);
        MessageData messageData = mock(MessageData.class);
        when(messageData.getDecompressedData()).thenReturn(requestBody);
        List<MessageData> batchMessageData = singletonList(messageData);
        MessageBatch messageBatch = mock(MessageBatch.class);
        when(messageBatch.getMessageData()).thenReturn(batchMessageData);
        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        List<MessageBatch> messageBatches = singletonList(messageBatch);
        when(readResponse.getBatches()).thenReturn(messageBatches);
        when(readResponse.getCookie()).thenReturn(1L);
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        when(syncConsumer.read()).thenReturn(readResponse, (ConsumerReadResponse) null);
        LogbrokerClientFactoryService logbrokerClientFactoryService = mock(LogbrokerClientFactoryService.class);
        when(logbrokerClientFactoryService.createConsumer(any(LogbrokerConsumerProperties.class)))
                .thenReturn(syncConsumer);
        return new FreelancerCardReceivingJob(
                ppcPropertiesSupport,
                directConfig,
                logbrokerClientFactoryService,
                freelancerCardVerdictHandler);
    }

    /**
     * Возвращает джобу с замоканным хождением в лог-брокер и отдельно замоканным консьюмером на котором можно проверить,
     * был ли коммит на полученное сообщение.
     * Все остальные операции выполняются честно.
     */
    private FreelancerCardReceivingJob getJobWithMockConsumer(long cookie, String notVerdict, SyncConsumer syncConsumer)
            throws InterruptedException, TimeoutException {
        byte[] requestBody = notVerdict.getBytes(StandardCharsets.UTF_8);
        MessageData messageData = mock(MessageData.class);
        when(messageData.getDecompressedData()).thenReturn(requestBody);
        List<MessageData> batchMessageData = singletonList(messageData);
        MessageBatch messageBatch = mock(MessageBatch.class);
        when(messageBatch.getMessageData()).thenReturn(batchMessageData);
        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        List<MessageBatch> messageBatches = singletonList(messageBatch);
        when(readResponse.getBatches()).thenReturn(messageBatches);
        when(readResponse.getCookie()).thenReturn(cookie);
        when(syncConsumer.read()).thenReturn(readResponse, (ConsumerReadResponse) null);
        LogbrokerClientFactoryService logbrokerClientFactoryService = mock(LogbrokerClientFactoryService.class);
        when(logbrokerClientFactoryService.createConsumer(any(LogbrokerConsumerProperties.class)))
                .thenReturn(syncConsumer);
        return new FreelancerCardReceivingJob(
                ppcPropertiesSupport,
                directConfig,
                logbrokerClientFactoryService,
                freelancerCardVerdictHandler);
    }

    private String getLogbrokerVerdictAnswer() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream(ANSWER_EXAMPLE_PATH)) {
            checkNotNull(is, "Not found resource-file '%s'", ANSWER_EXAMPLE_PATH);
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    @Test
    void execute_successAcceptedSetting() throws Exception {
        //Подготавливаем объекты в базе (фрилансера и новую карточку) и исходные данные
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        int shard = freelancerInfo.getShard();
        Long freelancerId = freelancerInfo.getFreelancerId();
        FreelancerCard newCard = defaultFreelancerCard(freelancerId)
                .withBriefInfo(BRIEF_INFO)
                .withStatusModerate(FreelancersCardStatusModerate.IN_PROGRESS);
        freelancerCardRepository.addFreelancerCards(shard, singletonList(newCard));
        Long addedCardId = newCard.getId();

        //Подготваливаем ожидаемое состояние карточки
        FreelancerCard expectedCard = defaultFreelancerCard(freelancerId)
                .withId(addedCardId)
                .withBriefInfo(BRIEF_INFO)
                .withStatusModerate(ACCEPTED);

        //Выполняем джобу
        String jsonVerdict = getJsonVerdict(freelancerId, addedCardId, ACCEPTED, emptySet());
        FreelancerCardReceivingJob job = getJob(jsonVerdict);
        job.execute();

        //Получаем реальное состояние карточки в базе
        List<FreelancerCard> freelancerCards =
                freelancerCardRepository.getFreelancerCards(shard, singletonList(addedCardId));
        FreelancerCard readCard = getOnlyElement(freelancerCards, null);

        //Сверяем ожидания и реальность.
        assertThat(readCard)
                .is(matchedBy(beanDiffer(expectedCard)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    void execute_successDeclinedSetting() throws Exception {
        //Подготавливаем объекты в базе (фрилансера и новую карточку) и исходные данные
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        int shard = freelancerInfo.getShard();
        Long freelancerId = freelancerInfo.getFreelancerId();
        FreelancerCard newCard = defaultFreelancerCard(freelancerId)
                .withBriefInfo(BRIEF_INFO)
                .withStatusModerate(FreelancersCardStatusModerate.IN_PROGRESS);
        freelancerCardRepository.addFreelancerCards(shard, singletonList(newCard));
        Long addedCardId = newCard.getId();
        Set<FreelancersCardDeclineReason> allDeclineReasons =
                StreamEx.of(FreelancersCardDeclineReason.values())
                        .toSet();

        //Подготваливаем ожидаемое состояние карточки
        FreelancerCard expectedCard = defaultFreelancerCard(freelancerId)
                .withId(addedCardId)
                .withBriefInfo(BRIEF_INFO)
                .withStatusModerate(DECLINED)
                .withDeclineReason(allDeclineReasons);

        //Выполняем джобу
        String jsonVerdict = getJsonVerdict(freelancerId, addedCardId, DECLINED, allDeclineReasons);
        FreelancerCardReceivingJob job = getJob(jsonVerdict);
        job.execute();

        //Получаем реальное состояние карточки в базе
        List<FreelancerCard> freelancerCards =
                freelancerCardRepository.getFreelancerCards(shard, singletonList(addedCardId));
        FreelancerCard readCard = getOnlyElement(freelancerCards, null);

        //Сверяем ожидания и реальность.
        assertThat(readCard)
                .is(matchedBy(beanDiffer(expectedCard)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    void execute_notFailIfClientNotExist() throws Exception {
        String jsonVerdict = getJsonVerdict(999999L, 999999L, ACCEPTED, emptySet());
        FreelancerCardReceivingJob job = getJob(jsonVerdict);
        assertThatCode(() -> job.execute()).doesNotThrowAnyException();
    }

    /**
     * Тест на то, что в лог-брокер отправляется подтверждение об успешной обработке прочитанной записи.
     */
    @Test
    void execute_successAndInvoicingCommit() throws Exception {
        //Подготавливаем исходное состояние
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        int shard = freelancerInfo.getShard();
        Long freelancerId = freelancerInfo.getFreelancerId();
        FreelancerCard newCard = defaultFreelancerCard(freelancerId)
                .withBriefInfo(BRIEF_INFO)
                .withStatusModerate(FreelancersCardStatusModerate.IN_PROGRESS);
        freelancerCardRepository.addFreelancerCards(shard, singletonList(newCard));
        Long addedCardId = newCard.getId();
        String jsonVerdict = getJsonVerdict(freelancerId, addedCardId, ACCEPTED, emptySet());
        long cookie = 777L;

        //Выполняем джобу
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        FreelancerCardReceivingJob job = getJobWithMockConsumer(cookie, jsonVerdict, syncConsumer);
        job.execute();

        //Проверяем, что получение записи с соответствующей кукой было закомичено.
        ArgumentCaptor<Long> cookieCaptor = ArgumentCaptor.forClass(Long.class);
        verify(syncConsumer).commit(cookieCaptor.capture());
        assertThat(cookieCaptor.getAllValues())
                .contains(cookie);
    }

    /**
     * Тест на то, что в случае падения джобы во время применения вердикта к карточке фрилансера в БД,
     * в лог-брокер не посылается подтверждение об успешной обработке.
     */
    @Test
    void execute_failureAndNotInvoicingCommit() throws Exception {
        //Подготвливаем джобу которая падает на применении вердикта модерации к карточке в БД.
        String jsonVerdict = getJsonVerdict(1L, 2L, ACCEPTED, emptySet());
        byte[] requestBody = jsonVerdict.getBytes(StandardCharsets.UTF_8);
        MessageData messageData = mock(MessageData.class);
        when(messageData.getDecompressedData()).thenReturn(requestBody);
        List<MessageData> batchMessageData = singletonList(messageData);
        MessageBatch messageBatch = mock(MessageBatch.class);
        when(messageBatch.getMessageData()).thenReturn(batchMessageData);
        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        List<MessageBatch> messageBatches = singletonList(messageBatch);
        when(readResponse.getBatches()).thenReturn(messageBatches);
        when(readResponse.getCookie()).thenReturn(666L);
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        when(syncConsumer.read()).thenReturn(readResponse, (ConsumerReadResponse) null);
        LogbrokerClientFactoryService logbrokerClientFactoryService = mock(LogbrokerClientFactoryService.class);
        when(logbrokerClientFactoryService.createConsumer(any(LogbrokerConsumerProperties.class)))
                .thenReturn(syncConsumer);
        FreelancerCardVerdictHandler thrownVerdictHandler = mock(FreelancerCardVerdictHandler.class);
        when(thrownVerdictHandler.getSupportedType()).thenReturn(VerdictType.FREELANCER_CARD);
        when(thrownVerdictHandler.applyVerdict(any())).thenThrow(new RuntimeException());
        FreelancerCardReceivingJob job =
                new FreelancerCardReceivingJob(
                        ppcPropertiesSupport,
                        directConfig,
                        logbrokerClientFactoryService,
                        thrownVerdictHandler);

        //Проверяем, что получение записи на которой джоба упала не закомитится в лог-брокере.
        assertThatThrownBy(() -> job.execute()).isInstanceOf(RuntimeException.class);
        verify(syncConsumer, times(0)).commit(any(Long.class));
    }

    /**
     * Тест парсера и конвертера на реальном наборе вердиктов (лежат в ресурсах) от Модерации.
     */
    @Test
    void execute_successAndNotInvoicingCommit() throws Exception {
        //Подготавливаем данные
        ArrayList<FreelancerCardModeration> cardModerationList = new ArrayList<>();
        String jsonVerdict = getLogbrokerVerdictAnswer();

        //Выполняем замоканую джобу
        byte[] requestBody = jsonVerdict.getBytes(StandardCharsets.UTF_8);
        MessageData messageData = mock(MessageData.class);
        when(messageData.getDecompressedData()).thenReturn(requestBody);
        List<MessageData> batchMessageData = singletonList(messageData);
        MessageBatch messageBatch = mock(MessageBatch.class);
        when(messageBatch.getMessageData()).thenReturn(batchMessageData);
        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        List<MessageBatch> messageBatches = singletonList(messageBatch);
        when(readResponse.getBatches()).thenReturn(messageBatches);
        when(readResponse.getCookie()).thenReturn(666L);
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        when(syncConsumer.read()).thenReturn(readResponse, (ConsumerReadResponse) null);
        LogbrokerClientFactoryService logbrokerClientFactoryService = mock(LogbrokerClientFactoryService.class);
        when(logbrokerClientFactoryService.createConsumer(any(LogbrokerConsumerProperties.class)))
                .thenReturn(syncConsumer);
        FreelancerCardService freelancerCardService = mock(FreelancerCardService.class);
        // ArgumentCaptor не позволяет получить аргументы сразу нескольких вызовов метода, поэтому использован этот подход
        when(freelancerCardService.applyModerationResult(any(FreelancerCardModeration.class)))
                .thenAnswer((InvocationOnMock invocation) -> {
                    FreelancerCardModeration freelancerCardModeration = invocation.getArgument(0);
                    cardModerationList.add(freelancerCardModeration);
                    return Result.successful(1L);
                });
        FreelancerCardVerdictHandler freelancerCardVerdictHandler =
                new FreelancerCardVerdictHandler(freelancerCardService);
        FreelancerCardReceivingJob job =
                new FreelancerCardReceivingJob(
                        ppcPropertiesSupport,
                        directConfig,
                        logbrokerClientFactoryService,
                        freelancerCardVerdictHandler);
        job.execute();

        //Проверяем, что ответ Модерации с множеством вердиктов был распарщен и сконвертирован правильно.
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(cardModerationList).hasSize(5);
            for (FreelancerCardModeration moderationResult : cardModerationList) {
                soft.assertThat(moderationResult.getId()).isGreaterThan(0);
                soft.assertThat(moderationResult.getFreelancerId()).isGreaterThan(0);
                soft.assertThat(moderationResult.getStatusModerate()).isIn(ACCEPTED, DECLINED);
                if (Objects.equals(moderationResult.getStatusModerate(), ACCEPTED)) {
                    soft.assertThat(moderationResult.getDeclineReason()).isEmpty();
                } else {
                    soft.assertThat(moderationResult.getDeclineReason())
                            .containsExactlyInAnyOrder(BAD_IMAGE, BAD_DESCRIPTION, BAD_HREF);
                }
            }
        });
    }

    /**
     * Тест на то, что джоба просто пропускает вердикты неизвестного типа и не падает на них.
     */
    @Test
    void execute_passStrangeVerdict() throws Exception {
        //Исходные данные (левый вердикт и кука)
        long cookie = 777L;
        //noinspection SpellCheckingInspection
        String strangeVerdict = "{\n"
                + "  \"service\": \"direct\", \n"
                + "  \"type\": \"abra_shvabra_cadabra!\", \n"
                + "  \"meta\": {\n"
                + "    \"freelancer_card_id\": 1111, \n"
                + "    \"client_id\": 2222 \n"
                + "  },\n"
                + "  \"result\": {\n"
                + "    \"verdict\": 3333, \n"
                + "    \"reasons\": 4444\n"
                + "  } \n"
                + "}";

        //Выполняем джобу
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        FreelancerCardReceivingJob job = getJobWithMockConsumer(cookie, strangeVerdict, syncConsumer);
        job.execute();

        //Проверяем, что получение записи с соответствующей кукой было закомичено.
        ArgumentCaptor<Long> cookieCaptor = ArgumentCaptor.forClass(Long.class);
        verify(syncConsumer).commit(cookieCaptor.capture());
        assertThat(cookieCaptor.getAllValues())
                .contains(cookie);
    }

    /**
     * Тест на то, что если из топика пришёл вообще не вердикт, то джоба его просто пропустит.
     */
    @Test
    void execute_passNotVerdict() throws Exception {
        //Исходные данные (левый вердикт и кука)
        long cookie = 777L;
        //noinspection SpellCheckingInspection
        String notVerdict = "abra_shvabra_cadabra!";

        //Выполняем джобу
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        FreelancerCardReceivingJob job = getJobWithMockConsumer(cookie, notVerdict, syncConsumer);
        job.execute();

        //Проверяем, что получение записи с соответствующей кукой было закомичено.
        ArgumentCaptor<Long> cookieCaptor = ArgumentCaptor.forClass(Long.class);
        verify(syncConsumer).commit(cookieCaptor.capture());
        assertThat(cookieCaptor.getAllValues())
                .contains(cookie);
    }

}
