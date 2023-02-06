package ru.yandex.market.crm.lb.test;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LogIdentifierTest {

    @Test
    public void testOldFormat() {
        LogIdentifier log = new LogIdentifier("test-ident", "testlog", LBInstallation.defaultInstallation());
        assertThat(log.getIdent(), is("test-ident"));
        assertThat(log.getLogType(), is("testlog"));
        assertThat(log.toString(), is("test-ident--testlog"));
    }

    @Test
    public void testNewFormatShort() {
        LogIdentifier log = new LogIdentifier("test-ident/testlog", LBInstallation.defaultInstallation());
        assertThat(log.getIdent(), is("test-ident"));
        assertThat(log.getLogType(), is("testlog"));
        assertThat(log.toString(), is("test-ident/testlog"));
    }

    @Test
    public void testNewFormatFull() {
        LogIdentifier log = new LogIdentifier("test-account/testing/testlog", LBInstallation.defaultInstallation());
        assertThat(log.getIdent(), is("test-account/testing"));
        assertThat(log.getLogType(), is("testlog"));
        assertThat(log.toString(), is("test-account/testing/testlog"));
    }

    @Test
    public void testNewTopicToOldFormatFull() {
        LogIdentifier log = new LogIdentifier("test-account@testing--testlog", LBInstallation.defaultInstallation());
        assertThat(log.getIdent(), is("test-account@testing"));
        assertThat(log.getLogType(), is("testlog"));
        assertThat(log.toString(), is("test-account@testing--testlog"));
    }

    @Test
    public void testInstallation() {
        Arrays.asList(LBInstallation.values()).forEach(installation -> {
            LogIdentifier log = new LogIdentifier("test-ident", "testlog", installation);
            assertThat(log.getInstallation(), is(installation));
        });
    }
}
