package ru.yandex.direct.ansiblejuggler;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.ansiblejuggler.model.PlayRecap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ParametersAreNonnullByDefault
public class PlayRecapPositiveTest {
    private static PlayRecap recap;

    @BeforeClass
    public static void beforeClass() {
        recap = new PlayRecap("ipv6.ppcdev2.ppc.yandex.ru : ok=9    changed=1    unreachable=2    failed=5");
    }

    @Test
    public void okValue() {
        assertThat(recap.getOk(), is(9));
    }

    @Test
    public void changedValue() {
        assertThat(recap.getChanged(), is(1));
    }

    @Test
    public void unreachableValue() {
        assertThat(recap.getUnreachable(), is(2));
    }

    @Test
    public void failedValue() {
        assertThat(recap.getFailed(), is(5));
    }
}
