package ru.yandex.market.arbiter.test.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.LockTimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.arbiter.api.mapper.ArbiterMapper;
import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.server.dto.NotificationChannelDto;
import ru.yandex.market.arbiter.jpa.entity.ConversationEntity;
import ru.yandex.market.arbiter.jpa.entity.NotificationChannelEntity;
import ru.yandex.market.arbiter.jpa.repository.AttachmentRepository;
import ru.yandex.market.arbiter.jpa.repository.BaseRepository;
import ru.yandex.market.arbiter.jpa.repository.ConversationRepository;
import ru.yandex.market.arbiter.jpa.repository.MerchantRepository;
import ru.yandex.market.arbiter.jpa.repository.MessageRepository;
import ru.yandex.market.arbiter.jpa.repository.NotificationChannelRepository;
import ru.yandex.market.arbiter.jpa.repository.SubjectRepository;
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.util.TestUtil;
import ru.yandex.market.arbiter.workflow.impl.LockService;

/**
 * @author moskovkin@yandex-team.ru
 * @since 15.05.2020
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class RepositorySmokeTest extends BaseUnitTest {
    private static final int PAEALLEL_MODIFICATIONS_COUNT = 10;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private NotificationChannelRepository notificationChannelRepository;

    @Autowired
    private LockService lock;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ArbiterMapper mapper;

    @Test
    public void testFindByExapmle() {
        TestDataService.TestData testData = testDataService.saveTestData();
        testFindByExample(conversationRepository, mapper.mapToConversationEntity(testData.someConversation()));
        testFindByExample(merchantRepository, mapper.mapToMerchantEntity(testData.someMerchant()));
        testFindByExample(subjectRepository, mapper.mapToSubjectEntity(testData.someSubject()));
        testFindByExample(messageRepository, mapper.mapToMessageEntity(testData.someMessage()));
        testFindByExample(attachmentRepository, mapper.mapToAttachmentEntity(testData.someAttachment()));
    }

    @Test
    public void testGetSingleField() {
        TestDataService.TestData testData = testDataService.saveTestData();

        Optional<ConversationEntity.Status> found = conversationRepository.findStatusById(
                testData.someConversation().getId()
        );
        Assertions.assertThat(found)
                .isNotEmpty()
                .containsInstanceOf(ConversationEntity.Status.class);

        Optional<ConversationEntity.Status> notFound = conversationRepository.findStatusById(
                testData.notExistingConversationId()
        );
        Assertions.assertThat(notFound)
                .isEmpty();
    }

    @Test
    public void testConversationOptimisticLocking() {
        TestDataService.TestData testData = testDataService.saveTestData();

        AtomicInteger i = new AtomicInteger(0);
        TestUtil.ParallelCallResults<Boolean> results = TestUtil.doParallelCalls(PAEALLEL_MODIFICATIONS_COUNT, () -> {
            return transactionTemplate.execute(status -> {
                lock.lockByConversationId(testData.someConversationId());
                ConversationEntity conversation = conversationRepository.findById(testData.someConversationId())
                        .orElseThrow();
                conversation.setDescription("New value: " + i.getAndIncrement());
                return true;
            });
        });

        // We should have exceptions thrown in parallel calls, but only ObjectOptimisticLockingFailureException
        Assertions.assertThat(results.getErrors())
                .isNotEmpty()
                .hasOnlyElementsOfType(LockTimeoutException.class);

        // Some modifications was successful
        Assertions.assertThat(results.getResults())
                .isNotEmpty();
    }

    @Test
    public void testFindConversationByBusinesschatId() {
        TestDataService.TestData testData = testDataService.saveTestData();
        ArbiterConversationDto conversation = testData.someConversation();
        NotificationChannelDto notificationChannel = conversation.getNotificationChannels().get(0);

        transactionTemplate.execute(status -> {
            Optional<NotificationChannelEntity> found =
                    notificationChannelRepository.findByBusinesschatIdAndRecipientId(
                        notificationChannel.getBusinesschatParams().getChatId(),
                        notificationChannel.getBusinesschatParams().getRecipientId()
                    );

            Assertions.assertThat(found).isNotEmpty();
            NotificationChannelEntity foundNotificationChannelEntity = found.get();

            Assertions.assertThat(foundNotificationChannelEntity).usingRecursiveComparison()
                    .ignoringFields("id", "creationTime", "conversation")
                    .ignoringAllOverriddenEquals()
                    .isEqualTo(notificationChannel);

            Assertions.assertThat(foundNotificationChannelEntity.getConversation().getId())
                    .isEqualTo(conversation.getId());
            return null;
        });
    }

    @Test
    public void testChannelExistsByBusinesschatId() {
        TestDataService.TestData testData = testDataService.saveTestData();
        ArbiterConversationDto conversation = testData.someConversation();
        NotificationChannelDto notificationChannel = conversation.getNotificationChannels().get(0);

        transactionTemplate.execute(status -> {
            boolean foundNotExisting = notificationChannelRepository.existsByBusinesschatId(
                    "NOT_EXISTING_CHAT_ID"
            );

            boolean foundExisting = notificationChannelRepository.existsByBusinesschatId(
                    notificationChannel.getBusinesschatParams().getChatId()
            );

            Assertions.assertThat(foundExisting).isTrue();
            Assertions.assertThat(foundNotExisting).isFalse();

            return null;
        });
    }


    private <E> void testFindByExample(BaseRepository<E, ?> repository, E entity) {
        List<E> found = repository.findAll(Example.of(entity));
        Assertions.assertThat(found).isNotEmpty();
    }
}
