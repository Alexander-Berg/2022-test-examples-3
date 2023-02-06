package ru.yandex.market.juggler;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 03/08/16
 */
@Ignore
public class JugglerClientTest {

    private JugglerClient jugglerClient;

    /*
    ssh -f -N -L 8999:jmon-test.search.yandex.net:8999 braavos
     */
    @Before
    public void setUp() throws Exception {
        jugglerClient = new JugglerClient();
        jugglerClient.setJugglerUrl("http://localhost:8999/");
        jugglerClient.afterPropertiesSet();
    }

    @Test
    public void sendEvents() throws Exception {
        jugglerClient.sendEvents(Collections.singletonList(
            new JugglerEvent("clickphite-tst", "tst", JugglerEvent.Status.CRIT, "desc")
        ));
    }

}