package ru.yandex.chemodan.uploader;

import ru.yandex.chemodan.boot.ChemodanMainSupport;
import ru.yandex.chemodan.util.test.ContextTestSupport;

/**
 * @author akirakozov
 */
public class UploaderContextTest extends ContextTestSupport {

    @Override
    public ChemodanMainSupport createMain() {
        return new UploaderMain();
    }

}
