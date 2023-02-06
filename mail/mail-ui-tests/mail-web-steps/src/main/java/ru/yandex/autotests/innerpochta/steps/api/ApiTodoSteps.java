package ru.yandex.autotests.innerpochta.steps.api;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.todo.Todo;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.innerpochta.api.todo.DoTodoCreate.doDoTodoCreate;
import static ru.yandex.autotests.innerpochta.api.todo.DoTodoSettings.doTodoSettings;
import static ru.yandex.autotests.innerpochta.api.todo.DoTodolistCreateHandler.doTodolistCreateHandler;
import static ru.yandex.autotests.innerpochta.api.todo.DoTodolistDeleteHandler.doTodolistDeleteHandler;
import static ru.yandex.autotests.innerpochta.api.todo.TodoListsHandler.todoListsHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;

/**
 * Created by mabelpines on 11.02.16.
 */
public class ApiTodoSteps {

    public RestAssuredAuthRule auth;

    public ApiTodoSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: todo-lists. Получаем список тудушек.")
    public List<Todo> getAllTodoLists() {
        return Arrays.asList(todoListsHandler().withAuth(auth).callTodoListsHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data", Todo[].class));
    }

    @Step("Удаляем все тудушки пользователя.")
    public ApiTodoSteps deleteAllTodoLists() {
        for (Todo todoList : getAllTodoLists()) {
            deleteTodoList(todoList.getTitle(), todoList);
        }
        return this;
    }

    @Step("Вызов api-метода: do-todolist-delete. Удаляем список: “{0}“")
    public Response deleteTodoList(String title, Todo todoList) {
        return doTodolistDeleteHandler().withAuth(auth).withExtId(todoList.getExternalId())
            .callDoTodolistDeleteHandler();
    }

    @Step("Вызов api-метода do-todolist-create. Создаем список: {0}")
    public Todo createTodoList(String title) {
        doTodolistCreateHandler().withAuth(auth).withTitle(title).callDoTodolistCreateHandler();
        return selectFirst(getAllTodoLists(), having(on(Todo.class).getTitle(), equalTo(title)));
    }

    @Step("Получаем объект списка с названием: {0}")
    public Todo getTodoListByTitle(String title) {
        return selectFirst(getAllTodoLists(), having(on(Todo.class).getTitle(), equalTo(title)));
    }

    @Step("Вызов api-метода do-todo-create. Создаем дело: {0} в списке {1}")
    public Todo createTodo(String title, String todoListTitle, Todo todolist) {
        doDoTodoCreate().withAuth(auth).withTitle(title).withExternalId(todolist.getExternalId()).callDoTodoCreate();
        return selectFirst(getAllTodoLists(), having(on(Todo.class).getTitle(), equalTo(title)));
    }

    @Step("Возвращаем пользователя в “Списки дел“")
    public ApiTodoSteps todoSettingsSetOpenTodoList() {
        doTodoSettings().withAuth(auth).setTodoList().callDoTodoSettings();
        return this;
    }

    @Step("Сворачиваем список дел пользователя")
    public ApiTodoSteps hideTodoList() {
        doTodoSettings().withAuth(auth).hideTodoList().callDoTodoSettings();
        return this;
    }

    @Step("Скрываем “Списки дел“")
    public ApiTodoSteps todoSettingsSetCloseTodoList() {
        doTodoSettings().withAuth(auth).closeTodoList().callDoTodoSettings();
        return this;
    }
}
