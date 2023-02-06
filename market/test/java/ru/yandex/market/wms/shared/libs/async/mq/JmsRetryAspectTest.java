package ru.yandex.market.wms.shared.libs.async.mq;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;

import ru.yandex.market.wms.shared.libs.async.jms.JmsRetryAspect;
import ru.yandex.market.wms.shared.libs.async.jms.Retry;
import ru.yandex.market.wms.shared.libs.async.retry.DeadQueueMessageTtl;
import ru.yandex.market.wms.shared.libs.async.retry.JmsRetry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JmsRetryAspectTest {
    private <T> T initListenerProxy(T listener, JmsRetry retry) {
        AspectJProxyFactory factory = new AspectJProxyFactory(listener);
        factory.addAspect(new JmsRetryAspect(retry));
        return factory.getProxy();
    }

    /**
     * Проверяем, что в случае ошибки сообщение JMS будет обработано объектом JmsRetry для помещения в очередь
     * повторных попыток обработки без TTL
     */
    @Test
    void validListenerFailure() {
        final String dest = "test_destination";
        final Object payload = new Object();
        final MessageHeaders headers = new MessageHeaders(Collections.emptyMap());

        class Listener {
            @JmsListener(destination = dest)
            @Retry
            public void listen(@Payload Object payload, MessageHeaders headers) {
                throw new IllegalStateException("always fail");
            }
        }

        JmsRetry jmsRetry = mock(JmsRetry.class);

        Listener listenerProxy = initListenerProxy(new Listener(), jmsRetry);

        assertDoesNotThrow(() -> listenerProxy.listen(payload, headers));
        verify(jmsRetry).retry("test_destination", payload, headers, DeadQueueMessageTtl.NONE);
    }

    /**
     * Проверяем, что в случае ошибки сообщение JMS будет обработано объектом JmsRetry для помещения в очередь
     * повторных попыток обработки с TTL
     */
    @Test
    void validListenerWithDeadQueueMessageTtlFailure() {
        final String dest = "test_destination";
        final Object payload = new Object();
        final MessageHeaders headers = new MessageHeaders(Collections.emptyMap());

        class Listener {
            @JmsListener(destination = dest)
            @Retry(deadQueueMessageTtl = DeadQueueMessageTtl.TWO_HOURS)
            public void listen(@Payload Object payload, MessageHeaders headers) {
                throw new IllegalStateException("always fail");
            }
        }

        JmsRetry jmsRetry = mock(JmsRetry.class);

        Listener listenerProxy = initListenerProxy(new Listener(), jmsRetry);

        assertDoesNotThrow(() -> listenerProxy.listen(payload, headers));
        verify(jmsRetry).retry("test_destination", payload, headers, DeadQueueMessageTtl.TWO_HOURS);
    }

    /**
     * Проверяем, что параметры в метод @JmsListener пробрасываются аспектом без изменений и при при отсутствии ошибок
     * метод объекта JmsRetry не вызывается.
     */
    @Test
    void noFailure() {
        final String dest = "test_destination";
        final Object payload = new Object();
        final MessageHeaders headers = new MessageHeaders(Collections.emptyMap());

        class Listener {
            @JmsListener(destination = dest)
            @Retry
            public void listen(@Payload Object localPayload, MessageHeaders localHeaders) {
                assertSame(payload, localPayload);
                assertSame(headers, localHeaders);
            }
        }

        JmsRetry jmsRetry = mock(JmsRetry.class);

        Listener listenerProxy = initListenerProxy(new Listener(), jmsRetry);

        assertDoesNotThrow(() -> listenerProxy.listen(payload, headers));
        verifyNoInteractions(jmsRetry);
    }

    /**
     * Проверяем, что при отсутствии аннотации @JmsListener аспект бросает исключение.
     */
    @Test
    void missingListenerAnnotation() {
        class Listener {
            @Retry
            public void listen(@Payload Object payload, MessageHeaders headers) {
                throw new IllegalStateException("always fail");
            }
        }

        Listener listenerProxy = initListenerProxy(new Listener(), new DummyJmsRetry());
        Exception thrown = assertThrows(RuntimeException.class, () -> listenerProxy.listen(null, null));
        assertEquals("Method must be annotated with @JmsListener", thrown.getMessage());
    }

    /**
     * Проверяем, что при отсутствии параметра с аннотацией @Payload аспект бросает исключение.
     */
    @Test
    void missingPayloadAnnotation() {
        class Listener {
            @JmsListener(destination = "test_destination")
            @Retry
            public void listen(Object payload, MessageHeaders headers) {
                throw new IllegalStateException("always fail");
            }
        }

        Listener listenerProxy = initListenerProxy(new Listener(), new DummyJmsRetry());
        Exception thrown = assertThrows(RuntimeException.class, () -> listenerProxy.listen(null, null));
        assertEquals("Parameter annotated with @Payload is not present", thrown.getMessage());
    }

    /**
     * Проверяем, что при отсутствии параметра класса MessageHeaders аспект бросает исключение.
     */
    @Test
    void missingHeadersParameter() {
        class Listener {
            @JmsListener(destination = "test_destination")
            @Retry
            public void listen(@Payload Object payload, Object headers) {
                throw new IllegalStateException("always fail");
            }
        }

        Listener listenerProxy = initListenerProxy(new Listener(), new DummyJmsRetry());
        Exception thrown = assertThrows(RuntimeException.class, () -> listenerProxy.listen(null, null));
        assertEquals("MessageHeaders parameter is not present", thrown.getMessage());
    }

    private static class DummyJmsRetry implements JmsRetry {
        @Override
        public void retry(String queue, Object request, MessageHeaders headers) {
            // no-op
        }

        @Override
        public void retry(
            String queue,
            Object request,
            MessageHeaders headers,
            DeadQueueMessageTtl deadQueueMessageTtl
        ) {
            // no-op
        }
    }
}
