package ru.yandex.autotests.direct.cmd.util;

import org.hamcrest.Matcher;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

public class TestUtils {

    @Step("Получаем первый элемент из коллекции, удовлетворяющий условию {1}")
    public static <T> T findFirstWithAssumption(List<T> list, Matcher<? super T> matcher) {
        AllureUtils.addJsonAttachment("Коллекция", JsonUtils.toString(list));
        T res = list.stream()
                .filter(n -> matcher.matches(n))
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в коллекции есть элемент"));
        AllureUtils.addJsonAttachment("Найденный элемент", JsonUtils.toString(res));
        return res;
    }

}
