package ru.yandex.direct.jobs.freelancers.moderation.receiving;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.jobs.configuration.ManualTestingWithTvm;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerClientFactoryService;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerConsumerProperties;
import ru.yandex.direct.jobs.freelancers.moderation.LogbrokerProducerProperties;
import ru.yandex.direct.jobs.freelancers.moderation.receiving.model.CardModerationVerdict;
import ru.yandex.direct.jobs.freelancers.moderation.receiving.model.VerdictType;
import ru.yandex.direct.jobs.freelancers.moderation.sending.FreelancerCardSendingJob;
import ru.yandex.direct.utils.InterruptedRuntimeException;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Disabled("Ходит в реальный логброкер")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ManualTestingWithTvm.class)
class VerdictReceiverManualTest {
    private static final Logger logger = LoggerFactory.getLogger(VerdictReceiverManualTest.class);
    private static final long SENDING_TIMEOUT_SEC = 20;
    private static final String POSOTIVE_VERDICT =
            "{\"service\":\"direct\",\"type\":\"freelancer_card\",\"meta\":{\"freelancer_card_id\":12345,\"client_id\":5678},\"result\":{\"verdict\":\"Yes\",\"reason\":\"\"}}";
    private static final String NEGATIVE_VERDICT =
            "{\"service\":\"direct\",\"type\":\"freelancer_card\",\"meta\":{\"freelancer_card_id\":12345,\"client_id\":5678},\"result\":{\"verdict\":\"No\",\"reason\":\"bad_img,bad_text,bad_href\"}}";
    private static final List<String> JSON_VERDICTS = Arrays.asList(POSOTIVE_VERDICT, NEGATIVE_VERDICT);

    @Autowired
    LogbrokerClientFactoryService logbrokerClientFactoryService;
    @Autowired
    DirectConfig directConfig;

    @Test
    void read_successVerdictReceiving()
            throws TimeoutException, ExecutionException, UnknownHostException {
        //Записываем вердикты в топик
        String hostName = InetAddress.getLocalHost().getHostName();
        String userName = System.getProperty("user.name");
        String className = this.getClass().getSimpleName();
        String sourceId = String.join(":", hostName, userName, className);
        long seqId = System.currentTimeMillis();
        LogbrokerProducerProperties producerProperties =
                LogbrokerProducerProperties.createInstance(directConfig, FreelancerCardSendingJob.CONFIG_SECTION_NAME);
        try (AsyncProducer asyncProducer = logbrokerClientFactoryService.createProducer(producerProperties, sourceId)) {
            for (String verdict : JSON_VERDICTS) {
                byte[] bytes = verdict.getBytes(StandardCharsets.UTF_8);
                CompletableFuture<ProducerWriteResponse> future = asyncProducer.write(bytes, seqId++);
                future.get(SENDING_TIMEOUT_SEC, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedRuntimeException(e);
        }

        // Ожидаемые из логброкера вердикты
        List<CardModerationVerdict> expectedVerdicts = StreamEx.of(JSON_VERDICTS)
                .map(json -> JsonUtils.fromJson(json, CardModerationVerdict.class))
                .toList();

        // Читаем вердикты из лог-брокера.
        LogbrokerConsumerProperties consumerProperties =
                LogbrokerConsumerProperties
                        .createInstance(directConfig, FreelancerCardReceivingJob.CONFIG_SECTION_NAME);
        List<CardModerationVerdict> gotVerdicts = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger(0);
        VerdictHandler verdictHandler = new VerdictHandler() {
            @Override
            public boolean applyVerdict(String json) {
                CardModerationVerdict cardModerationVerdict = JsonUtils.fromJson(json, CardModerationVerdict.class);
                gotVerdicts.add(cardModerationVerdict);
                counter.incrementAndGet();
                return true;
            }

            @Override
            public VerdictType getSupportedType() {
                return VerdictType.FREELANCER_CARD;
            }
        };
        Map<VerdictType, VerdictHandler> verdictHandlersByVerdictType =
                Collections.singletonMap(VerdictType.FREELANCER_CARD, verdictHandler);
        try (SyncConsumer syncConsumer = logbrokerClientFactoryService.createConsumer(consumerProperties)) {
            VerdictReceiver verdictReceiver = new VerdictReceiver(syncConsumer, verdictHandlersByVerdictType);
            verdictReceiver.readAndApply();
        }
        logger.info("Verdicts was read: {}", counter.get());
        // Сверяем ожидания и реальность
        assertThat(gotVerdicts)
                .is(matchedBy(beanDiffer(expectedVerdicts)
                        .useCompareStrategy(onlyExpectedFields())));
    }
}
