package ru.yandex.market.checkout.pushapi.ping;

import org.apache.log4j.Logger;
import ru.yandex.market.common.ping.PingChecker;
import ru.yandex.market.common.ping.PingHelper;

import java.util.List;

public class PushApiPingChecker {

    private static final Logger log = Logger.getLogger(PushApiPingChecker.class);

    private PingHelper pingHelper = new PingHelper();

    public PushApiPingChecker() {
        pingHelper.setOkAnswer("0;OK\n");
    }

    public void setCheckers(List<PingChecker> checkers) {
        pingHelper.setCheckers(checkers);
    }

    public String check() {
        return pingHelper.makeChecks();
    }

}
