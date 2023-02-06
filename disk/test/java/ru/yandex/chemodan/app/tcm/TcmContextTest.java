package ru.yandex.chemodan.app.tcm;

import ru.yandex.chemodan.boot.ChemodanMainSupport;
import ru.yandex.chemodan.util.test.ContextTestSupport;

public class TcmContextTest extends ContextTestSupport {

    @Override
    public ChemodanMainSupport createMain() {
        return new TcmMain();
    }
}
