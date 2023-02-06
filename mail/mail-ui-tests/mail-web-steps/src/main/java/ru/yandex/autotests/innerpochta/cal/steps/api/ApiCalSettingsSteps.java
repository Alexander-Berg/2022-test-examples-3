package ru.yandex.autotests.innerpochta.cal.steps.api;

import edu.emory.mathcs.backport.java.util.Arrays;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.calTodoItem.TodoItem;
import ru.yandex.autotests.innerpochta.steps.beans.layer.Layer;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.cal.api.CreateLayerHandler.createLayer;
import static ru.yandex.autotests.innerpochta.cal.api.DoCreateEventHandler.createEvent;
import static ru.yandex.autotests.innerpochta.cal.api.DoCreateTodoItemHandler.createTodoItem;
import static ru.yandex.autotests.innerpochta.cal.api.DoCreateTodoListHandler.createTodoList;
import static ru.yandex.autotests.innerpochta.cal.api.DoDeleteEventsHandler.deleteUserEvents;
import static ru.yandex.autotests.innerpochta.cal.api.DoDeleteLayerHandler.deleteLayer;
import static ru.yandex.autotests.innerpochta.cal.api.DoDeleteTodoListHandler.deleteTodoList;
import static ru.yandex.autotests.innerpochta.cal.api.DoToggleLayer.doToggleLayer;
import static ru.yandex.autotests.innerpochta.cal.api.DoUpdateUserSettingsHandler.doUpdateUserSettings;
import static ru.yandex.autotests.innerpochta.cal.api.GetUserEventsHandler.getUserEvents;
import static ru.yandex.autotests.innerpochta.cal.api.GetUserLayersHandler.getLayersHandler;
import static ru.yandex.autotests.innerpochta.cal.api.GetUserTodoListsHandler.getTodoListsHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;

/**
 * @author cosmopanda
 */
public class ApiCalSettingsSteps {

    public RestAssuredAuthRule auth;
    private AllureStepStorage user;

    public ApiCalSettingsSteps(AllureStepStorage user) {
        this.user = user;
    }

    public ApiCalSettingsSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Получаем все события")
    public List<Event> getAllEvents() {
        return Arrays.asList(getUserEvents().withAuth(auth).withLayers(getUserLayersIds()).callGetEvents().then()
            .extract().jsonPath(getJsonPathConfig()).getObject("models[0].data.events", Event[].class));
    }

