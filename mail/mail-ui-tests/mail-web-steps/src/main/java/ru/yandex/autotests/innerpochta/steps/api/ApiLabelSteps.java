package ru.yandex.autotests.innerpochta.steps.api;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.innerpochta.api.labels.DoLabelHandler.doLabelHandler;
import static ru.yandex.autotests.innerpochta.api.labels.DoLabelsAddHandler.doLabelsAddHandler;
import static ru.yandex.autotests.innerpochta.api.labels.DoLabelsDeleteHandler.doLabelsDeleteHandler;
import static ru.yandex.autotests.innerpochta.api.labels.DoUnlabelHandler.doUnlabelHandler;
import static ru.yandex.autotests.innerpochta.api.labels.LabelsHandler.labelsHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;

/**
 * Created by mabelpines
 */
public class ApiLabelSteps {

    public RestAssuredAuthRule auth;
    private AllureStepStorage user;

    public ApiLabelSteps(AllureStepStorage user) {
        this.user = user;
    }

    public ApiLabelSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: labels. Получаем все метки.")
    public List<Label> getAllLabels() {
        return Arrays.asList(labelsHandler().withAuth(auth).callLabelsHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.label", Label[].class));
    }

    @Step("Вызов api-метода: labels. Получаем все пользовательские метки.")
    public List<Label> getAllUserLabels() {
        return select(getAllLabels(), having(on(Label.class).getUser(), equalTo(true)));
    }

    @Step("Вызов api-метода: do-labels-add. Добавляем метку с именем “{0}“")
    public Label addNewLabel(String name, String color) {
        doLabelsAddHandler().withAuth(auth).withName(name).withColor(color).callDoLabelsAddHandler();
        return selectFirst(getAllUserLabels(), having(on(Label.class).getName(), equalTo(name)));
    }

    @Step("Вызов api-метода: do-labels-delete. Удаляем все пользовательские метки.")
    public ApiLabelSteps deleteAllCustomLabels() {
        getAllUserLabels().forEach(this::deleteLabel);
        return this;
    }

    @Step("Вызов api-метода: do-labels-remove. Удаляем метку: “{0}“")
    public Response deleteLabel(Label label) {
        if (label.getLid() != null) {
            return doLabelsDeleteHandler().withAuth(auth).withLids(label.getLid()).calldoLabelsDeleteHandler();
        }
        return null;
    }

    @Step("Вызов api-метода: do-labels. Помечаем письмо “{0}“ меткой “{1}“")
    public ApiLabelSteps markWithLabel(Message msg, Label label) {
        doLabelHandler().withAuth(auth).withLid(label.getLid()).withIds(msg.getMid()).callDoLabelHandler();
        return this;
    }

    @Step("Вызов api-метода: do-unlabel. Открепляем письмо: “{0}“")
    public ApiLabelSteps unPinLetter(Message msg) {
        Label pinnLabel = selectFirst(getAllLabels(), having(on(Label.class).getSymbolicName(), equalTo("pinned_label")));
        doUnlabelHandler().withAuth(auth).withIds(msg.getMid()).withLids(pinnLabel.getLid()).callDoUnlabelHandler();
        return this;
    }

    @Step("Вызов api-метода: do-unlabel. Снимаем приоритетную метку со всех писем:")
    public ApiLabelSteps unPriorityLetters() {
        Label priorityLabel = selectFirst(
            getAllLabels(),
            having(on(Label.class).getSymbolicName(), equalTo("important_label"))
        );
        user.apiMessagesSteps().getAllMessagesLabel(priorityLabel.getName())
            .forEach(msg -> doUnlabelHandler().withAuth(auth).withIds(msg.getMid()).withLids(priorityLabel.getLid()).callDoUnlabelHandler());
        return this;
    }

    @Step("Вызов api-метода: do-label. Закрепляем письмо: “{0}“")
    public ApiLabelSteps pinLetter(Message msg) {
        Label pinnLabel = selectFirst(
            getAllLabels(),
            having(on(Label.class).getSymbolicName(), equalTo("pinned_label"))
        );
        doLabelHandler().withAuth(auth).withLid(pinnLabel.getLid()).withIds(msg.getMid()).callDoLabelHandler();
        return this;
    }

    @Step("Вызов api-метода: do-label. Помечаем важным: “{0}“")
    public ApiLabelSteps markImportant(Message msg) {
        Label importantLabel = selectFirst(
            getAllLabels(),
            having(on(Label.class).getSymbolicName(), equalTo("important_label"))
        );
        doLabelHandler().withAuth(auth).withLid(importantLabel.getLid()).withIds(msg.getMid()).callDoLabelHandler();
        return this;
    }

    public Label getLabelByName(String name) {
        return selectFirst(getAllLabels(), having(on(Label.class).getName(), equalTo(name)));
    }

}
