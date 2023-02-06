package ru.yandex.common.framework.http;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;

/**
 * Date: Jun 7, 2009
 * Time: 3:15:09 AM
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class FakeRedirServantlet implements Servantlet {

    @Override
    public void process(final ServRequest request, final ServResponse response) {
        response.sendRedirect("/test.xml");
    }

}