    @Step("Получаем все события за сегодня")
    public List<Event> getTodayEvents() {
        return getEventsForDate(
            DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDateTime.now()),
            DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDateTime.now())
        );
    }

    @Step("Получаем все события c {0} по {1}")
    public List<Event> getEventsForDate(String fromDate, String toDate) {
        return Arrays.asList(getUserEvents().withAuth(auth).withDate(fromDate, toDate).withLayers(getUserLayersIds())
            .callGetEvents().then().extract().jsonPath(getJsonPathConfig())
            .getObject("models[0].data.events", Event[].class));
    }

    @Step("Создаем новое событие")
    public ApiCalSettingsSteps createNewEvent(Event event) {
        assertThat(
            "Событие не создалось!",
            createEvent().withAuth(auth).withEvent(event).callCreateEvent().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Создаем новое событие с указанными участниками")
    public ApiCalSettingsSteps createNewEventWithAttendees(Event event, List<String> attendees) {
        assertThat(
            "Событие не создалось!",
            createEvent().withAuth(auth).withEvent(event).withAttendees(new ArrayList<>(attendees))
                .callCreateEvent().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Создаем повторяющееся событие")
    public ApiCalSettingsSteps createNewRepeatEvent(Event event) {
        assertThat(
            "Событие не создалось!",
            createEvent().withAuth(auth).withRepeatEvent(event).callCreateEvent().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Создаем повторяющееся событие c указанными участниками")
    public ApiCalSettingsSteps createNewRepeatEventWithAttendees(Event event, List<String> attendees) {
        assertThat(
            "Событие не создалось!",
            createEvent().withAuth(auth).withRepeatEvent(event).withAttendees(new ArrayList<>(attendees))
                .callCreateEvent().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Создаем несколько одинаковых событий")
    public ApiCalSettingsSteps createCoupleOfEvents(int numOfEvents) {
        Event event = user.settingsCalSteps().formDefaultEvent(getUserLayersIds().get(0));
        for (int i = 0; i < numOfEvents; i++)
            createNewEvent(event);
        return this;
    }

    @Step("Создаем несколько одинаковых событий на весь день")
    public ApiCalSettingsSteps createCoupleOfAllDayEvents(int numOfEvents) {
        Event allDayEvent =
            user.settingsCalSteps().formDefaultAllDayEvent(getUserLayersIds().get(0));
        for (int i = 0; i < numOfEvents; i++)
            createNewEvent(allDayEvent);
        return this;
    }

    @Step("Удаляем все события за сегодня")
    public ApiCalSettingsSteps deleteTodayEvents() {
        getTodayEvents().forEach(event ->
            deleteUserEvents().withAuth(auth).withEvent(event.getId()).callDeleteEvents());
        return this;
    }

    @Step("Создаем новый слой")
    public ApiCalSettingsSteps createNewLayer(Layer layer) {
        assertThat(
            "Слой не создался!",
            createLayer().withAuth(auth).withLayer(layer).callCreateLayer().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Получаем все слои")
    public List<Layer> getUserLayers() {
        return Arrays.asList(getLayersHandler().withAuth(auth).callGetLayersHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.layers", Layer[].class));
    }

    @Step("Получаем слой по id: «{0}»")
    public Layer getLayerById(long layerId) {
        Optional<Layer> mayBeLayer =
            getUserLayers().stream().filter(userLayer -> userLayer.getId().equals(layerId)).findFirst();
        assertTrue("Слой с таким id не существует", mayBeLayer.isPresent());
        return mayBeLayer.get();
    }

    @Step("Получаем слой по имени: «{0}»")
    public Layer getLayerByName(String layerName) {
        Optional<Layer> mayBeLayer =
            getUserLayers().stream().filter(userLayer -> userLayer.getName().equals(layerName)).findFirst();
        assertTrue("Слой с таким именем не существует", mayBeLayer.isPresent());
        return mayBeLayer.get();
    }

    @Step("Получаем id всех слоев")
    public List<Long> getUserLayersIds() {
        return getUserLayers().stream().map(Layer::getId).collect(Collectors.toList());
    }

    @Step("Меняем настройки: {0}")
    public ApiCalSettingsSteps updateUserSettings(String comment, Params params) {
        assertThat(
            "Не удалось изменить настройки!",
            doUpdateUserSettings().withAuth(auth).withSettings(params).callUpdateUserSettings().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Ставим чекбокс календаря в положение {1}")
    public ApiCalSettingsSteps togglerLayer(Long layerID, Boolean status) {
        assertThat(
            "Не удалось переключить слой!",
            doToggleLayer().withAuth(auth).withLayerId(layerID, status).callDoToggleLayer().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Получаем все списки дел")
    public List<TodoItem> getTodoLists() {
        return Arrays.asList(getTodoListsHandler().withAuth(auth).callGetTodoListsHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.items", TodoItem[].class));
    }

    @Step("Удаляем все списки дел")
    public ApiCalSettingsSteps deleteAllTodoLists() {
        getTodoLists().forEach(todoList ->
            deleteTodoList().withAuth(auth).withListID(todoList.getId()).callDeleteTodoList());
        return this;
    }

    @Step("Создаем новый список дел {0}")
    public ApiCalSettingsSteps createNewTodoList(String listName) {
        assertThat(
            "Список дел не создался!",
            createTodoList().withAuth(auth).withTitle(listName).callCreateTodoList().getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Создаем дело {1} в {0} списке")
    public ApiCalSettingsSteps createNewTodoItem(int index, String name, String dueDate) {
        assertThat(
            "Дело не создалось!",
            createTodoItem().withAuth(auth)
                .withParams(getTodoLists().get(index).getId(), name, dueDate)
                .callCreateTodoItem()
                .getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Удаляем все календари")
    public ApiCalSettingsSteps deleteLayers() {
        getUserLayers().forEach(layer -> deleteLayer().withAuth(auth).withLayerID(layer.getId()).callDeleteLayer());
        return this;
    }

    @Step("Удаляем все слои и добавляем новый")
    public ApiCalSettingsSteps deleteAllAndCreateNewLayer() {
        getUserLayers().forEach(layer -> deleteLayer().withAuth(auth).withLayerID(layer.getId()).callDeleteLayer());
        assertThat(
            "Слой не создался!",
            createLayer().withAuth(auth).withLayer(user.settingsCalSteps().formDefaultLayer()).callCreateLayer()
                .getStatusCode(),
            equalTo(200)
        );
        return this;
    }

    @Step("Получаем события за два дня")
    public List<Event>[] getEventsForTwoDays() {
        List<Event> todayEvents = getTodayEvents();
        List<Event> tomorrowEvents = getEventsForDate(
            DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDateTime.now().plusDays(1L)),
            DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDateTime.now().plusDays(1L))
        );
        return new List[]{todayEvents, tomorrowEvents};
    }
}
