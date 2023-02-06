package ru.yandex.autotests.innerpochta.data;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * @author a-zoshchuk
 */
public interface Select extends MailElement {

    default List<WebElement> getOptions() {
        return this.findElements(By.cssSelector("option"));
    }

    default WebElement getOptionByText(String optionText) {
        List<WebElement> options = filter(hasText(containsString(optionText)), getOptions());
        assertThat("Нет опции с нужным текстом", options, hasSize(greaterThan(0)));
        return options.get(0);
    }

    default WebElement getOptionByNumber(int num) {
        List<WebElement> options = getOptions();
        assertThat("Опции не найдены", options, hasSize(greaterThan(0)));
        assertThat("Количество опций меньше ожидаемого", options, hasSize(greaterThan(num - 1)));
        return options.get(num);
    }
}
