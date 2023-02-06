package ru.yandex.direct.ansiblejuggler.model.checks;

import java.time.Duration;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.ansiblejuggler.Util.dumpAsString;
import static ru.yandex.direct.ansiblejuggler.Util.getLocalResource;

public class SingleEventFromAnyHostCheckSerializationTest {
    private static final String namespace = "testnamespace";

    @Test
    public void sameSourceAndTargetServiceNames_SingleSourceHost_Test() {
        JugglerCheck check =
                new SingleEventFromAnyHostCheck("targetHost", "junitTestService", "CGROUP%sourceHostsGroup",
                        Duration.ofHours(2), namespace, "junitTestMark");

        String expected = getLocalResource("checks/SingleEventFromAnyHostCheckSameServiceSingleSource.yaml");
        String actual = dumpAsString(check);
        assertEquals(expected, actual);
    }

    @Test
    public void sameSourceAndTargetServiceNames_SeveralSourceHosts_Test() {
        JugglerCheck check = new SingleEventFromAnyHostCheck("targetHost", "junitTestService",
                Arrays.asList("CGROUP%sourceHostsGroup", "simple.host.name", "CGROUP%secondGroup"),
                Duration.ofHours(2), namespace, "junitTestMark");

        String expected = getLocalResource("checks/SingleEventFromAnyHostCheckSameServiceSeveralSources.yaml");
        String actual = dumpAsString(check);
        assertEquals(expected, actual);
    }

    @Test
    public void differentSourceAndTargetServiceNames_SeveralSourceHosts_Test() {
        JugglerCheck check = new SingleEventFromAnyHostCheck("targetHost", "targetService",
                Arrays.asList("CGROUP%sourceHostsGroup", "simple.host.name", "CGROUP%secondGroup"), "sourceService",
                Duration.ofHours(1), namespace, "junitTestMark");

        String expected = getLocalResource("checks/SingleEventFromAnyHostCheckDifferentServiceSeveralSources.yaml");
        String actual = dumpAsString(check);
        assertEquals(expected, actual);
    }
}
