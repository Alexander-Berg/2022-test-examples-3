package ru.yandex.market.notification.common;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.notification.model.data.NotificationContent;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit тест для {@link Caster#cast(Class)}.
 */
@RunWith(Parameterized.class)
public class CasterCastTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Class<?> testClass;
    private final boolean correct;


    public CasterCastTest(final Class<?> testClass, final boolean correct) {
        this.testClass = testClass;
        this.correct = correct;
    }


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { TestCaster.class, true },
            { Caster.class, true },
            { Object.class, true },
            { NotificationContent.class, false },
            { String.class, false },
            { null, false },
        });
    }


    @Test
    public void test() {
        final Caster caster = new TestCaster();

        if (correct) {
            assertThat(cast(caster), equalTo(caster));
        } else {
            exception.expect(ClassCastException.class);

            final Object result = cast(caster);
            Assert.fail("Caster should throw an exception, but returns " + result);
        }
    }


    @SuppressWarnings("unchecked")
    private Object cast(final Caster caster) {
        return caster.cast(testClass);
    }


    /**
     * Фальшивая пусткая реализация {@link Caster}, необходимая для теста.
     */
    class TestCaster implements Caster<TestCaster> {
    }

}
