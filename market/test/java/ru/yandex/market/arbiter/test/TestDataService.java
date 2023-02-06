package ru.yandex.market.arbiter.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.arbiter.api.server.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.server.dto.AttachmentDto;
import ru.yandex.market.arbiter.api.server.dto.CreateConversationRequestDto;
import ru.yandex.market.arbiter.api.server.dto.MerchantDto;
import ru.yandex.market.arbiter.api.server.dto.MessageDto;
import ru.yandex.market.arbiter.api.server.dto.SubjectDto;
import ru.yandex.market.arbiter.jpa.repository.AttachmentRepository;
import ru.yandex.market.arbiter.jpa.repository.AuditRepository;
import ru.yandex.market.arbiter.jpa.repository.ConversationRepository;
import ru.yandex.market.arbiter.jpa.repository.MerchantRepository;
import ru.yandex.market.arbiter.jpa.repository.MessageRepository;
import ru.yandex.market.arbiter.jpa.repository.NotificationChannelRepository;
import ru.yandex.market.arbiter.jpa.repository.NotificationRepository;
import ru.yandex.market.arbiter.jpa.repository.SubjectRepository;
import ru.yandex.market.arbiter.jpa.repository.VerdictRepository;
import ru.yandex.market.arbiter.jpa.repository.WaitingRepository;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.test.util.RandomUtil;
import ru.yandex.market.arbiter.workflow.Workflow;

import static ru.yandex.market.arbiter.test.util.RandomUtil.randomItem;

/**
 * @author moskovkin@yandex-team.ru
 * @since 24.05.2020
 */
@RequiredArgsConstructor
public class TestDataService {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestDataService.class).build();
    public static final int MERCHANTS_AMOUNT = 3;
    public static final int SUBJECTS_AMOUNT = 5;
    public static final int CONVERSATIONS_AMOUNT = 10;
    public static final int MESSAGES_AMOUNT = CONVERSATIONS_AMOUNT * 10;
    public static final int ATTACHMENTS_AOUNT = MESSAGES_AMOUNT / 2;

    @Autowired
    private final AttachmentRepository attachmentRepository;

    @Autowired
    private final MessageRepository messageRepository;

    @Autowired
    private final WaitingRepository waitingRepository;

    @Autowired
    private final VerdictRepository verdictRepository;

    @Autowired
    private final SubjectRepository subjectRepository;

    @Autowired
    private final MerchantRepository merchantRepository;

    @Autowired
    private final ConversationRepository conversationRepository;

    @Autowired
    private final NotificationChannelRepository notificationChannelRepository;

    @Autowired
    private final NotificationRepository notificationRepository;

    @Autowired
    private final AuditRepository auditRepository;

    @Autowired
    private final Workflow workflow;

    public TestData saveTestData() {
        List<MerchantDto> merchants = RANDOM.objects(MerchantDto.class, MERCHANTS_AMOUNT)
                .collect(Collectors.toUnmodifiableList());

        List<SubjectDto> subjects = RANDOM.objects(SubjectDto.class, SUBJECTS_AMOUNT)
                .collect(Collectors.toUnmodifiableList());

        List<CreateConversationRequestDto> createConversationRequests =
                RANDOM.objects(CreateConversationRequestDto.class, CONVERSATIONS_AMOUNT)
                        .peek(c -> c.setMessages(new ArrayList<>()))
                        .collect(Collectors.toUnmodifiableList());

        for (int i = 0; i < createConversationRequests.size(); i++) {
            createConversationRequests.get(i).setMerchant(merchants.get(i % merchants.size()));
            createConversationRequests.get(i).setSubject(subjects.get(i % subjects.size()));
        }

        List<MessageDto> messages = RANDOM.objects(MessageDto.class, MESSAGES_AMOUNT)
                .peek(m -> m.setAttachments(new ArrayList<>()))
                .peek(m -> randomItem(RANDOM, createConversationRequests).getMessages().add(m))
                .collect(Collectors.toUnmodifiableList());

        List<AttachmentDto> attachments = RANDOM.objects(AttachmentDto.class, ATTACHMENTS_AOUNT)
                .peek(a -> randomItem(RANDOM, messages).getAttachments().add(a))
                .collect(Collectors.toUnmodifiableList());

        Long arbiterUid = RANDOM.nextLong();
        List<ArbiterConversationDto> conversations = createConversationRequests.stream()
                .map(workflow::addConversation)
                .peek(id -> workflow.arbiterInProgress(arbiterUid, id))
                .map(workflow::getConversation)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableList());

        return new TestData()
                .setConversations(conversations)
                .setSubjects(subjects)
                .setMerchants(merchants)
                .setMessages(messages)
                .setAttachments(attachments);
    }

    public void cleanDatabase() {
        auditRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        notificationChannelRepository.deleteAllInBatch();
        attachmentRepository.deleteAllInBatch();
        messageRepository.deleteAllInBatch();
        waitingRepository.deleteAllInBatch();
        verdictRepository.deleteAllInBatch();
        conversationRepository.deleteAllInBatch();
        merchantRepository.deleteAllInBatch();
        subjectRepository.deleteAllInBatch();
    }

    @Data @RequiredArgsConstructor @Accessors(chain = true)
    public static class TestData {
        private List<MerchantDto> merchants;
        private List<ArbiterConversationDto> conversations;
        private List<SubjectDto> subjects;
        private List<MessageDto> messages;
        private List<AttachmentDto> attachments;

        public ArbiterConversationDto someConversation() {
            return conversations.get(0);
        }

        public Long someConversationId() {
            return conversations.get(0).getId();
        }

        public MerchantDto someMerchant() {
            return merchants.get(0);
        }

        public SubjectDto someSubject() {
            return subjects.get(0);
        }

        public MessageDto someMessage() {
            return messages.get(0);
        }

        public AttachmentDto someAttachment() {
            return attachments.get(0);
        }

        public Long notExistingConversationId() {
            return notExistingId(conversations.stream().map(ArbiterConversationDto::getId));
        }

        public Long notExistingMessageId() {
            return notExistingId(messages.stream().map(MessageDto::getId));
        }

        public Long notExistingSubjectId() {
            return notExistingId(subjects.stream().map(SubjectDto::getId));
        }

        public Long notMerhantSubjectId() {
            return notExistingId(merchants.stream().map(MerchantDto::getId));
        }

        private Long notExistingId(Stream<Long>existingIds) {
            return RandomUtil.someNotExistingValue(
                    RANDOM, Long.class,
                    existingIds.collect(Collectors.toSet())
            );
        }
    }
}
