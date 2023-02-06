package ru.yandex.common.framework.http;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;

/**
 * @author ivankovv
 */
public class FakeDirectServantlet implements Servantlet {

    @Override
    public void process(ServRequest request, ServResponse response) {
        response.write("fakeDirect".getBytes());
    }

}
