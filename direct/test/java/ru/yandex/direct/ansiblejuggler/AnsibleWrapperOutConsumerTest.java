package ru.yandex.direct.ansiblejuggler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ansiblejuggler.model.PlayRecap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNotNull;

public class AnsibleWrapperOutConsumerTest {

    AnsibleWrapper.OutConsumer consumer;

    @Before
    public void before() {
        consumer = new AnsibleWrapper.OutConsumer();
    }

    private Stream<String> getLines(String source) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(source.getBytes()))).lines();
    }

    @Test
    public void processAnsibleOutput_ValidInput_ReturnsPlayRecap() {
        String source = "TASK: [juggler_cleanup jcheck_mark=direct_dev] ********************************\n"
                + "ok: [localhost]\n"
                + "\n"
                + "PLAY RECAP ********************************************************************\n"
                + "localhost                  : ok=6    changed=0    unreachable=0    failed=0";

        getLines(source).forEach(consumer);

        assertNotNull(consumer.getRecap());
    }

    @Test
    public void processAnsibleOutput_ValidInputWithSeveralHosts_ReturnsPlayRecapForFirst() {
        String firstHostStat = "ipv6.ppcdev2.ppc.yandex.ru : ok=7    changed=6    unreachable=5    failed=4\n";
        String source = "PLAY RECAP ********************************************************************\n"
                + firstHostStat
                + "ipv6.ppcdev6.ppc.yandex.ru : ok=9    changed=1    unreachable=1    failed=0";
        PlayRecap expected = new PlayRecap(firstHostStat);

        getLines(source).forEach(consumer);

        PlayRecap recap = consumer.getRecap();
        assumeNotNull(recap);
        assertEquals(expected, recap);
    }

    @Test
    public void processAnsibleOutput_FailedInputWithRetryLineSeveralHosts_ReturnsPlayRecapForFirst() {
        String firstHostStat = "ipv6.ppcdev2.ppc.yandex.ru : ok=7    changed=6    unreachable=5    failed=4\n";
        String source = "PLAY RECAP ********************************************************************\n"
                + "           to retry, use: --limit @/Users/ppalex/haze-docker.retry\n"
                + "\n"
                + firstHostStat
                + "ipv6.ppcdev6.ppc.yandex.ru : ok=9    changed=1    unreachable=1    failed=0";
        PlayRecap expected = new PlayRecap(firstHostStat);

        getLines(source).forEach(consumer);

        PlayRecap recap = consumer.getRecap();
        assumeNotNull(recap);
        assertEquals(expected, recap);
    }

    @Test
    public void processAnsibleOutput_InputWithoutMarker_ReturnsNull() {
        String source = "localhost                  : ok=6    changed=0    unreachable=0    failed=0";

        getLines(source).forEach(consumer);

        assertNull(consumer.getRecap());
    }

    @Test
    public void processAnsibleOutput_WithEmptyInput_ReturnsNull() {
        getLines("").forEach(consumer);
        assertNull(consumer.getRecap());
    }

    @Test
    public void processAnsibleOutput_FailedInputWithError_ReturnsPlayRecapForFirst() {
        String localhostStat = "localhost                  : ok=82   changed=0    unreachable=0    failed=1   \n";
        String source = "PLAY RECAP ******************************************************************** \n"
                + "\n"
                + "ERROR: could not create retry file. Check the value of \n"
                + "the configuration variable 'retry_files_save_path' or set \n"
                + "'retry_files_enabled' to False to avoid this message.\n"
                + "\n"
                + localhostStat
                + "\n";
        PlayRecap expected = new PlayRecap(localhostStat);

        getLines(source).forEach(consumer);

        PlayRecap recap = consumer.getRecap();
        assumeNotNull(recap);
        assertEquals(expected, recap);
    }
}
