package ru.yandex.market.mstat.planner.security;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.inside.passport.blackbox.Blackbox;
import ru.yandex.inside.passport.blackbox.protocol.BlackboxResponse;
import ru.yandex.misc.ip.IpAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BBTest {


    //curl "http://blackbox-test.yandex-team.ru/blackbox?method=sessionid&sessionid=aaaaaaa&userip=127.0.0.1&host=antifraud-pgaas.tst.vs.market.yandex.net"
    // sudo ifconfig awdl0 down

    @Test
    @Ignore
    public void testLogin() throws Exception {
        Blackbox blackBoxClient = new Blackbox("http://blackbox-test.yandex.net/blackbox");

        BlackboxResponse login = blackBoxClient.sessionId(IpAddress.parse("127.0.0.1"),
            "3:1549469667.5.0.1549459749426:FpsHTbZyfHUJIwAAuAYCKg:9.1|4020089680.0.2|308168.412058.PxRAP_lPaFWxnUYXXWyPthm5zEI",
            "mstat-planer-local.yandex.ru", null, false
        );
        assertThat(login.getLogin().get(), is("mstattest"));

    }
}
