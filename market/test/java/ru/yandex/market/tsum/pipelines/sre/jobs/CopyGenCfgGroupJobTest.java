package ru.yandex.market.tsum.pipelines.sre.jobs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipelines.sre.resources.ConfigCopyGenCfgGroup;


@RunWith(MockitoJUnitRunner.class)
public class CopyGenCfgGroupJobTest {
    @Spy
    private ConfigCopyGenCfgGroup configCopyGenCfgGroup = new ConfigCopyGenCfgGroup();

    @InjectMocks
    private CopyGenCfgGroupJob copyGenCfgGroupJob;

    @Before
    public void setup() {
        configCopyGenCfgGroup.setSrcGroupName("SAS_MARKET_TEST_SERVICE_1");
        configCopyGenCfgGroup.setDstGroupName("MAN_MARKET_TEST_SERVICE_1");
    }

    @Test
    public void prepareProgramArgs() throws Exception {
        String args = copyGenCfgGroupJob.prepareProgramArgs();
        String expected = "-s SAS_MARKET_TEST_SERVICE_1 -d MAN_MARKET_TEST_SERVICE_1";
        Assert.assertEquals(expected, args);
    }
}
