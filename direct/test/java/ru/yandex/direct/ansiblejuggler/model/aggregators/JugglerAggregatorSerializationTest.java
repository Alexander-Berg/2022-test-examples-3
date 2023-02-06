package ru.yandex.direct.ansiblejuggler.model.aggregators;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ansiblejuggler.model.checks.JugglerChild;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.ansiblejuggler.Util.dumpAsString;
import static ru.yandex.direct.ansiblejuggler.Util.getLocalResource;
import static ru.yandex.direct.ansiblejuggler.model.aggregators.ParamsMode.FORCE_OK;
import static ru.yandex.direct.ansiblejuggler.model.aggregators.ParamsMode.SKIP;

public class JugglerAggregatorSerializationTest {
    private static final JugglerChild unreachService = new JugglerChild("unreach_host", "UNREACHABLE");
    private JugglerAggregator base;

    @Before
    public void before() {
        base = new AnyChildShouldBeOk();
    }

    @Test
    public void withSkipDowntimes_Test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOkWithSkipDowntimes.yaml");
        String actual = dumpAsString(base.withSkipDowntimes());
        assertEquals(expected, actual);
    }

    @Test
    public void withDowntimesMode_Skip_Test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOkWithSkipDowntimes.yaml");
        String actual = dumpAsString(base.withDowntimesMode(SKIP));
        assertEquals(expected, actual);
    }

    @Test
    public void withDowntimesMode_ForceOk_Test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOkWithForceOkDowntimes.yaml");
        String actual = dumpAsString(base.withDowntimesMode(FORCE_OK));
        assertEquals(expected, actual);
    }

    @Test
    public void withSkipUnreachable_Test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOkWithSkipUnreachable.yaml");
        String actual = dumpAsString(base.withSkipUnreachable(unreachService));
        assertEquals(expected, actual);
    }

    @Test
    public void withUnreachableParams_Skip_Test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOkWithSkipUnreachable.yaml");
        String actual = dumpAsString(base.withUnreachableParams(new UnreachableParams(SKIP, unreachService)));
        assertEquals(expected, actual);
    }

    @Test
    public void withUnreachableParams_ForceOk_Test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOkWithForceOkUnreachable.yaml");
        String actual = dumpAsString(base.withUnreachableParams(new UnreachableParams(FORCE_OK, unreachService)));
        assertEquals(expected, actual);
    }

    @Test
    public void withSkipDowntimes_WithSkipUnreachable_test() {
        String expected = getLocalResource("aggregators/AnyChildShouldBeOkWithSkipDowntimesSkipUnreachable.yaml");
        String actual = dumpAsString(base.withSkipDowntimes().withSkipUnreachable(unreachService));
        assertEquals(expected, actual);
    }
}
