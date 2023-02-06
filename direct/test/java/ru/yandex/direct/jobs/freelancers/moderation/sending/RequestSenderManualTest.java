package ru.yandex.direct.jobs.freelancers.moderation.sending;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import one.util.streamex.LongStreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.testing.data.TestFreelancers;
import ru.yandex.direct.jobs.configuration.ManualTestingWithTvm;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerClientFactoryService;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerProducerProperties;
import ru.yandex.direct.jobs.freelancers.moderation.sending.model.CardModerationRequest;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;

import static ru.yandex.direct.core.entity.freelancer.service.FreelancerClientAvatarService.DEFAULT_AVATAR_SIZE_180;
import static ru.yandex.direct.core.testing.data.TestFreelancers.DEFAULT_AVATAR_ID;

@Disabled("Ходит в реальный логброкер")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ManualTestingWithTvm.class)
class RequestSenderManualTest {
    private static final Logger logger = LoggerFactory.getLogger(RequestSenderManualTest.class);

    @Autowired
    LogbrokerClientFactoryService logbrokerClientFactoryService;
    @Autowired
    DirectConfig directConfig;

    private long seqId;
    private String sourceId;
    private long cardId;

    @BeforeEach
    void setUp() throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName();
        String userName = System.getProperty("user.name");
        String className = this.getClass().getSimpleName();
        sourceId = String.join(":", hostName, userName, className);
        seqId = System.currentTimeMillis();
        cardId = System.currentTimeMillis();
    }

    @Test
    void execute_successRequestSending() throws Exception {
        //Подготавливаем запросы на модерацию
        Map<Long, String> avatarUrlByAvatarId = Collections.singletonMap(DEFAULT_AVATAR_ID, DEFAULT_AVATAR_SIZE_180);
        List<CardModerationRequest> requests = LongStreamEx.range(1, 6)
                .boxed()
                .map(TestFreelancers::defaultFreelancerCard)
                .map(card -> card.withId(cardId++))
                .map(card -> Converter.createRequest(card, avatarUrlByAvatarId))
                .toList();

        //Отправляем запросы
        Supplier<Long> seqIdSupplier = getSeqIdSupplier();
        LogbrokerProducerProperties producerProperties =
                LogbrokerProducerProperties.createInstance(directConfig, FreelancerCardSendingJob.CONFIG_SECTION_NAME);
        try (AsyncProducer asyncProducer = logbrokerClientFactoryService.createProducer(producerProperties, sourceId)) {
            RequestSender requestSender = new RequestSender(asyncProducer, seqIdSupplier);
            for (CardModerationRequest request : requests) {
                logger.info("Request: {}", JsonUtils.toJson(request));
                requestSender.send(request);
            }
        }
    }

    private Supplier<Long> getSeqIdSupplier() {
        return () -> {
            seqId++;
            return seqId;
        };
    }

}
