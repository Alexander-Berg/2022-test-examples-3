package ru.yandex.common.framework.http;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;

/**
 * Author: Olga Bolshakova (obolshakova@yandex-team.ru)
 * Date: 07.09.2009
 */
public class ThrowExceptionServantlet implements Servantlet {
    @Override
    public void process(final ServRequest request, final ServResponse response) {
        throw new RuntimeException("ThrowExceptionServantlet TEST ERROR");
    }
}
