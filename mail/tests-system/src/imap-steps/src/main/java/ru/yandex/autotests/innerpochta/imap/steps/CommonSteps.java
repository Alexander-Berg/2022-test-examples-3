package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.List;

import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * Created by kurau on 16.07.14.
 */
public class CommonSteps {

    private CommonSteps() {
    }

    public static CommonSteps common() {
        return new CommonSteps();
    }


    @Step("Проверяем, что в строчке {0} есть вхождение {1}")
    public void shouldContainsString(String string, String substring) {
        assertThat("Запрос не содержит нужное вхождение", string, containsString(substring));
    }

    @Step("Проверяем, что в строчке {0} нет вхождения {1}")
    public void shouldNotContainsString(String string, String substring) {
        assertThat("Запрос не содержит нужное вхождение", string, not(containsString(substring)));
    }

    @Step("Проверяем, что список {0} не пустой")
    public void shouldNonEmptyList(List<String> list) {
        assertThat("Список оказался пустым", list, hasSize(greaterThan(0)));
    }
}
