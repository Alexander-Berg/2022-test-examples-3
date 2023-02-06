package ru.yandex.autotests.innerpochta.cal.steps;

import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.ENABLE_TAB_SELECTOR;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TODO_DATE_EN_FORMAT;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TODO_DATE_FORMAT;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TODO_MENU_TITLE;

/**
 * @author marchart
 */
public class CalTouchTodoSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    public CalTouchTodoSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Открываем Список дел")
    public CalTouchTodoSteps openTodo() {
        user.defaultSteps().clicksOn(user.pages().calTouch().burger())
            .clicksOnElementWithText(user.pages().calTouch().sidebar().menuItems(), TODO_MENU_TITLE)
            .refreshIfSee(user.pages().calTouch().notificationMsg());
        return this;
    }

    @Step("Добавляем дело с именем «{0}» и датой «{1}»")
    public CalTouchTodoSteps createToDoItemWithDate(String name, String date) {
        int count = user.pages().calTouch().todo().todoLists().get(0).items().size();
        user.defaultSteps()
            .shouldSee(user.pages().calTouch().todo().todoLists().waitUntil(not(empty())).get(0).items())
            .clicksOn(user.pages().calTouch().todo().todoLists().get(0).newTodoItemBtn())
            .inputsTextInElement(user.pages().calTouch().todo().todoLists().get(0).items().get(count).inputName(), name)
            .appendTextInElement(user.pages().calTouch().todo().todoLists().get(0).items().get(count).inputDate(), date)
            .clicksOn(user.pages().calTouch().todo())
            .shouldNotSee(user.pages().calTouch().todo().todoLists().get(0).items().get(count).inputName())
            .shouldSeeThatElementHasText(
                user.pages().calTouch().todo().todoLists().get(0).items().get(count).itemName(),
                name
            );
        return this;
    }

    @Step("В списке «{0}» у элемента «{1}» должна быть дата «{2}»")
    public CalTouchTodoSteps shouldSeeItemDate(int list, int item, String changedDate) {
        user.defaultSteps().shouldSeeThatElementHasText(
            user.pages().calTouch().todo().todoLists().get(list).items().get(item).itemDate(),
            changedDate
        );
        return this;
    }

    @Step("Возвращаем формат даты в зависимости от текущего языка браузера")
    public String getRegionDateFormat(LocalDateTime date) {
        String return_value = user.defaultSteps()
            .executesJavaScriptWithResult("return window.navigator.userLanguage || window.navigator.language");
        if (return_value.equals("ru-RU")) {
            return TODO_DATE_FORMAT.format(date);
        } else return TODO_DATE_EN_FORMAT.format(date);
    }

    @Step("Открываем таб «{0}» и проверяем что он стал активен")
    public CalTouchTodoSteps openTab(WebElement tab) {
        user.defaultSteps().shouldNotSee(user.pages().calTouch().loader())
            .clicksOn(tab)
            .shouldContainsAttribute(
                tab,
                "class",
                ENABLE_TAB_SELECTOR
            );
        return this;
    }
}
