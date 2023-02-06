package ru.yandex.direct.ansiblejuggler;

import java.time.Duration;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import ru.yandex.direct.ansiblejuggler.model.JugglerPlay;
import ru.yandex.direct.ansiblejuggler.model.aggregators.AllChildrenShouldBeOk;
import ru.yandex.direct.ansiblejuggler.model.checks.JugglerAlertMethod;
import ru.yandex.direct.ansiblejuggler.model.checks.JugglerCheck;
import ru.yandex.direct.ansiblejuggler.model.checks.JugglerChild;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.ansiblejuggler.Util.getLocalResource;

public class PlaybookSerializationSmokeTest {
    private static final String jserverApi = "http://fakejuggler.yandex.ru:8998/api";
    private static final String jcheckMark = "junitTestMark";
    private static final String namespace = "testnamespace";

    @Test
    @SuppressWarnings("deprecation")
    public void withAlertMethod_WithUnreachable_WithCleanup() throws JsonProcessingException {
        JugglerCheck check = new JugglerCheck("fake_checks.yandex.ru", "queue_juggler_event_from_perl",
                Duration.ofMinutes(15),
                new AllChildrenShouldBeOk().withSkipUnreachable(new JugglerChild("direct.developers", "UNREACHABLE")),
                namespace,
                jcheckMark);
        IntStream.rangeClosed(1, 4)
                .mapToObj(i -> "fakeserv" + i + ".yandex.ru")
                .map(h -> new JugglerChild(h, "queue_raw_event_from_perl"))
                .forEach(check::withChild);
        check.withAlertMethod(JugglerAlertMethod.GOLEM);

        String expected = getLocalResource("playbooks/WithAlertMethodWithUnreachableWithCleanup.yaml");
        String actual =
                new JugglerPlay(jserverApi).withCleanup(jcheckMark).withCheck("dummy task", check).asPlaybook().dump();

        assertEquals(expected, actual);
    }

    @Test
    public void WithTags() throws JsonProcessingException {
        JugglerCheck check = new JugglerCheck("fake_checks.yandex.ru", "queue_juggler_event_from_perl",
                Duration.ofMinutes(15),
                new AllChildrenShouldBeOk(),
                namespace,
                jcheckMark);
        IntStream.rangeClosed(1, 4)
                .mapToObj(i -> "fakeserv" + i + ".yandex.ru")
                .map(h -> new JugglerChild(h, "queue_raw_event_from_perl"))
                .forEach(check::withChild);
        check.withTag("test_tag").withTag("direct_dev");


        String expected = getLocalResource("playbooks/WithTags.yaml");
        String actual = new JugglerPlay(jserverApi).withCheck("dummy task", check).asPlaybook().dump();

        assertEquals(expected, actual);
    }
}
