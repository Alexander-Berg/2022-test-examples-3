package ru.yandex.market.utils.executor;

import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Nonnull;

import com.google.common.base.Suppliers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.utils.executor.CapacityAwareTask;
import ru.yandex.market.utils.executor.RatioCapacityKeyCounter;
import ru.yandex.market.utils.executor.SupplierWithName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CapacityAwareTaskTest {
    RatioCapacityKeyCounter counter = new RatioCapacityKeyCounter(
            2,
            Suppliers.ofInstance(0),
            Suppliers.ofInstance(50)
    );

    CapacityAwareTask<String> makeSupplier(String key, boolean ok) {
        return new CapacityAwareTask<>(new SupplierWithName<>() {
            @Override
            public String get() {
                assertThat(counter.getCurrent(key)).as("added by CapacityAwareTask").isOne();
                if (ok) {
                    return Thread.currentThread().getName();
                }
                throw new RuntimeException(key);
            }

            @Nonnull
            @Override
            public String getName() {
                return key;
            }
        }, null, counter);
    }

    @Test
    void runSucceeded() {
        // given
        var key = "whatever";
        var cs = makeSupplier(key, true);

        // when
        var result = cs.get();

        // then
        assertThat(result).endsWith(key);
        assertThat(Thread.currentThread().getName()).doesNotEndWith(key);
        assertThat(counter.getCurrent(key)).isZero();
    }

    @Test
    void runFailed() {
        // given
        var key = "whatever";
        var cs = makeSupplier(key, false);

        // when
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(cs::get).withMessage(key);

        // then
        assertThat(Thread.currentThread().getName()).doesNotEndWith(key);
        assertThat(counter.getCurrent(key)).isZero();
    }

    @Test
    void runFailedOnCapacity() {
        // given
        var key = "whatever";
        var cs = makeSupplier(key, true);
        assertThat(counter.tryAdd(key)).isTrue(); // grab all task capacity
        assertThat(counter.getCurrent(key)).isOne();

        // when
        assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(cs::get);

        // then
        assertThat(Thread.currentThread().getName()).doesNotEndWith(key);
        assertThat(counter.getCurrent(key)).isOne();
    }
}
