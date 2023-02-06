package ru.yandex.market.core.trace;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.TraceWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Vadim Lyalin
 */
class TraceWrapperTest {
    TraceWrapper traceWrapper = new TraceWrapper(Module.MBI_PARTNER, Module.ORACLE);

    @Test
    void logRunnable() {
        // given
        var sideEffect = new AtomicInteger();

        // when
        traceWrapper.log(sideEffect::incrementAndGet, "");

        // then
        assertThat(sideEffect.get()).isEqualTo(1);
    }

    @Test
    void logSupplier() {
        // when
        var result = traceWrapper.log(() -> "str", "");

        // then
        assertThat(result).isEqualTo("str");
    }

    @Test
    void logException() {
        // when-then
        assertThrows(
                IllegalArgumentException.class,
                () -> traceWrapper.log(() -> {
                    throw new IllegalArgumentException();
                }, "")
        );
    }
}
