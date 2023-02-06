package ru.yandex.market.stat.dicts.bazinga;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.stat.dicts.config.BazingaConfig;

public class BazingaConfigTest {


    @Test
    public void canGuessPortByRTCHost() {
        String port = BazingaConfig.getValidPortForRtcHost(
                "http://sas5-3600-924-sas-market-test--b26-19082.gencfg-c.yandex.net", 777
        );
        Assert.assertThat("Wrong port for rtc host!", port, Matchers.is(":19082"));
    }

    @Test
    public void canGuessPortByRTCHostWithAlotOfNumbers() {
        String port = BazingaConfig.getValidPortForRtcHost(
                "http://sas5-3600-924-sas-market-test--b26-19082-333d-8888-f9999.gencfg-c.yandex.net", 777);
        Assert.assertThat("Wrong port for rtc host!", port, Matchers.is(":8888"));
    }


    @Test
    public void noGuessingForNonRTC() {
        String port = BazingaConfig.getValidPortForRtcHost("data-capture.vs.market.yandex.net", 777);
        Assert.assertThat("Wrong port for non-rtc host!", port, Matchers.is(":777"));
    }

    @Test
    public void stillNoGuessingForNonRTC() {
        String port = BazingaConfig.getValidPortForRtcHost("data-capture.vs-9999-mm-666666-n.market.yandex.net", 777);
        Assert.assertThat("Wrong port for non-rtc host!", port, Matchers.is(":777"));
    }

    @Test
    public void defaultNoPortForYPhost() {
        String port = BazingaConfig.getValidPortForRtcHost("http://myapwefk2q45rky2.vla.yp-c.yandex.net", 777);
        Assert.assertThat("Wrong port for non-rtc host!", port, Matchers.is(""));
    }
}
