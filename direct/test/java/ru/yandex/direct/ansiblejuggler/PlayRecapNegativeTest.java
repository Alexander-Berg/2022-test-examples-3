package ru.yandex.direct.ansiblejuggler;

import org.junit.Test;

import ru.yandex.direct.ansiblejuggler.model.PlayRecap;

public class PlayRecapNegativeTest {
    @Test(expected = PlayRecap.ParseException.class)
    public void noHostnameParseException() {
        new PlayRecap("              : ok=9    changed=1    unreachable=2    failed=5");
    }

    @Test(expected = PlayRecap.ParseException.class)
    public void noFailedParseException() {
        new PlayRecap("localhost : ok=9    changed=1    unreachable=2");
    }

    @Test(expected = PlayRecap.ParseException.class)
    public void badDelimiterParseException() {
        new PlayRecap("localhost : : ok 9    changed 1    unreachable 2    failed 5");
    }
}
