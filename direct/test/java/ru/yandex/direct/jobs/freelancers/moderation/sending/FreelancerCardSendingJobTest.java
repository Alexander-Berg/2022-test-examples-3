package ru.yandex.direct.jobs.freelancers.moderation.sending;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.freelancer.model.ClientAvatar;
import ru.yandex.direct.core.entity.freelancer.model.ClientAvatarId;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerContacts;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.core.entity.freelancer.repository.ClientAvatarRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerCardRepository;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerClientAvatarService;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerClientFactoryService;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerProducerProperties;
import ru.yandex.direct.jobs.freelancers.moderation.sending.model.CardModerationRequest;
import ru.yandex.direct.jobs.freelancers.moderation.sending.model.RequestData;
import ru.yandex.direct.jobs.freelancers.moderation.sending.model.RequestMeta;
import ru.yandex.direct.jobs.freelancers.moderation.sending.model.Workflow;
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;
import ru.yandex.direct.scheduler.support.PeriodicJobWrapper;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.FREELANCER_CARDS_MODERATION_SENDING_ENABLED;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerCard;
import static ru.yandex.direct.core.testing.steps.AvatarSteps.defaultClientAvatar;
import static ru.yandex.direct.scheduler.support.DirectShardedJob.SHARD_PARAM;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@JobsTest
@ExtendWith(SpringExtension.class)
class FreelancerCardSendingJobTest {
    private static final String BRIEF_INFO = "Тестовое кирилическое описание.";
    private static final String SITE_URL = "https://direct.yandex.ru";

    @Autowired
    ShardHelper shardHelper;
    @Autowired
    Steps steps;
    @Autowired
    FreelancerCardRepository freelancerCardRepository;
    @Autowired
    ClientAvatarRepository clientAvatarRepository;
    @Autowired
    FreelancerClientAvatarService freelancerClientAvatarService;
    @Autowired
    FreelancerCardModerationService freelancerCardModerationService;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    LbSeqIdService lbSeqIdService;
    @Autowired
    DirectConfig directConfig;

    @BeforeEach
    void init() {
        initMocks(this);
        String jobName = FREELANCER_CARDS_MODERATION_SENDING_ENABLED.getName();
        String trueAsString = Boolean.toString(true);
        ppcPropertiesSupport.set(jobName, trueAsString);
    }

