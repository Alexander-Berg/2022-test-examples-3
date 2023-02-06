package ru.yandex.calendar.frontend.api.todo;

import lombok.SneakyThrows;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.calendar.frontend.api.ApiContextA3Configuration;
import ru.yandex.calendar.frontend.api.ApiContextConfiguration;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.todo.TodoContextConfiguration;
import ru.yandex.calendar.logic.todo.TodoRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;
import ru.yandex.commune.a3.ActionApp;
import ru.yandex.commune.a3.action.CloneableAction;
import ru.yandex.commune.a3.action.http.ActionInvocationServlet;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.parse.BenderParser;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.net.uri.Uri2;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

/**
 * @author dbrylev
 */
@ContextConfiguration(classes = {
        TestBaseContextConfiguration.class,
        TodoContextConfiguration.class,
        ApiContextConfiguration.class,
        ApiContextA3Configuration.class,
})
public abstract class ApiTodoActionTestBase<T> extends AbstractConfTest {

    @Autowired
    protected TestManager testManager;
    @Autowired
    protected TodoRoutines todoRoutines;
    @Autowired
    private ActionApp actionApp;

    protected TestUserInfo user;
    protected PassportUid uid;
    protected long listId;

    private ActionInvocationServlet servlet;

    private final String actionName;
    private final BenderParser<T> resultParser;

    public ApiTodoActionTestBase(Class<? extends CloneableAction> actionClass, Class<T> resultClass) {
        this.actionName = StringUtils.uncapitalize(actionClass.getSimpleName().replaceFirst("((A3)?Action)$", ""));
        this.resultParser = BenderParser.cons(ClassX.wrap(resultClass), BenderConfiguration.defaultConfiguration());
    }

    @Before
    public void setup() {
        user = testManager.prepareRandomYaTeamUser(12383);
        uid = user.getUid();
        listId = todoRoutines.findFirstCreatedListOrCreateNewWithName(uid, "ToDo", ActionInfo.webTest());

        servlet = actionApp.createServlet();
    }

    @SneakyThrows
    protected T execute(String urlParams) {
        Uri2 uri = Uri2.parse("/" + actionName + "?uid=" + uid + "&" + urlParams);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri.onlyPathQueryFragment().toString());

        request.setPathInfo(uri.getPath());
        request.setParameters(uri.getQueryArgs().toMap());

        request.addHeader("Accept", "application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.service(request, response);

        return resultParser.parseJson(response.getContentAsString());
    }
}
