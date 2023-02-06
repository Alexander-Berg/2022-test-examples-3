package ru.yandex.market.tsum.ui.service.auth;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.tsum.clients.blackbox.BlackBoxClient;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 18/08/16
 */
@Ignore
public class BlackBoxClientTest {


    /*
     ssh -f -N -L 8080:blackbox.yandex-team.ru:80 graph01gt.market.yandex.net
     */
    @Test
    public void testLogin() throws Exception {
        BlackBoxClient blackBoxClient = new BlackBoxClient(BlackBoxClient.BlackBoxType.TEST_YA_TEAM);

        String login = blackBoxClient.getLogin(
            "SESSION_ID HERE",
            "tsum.yandex-team.ru",
            "95.108.173.188"
        );

        System.gc();
    }
}
