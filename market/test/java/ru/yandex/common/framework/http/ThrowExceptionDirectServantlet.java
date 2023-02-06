package ru.yandex.common.framework.http;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;

/**
 * Created by IntelliJ IDEA.
 * User: ivankovv
 * Date: 27.10.2009
 * Time: 16:46:35
 */
public class ThrowExceptionDirectServantlet implements Servantlet {
    @Override
    public void process(ServRequest request, ServResponse response) {
        response.write("throwExceptionDirect".getBytes());
        response.addData("will throw illegal state exception");
    }
}
