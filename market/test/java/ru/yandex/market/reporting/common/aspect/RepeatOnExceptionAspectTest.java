package ru.yandex.market.reporting.common.aspect;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
public class RepeatOnExceptionAspectTest {

    @Test(expected = NullPointerException.class)
    public void repeatOnException() throws Throwable {
        RepeatOnExceptionAspect.repeatOnException(() -> {
                throw new NullPointerException();
            },
            new Class[]{NullPointerException.class}, new Class[]{}, new String[]{},
            3, 1000, 5000, true);
    }

    @Test
    public void successAfterRepeat() throws Throwable {
        AtomicInteger count = new AtomicInteger(0);
        Integer result = (Integer) RepeatOnExceptionAspect.repeatOnException(() -> {
                if (count.incrementAndGet() < 2) {
                    throw new NullPointerException();
                }
                return 42;
            }, new Class[]{NullPointerException.class}, new Class[]{},
            new String[]{}, 3, 1000, 5000, true);

        assertThat(result, is(42));
        assertThat(count.get(), is(2));
    }

    @Test(expected = NullPointerException.class)
    public void noRepeatOnSomeException() throws Throwable {
        RepeatOnExceptionAspect.repeatOnException(() -> {
                throw new NullPointerException();
            },
            new Class[]{RuntimeException.class}, new Class[]{NullPointerException.class}, new String[]{},
            3, 1000, 5000, true);
    }

    @Test
    public void failAfterRepeat() throws Throwable {
        AtomicInteger count = new AtomicInteger(0);
        try {
            RepeatOnExceptionAspect.repeatOnException(() -> {
                    switch (count.incrementAndGet()) {
                        case 1:
                            throw new NullPointerException();
                        case 2:
                            throw new IllegalStateException();
                        default:
                            return 43;
                    }
                }, new Class[]{NullPointerException.class}, new Class[]{IllegalStateException.class}, new String[]{},
                3, 1000, 5000, true);
            fail(IllegalStateException.class + " should be thrown");
        } catch (IllegalStateException e) {
            // ignore
        }
        assertThat(count.get(), is(2));
    }

}
