package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.LegalPartnerPupEvent;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PupEvent;
import ru.yandex.market.crm.lb.LogBrokerMessageConsumer;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;
import ru.yandex.market.jmf.utils.serialize.SerializationException;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadType;
import ru.yandex.market.pvz.client.crm.dto.LegalPartnerCrmDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@B2bPickupPointTests
@ExtendWith(SpringExtension.class)
class PupEventConsumerFailsafeTest {

    @Inject
    private LegalPartnerPupEventProcessor processor;

    @Inject
    private UnprocessedEventsDao unprocessedEventsDao;

    @Inject
    private ObjectSerializeService serializeService;

    @Inject
    private TxService txService;

    private LogBrokerMessageConsumer<PupEvent<?>> consumer;

    private static <T> T spyDynamicProxy(Class<T> clazz, T object) {
        return Mockito.mock(clazz, Mockito.withSettings().defaultAnswer(
                AdditionalAnswers.delegatesTo(object)
        ));
    }

    @BeforeEach
    void setUp() {
        unprocessedEventsDao = spyDynamicProxy(UnprocessedEventsDao.class, unprocessedEventsDao);
        processor = spyDynamicProxy(LegalPartnerPupEventProcessor.class, processor);
        consumer = new PupEventConsumer(
                serializeService,
                Set.of(processor),
                mock(LogIdentifier.class),
                unprocessedEventsDao,
                txService
        );
    }

    @Test
    void shouldIgnoreAndSaveErrorsWhileParsingEvent() {
        String message = "{\"type\": \"unknown\"}";
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        consumer.transform(messageBytes);
        verify(unprocessedEventsDao, times(1)).save(eq(messageBytes), any(SerializationException.class));
        verify(processor, times(0)).process(any());
        assertThat(unprocessedEventsDao.count()).isEqualTo(1L);
    }

    @Test
    void shouldIgnoreAndSaveErrorsWhileProcessingEvent() {
        InvalidEvent firstEvent = new InvalidEvent();
        InvalidEvent secondEvent = new InvalidEvent();
        consumer.accept(List.of(firstEvent, secondEvent));

        verify(unprocessedEventsDao, Mockito.atLeastOnce())
                .save(eq(firstEvent), any(IllegalStateException.class));
        verify(processor, times(1)).process(firstEvent);

        //Первое невалидное сообщение не остановило вычитку
        verify(unprocessedEventsDao, Mockito.atLeastOnce())
                .save(eq(secondEvent), any(IllegalStateException.class));
        verify(processor, times(1)).process(firstEvent);

        //Оба невалидных событий сохранились в бд
        assertThat(unprocessedEventsDao.count()).isEqualTo(2L);
    }

    private static class InvalidEvent extends LegalPartnerPupEvent {

        private final String id;

        public InvalidEvent() {
            super(CrmPayloadType.LEGAL_PARTNER, Instant.now(), null);
            id = UUID.randomUUID().toString();
        }

        @Override
        public LegalPartnerCrmDto getValue() {
            throw new IllegalStateException();
        }

        @JsonProperty("value")
        public LegalPartnerCrmDto getSerialValue() {
            return super.getValue();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InvalidEvent that = (InvalidEvent) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
