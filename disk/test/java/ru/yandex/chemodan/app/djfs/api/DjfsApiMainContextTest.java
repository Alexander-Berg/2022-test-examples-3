package ru.yandex.chemodan.app.djfs.api;

import ru.yandex.chemodan.boot.ChemodanMainSupport;
import ru.yandex.chemodan.util.test.ContextTestSupport;

/**
 * @author tolmalev
 */
public class DjfsApiMainContextTest extends ContextTestSupport {
    @Override
    public ChemodanMainSupport createMain() {
        return new DjfsApiMain();
    }
}