    @Test
    void execute_successRequestSending() throws Exception {
        //Создаём объекты в базе (фрилансера, аватарку и новую карточку с этой аватаркой)
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        int shard = freelancerInfo.getShard();
        Long freelancerId = freelancerInfo.getFreelancerId();
        FreelancerCard addedCard = addNewFreelancerCard(shard, freelancerId);
        Long addedCardId = addedCard.getId();
        ClientAvatarId avatarId = new ClientAvatar()
                .withId(addedCard.getAvatarId())
                .withClientId(freelancerId);
        String addedAvatarUrl = freelancerClientAvatarService.getUrlSize180(avatarId);

        //Подготваливаем ожидаемый запрос на модерацию
        CardModerationRequest expectingRequest = new CardModerationRequest()
                .withWorkflow(Workflow.COMMON)
                .withMeta(new RequestMeta()
                        .withClientId(freelancerId)
                        .withFreelancerCardId(addedCard.getId()))
                .withData(new RequestData()
                        .withDescription(BRIEF_INFO)
                        .withHref(SITE_URL)
                        .withImgUrl(addedAvatarUrl));

        //Выполняем джобу
        @SuppressWarnings("unchecked")
        CompletableFuture<ProducerWriteResponse> completableFuture =
                (CompletableFuture<ProducerWriteResponse>) mock(CompletableFuture.class);
        AsyncProducer asyncProducer = mock(AsyncProducer.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        when(asyncProducer.write(bodyCaptor.capture(), anyLong())).thenReturn(completableFuture);
        FreelancerCardSendingJob job = getJob(asyncProducer);
        TaskParametersMap shardContext = getShardContext(shard);
        new PeriodicJobWrapper(job).execute(shardContext);

        //Выпарщиваем реально отправленный на модерацию запрос с созданной нами карточкой
        List<byte[]> bodyAllValues = bodyCaptor.getAllValues();
        CardModerationRequest sentRequest = StreamEx.of(bodyAllValues)
                .map(byteArray -> JsonUtils.fromJson(byteArray, CardModerationRequest.class))
                .filter(request -> request.getMeta().getFreelancerCardId().equals(addedCardId))
                .findAny()
                .orElse(null);

        //Сверяем ожидания и реальность.
        assertThat(sentRequest)
                .is(matchedBy(beanDiffer(expectingRequest)
                        .useCompareStrategy(onlyExpectedFields())));

    }

    @Test
    void execute_successSeqIdSendingAndRaising() throws Exception {
        //Создаём объекты в базе (фрилансера с новой карточкой и seqId)
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        int shard = freelancerInfo.getShard();
        Long freelancerId = freelancerInfo.getFreelancerId();
        addNewFreelancerCard(shard, freelancerId);
        String shardAsString = Integer.toString(shard);
        String propertyKey = LbSeqIdService.SEQ_ID_PROPERTY_KEY_PATTERN + shardAsString;
        String startPropertyValue = ppcPropertiesSupport.get(propertyKey);
        if (isBlank(startPropertyValue)) {
            ppcPropertiesSupport.set(propertyKey, "8888");
            startPropertyValue = ppcPropertiesSupport.get(propertyKey);
        }
        @SuppressWarnings("ConstantConditions")
        long startBdSeqId = Long.parseLong(startPropertyValue);

        //Выполняем джобу
        @SuppressWarnings("unchecked")
        CompletableFuture<ProducerWriteResponse> completableFuture =
                (CompletableFuture<ProducerWriteResponse>) mock(CompletableFuture.class);
        AsyncProducer asyncProducer = mock(AsyncProducer.class);
        when(asyncProducer.write(any(byte[].class), anyLong())).thenReturn(completableFuture);
        FreelancerCardSendingJob job = getJob(asyncProducer);
        TaskParametersMap shardContext = getShardContext(shard);
        new PeriodicJobWrapper(job).execute(shardContext);
        ArgumentCaptor<Long> seqCaptor = ArgumentCaptor.forClass(Long.class);
        verify(asyncProducer).write(any(byte[].class), seqCaptor.capture());

        //Получаем первый отправленный seqId и итоговое значение в базе
        List<Long> allValues = seqCaptor.getAllValues();
        long firstSentSeqId = allValues.get(0);
        String endPropertyValue = ppcPropertiesSupport.get(propertyKey);
        @SuppressWarnings("ConstantConditions")
        long endBdSeqId = Long.parseLong(endPropertyValue);

        //Проверяем, что оба значения увеличились по сравнению с начальным
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstSentSeqId)
                    .isGreaterThan(startBdSeqId);
            soft.assertThat(endBdSeqId)
                    .isGreaterThan(startBdSeqId);
        });
    }

    @Test
    void execute_successStatusModerateSetting() throws Exception {
        //Создаём объекты в базе (фрилансера и новую карточку)
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        int shard = freelancerInfo.getShard();
        Long freelancerId = freelancerInfo.getFreelancerId();
        FreelancerCard addedCard = addNewFreelancerCard(shard, freelancerId);
        Long addedCardId = addedCard.getId();

        //Подготваливаем ожидаемое состояние карточки
        FreelancerCard expectedCard = new FreelancerCard()
                .withId(addedCardId)
                .withFreelancerId(freelancerId)
                .withBriefInfo(BRIEF_INFO)
                .withAvatarId(addedCard.getAvatarId())
                .withContacts(new FreelancerContacts()
                        .withSiteUrl(SITE_URL))
                .withStatusModerate(FreelancersCardStatusModerate.IN_PROGRESS)
                .withIsArchived(false);

        //Выполняем джобу
        @SuppressWarnings("unchecked")
        CompletableFuture<ProducerWriteResponse> completableFuture =
                (CompletableFuture<ProducerWriteResponse>) mock(CompletableFuture.class);
        AsyncProducer asyncProducer = mock(AsyncProducer.class);
        when(asyncProducer.write(any(byte[].class), anyLong())).thenReturn(completableFuture);
        FreelancerCardSendingJob job = getJob(asyncProducer);
        TaskParametersMap shardContext = getShardContext(shard);
        new PeriodicJobWrapper(job).execute(shardContext);

        //Получаем реальное состояние карточки в базе
        List<FreelancerCard> freelancerCards =
                freelancerCardRepository.getFreelancerCards(shard, singletonList(addedCardId));
        FreelancerCard readCard = getOnlyElement(freelancerCards, null);

        //Сверяем ожидания и реальность.
        assertThat(readCard)
                .is(matchedBy(beanDiffer(expectedCard)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private FreelancerCard addNewFreelancerCard(int shard, long freelancerId) {
        ClientAvatar clientAvatar = defaultClientAvatar()
                .withId(shardHelper.generateClientAvatarIds(1).get(0))
                .withClientId(freelancerId)
                .withExternalId("test/test/test");
        clientAvatarRepository.add(shard, singletonList(clientAvatar));
        FreelancerCard newCard = defaultFreelancerCard(freelancerId)
                .withFreelancerId(freelancerId)
                .withAvatarId(clientAvatar.getId())
                .withBriefInfo(BRIEF_INFO)
                .withStatusModerate(FreelancersCardStatusModerate.DRAFT)
                .withIsArchived(false);
        newCard.getContacts().withSiteUrl(SITE_URL);
        freelancerCardRepository.addFreelancerCards(shard, singletonList(newCard));
        return newCard;
    }

    private FreelancerCardSendingJob getJob(AsyncProducer asyncProducer) throws Exception {
        LogbrokerClientFactoryService logbrokerClientFactoryService = mock(LogbrokerClientFactoryService.class);
        when(logbrokerClientFactoryService.createProducer(any(LogbrokerProducerProperties.class), any(String.class)))
                .thenReturn(asyncProducer);
        return new FreelancerCardSendingJob(
                ppcPropertiesSupport,
                directConfig,
                freelancerCardModerationService,
                logbrokerClientFactoryService,
                lbSeqIdService);
    }

    private TaskParametersMap getShardContext(int shard) {
        return TaskParametersMap.of(SHARD_PARAM, String.valueOf(shard));
    }

}
