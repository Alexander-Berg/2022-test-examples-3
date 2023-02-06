package ru.yandex.autotests.directmonitoring.tests.search;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.*;

/**
* User: buhter
* Date: 28.10.12
* Time: 0:12
*/
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.SEARCH)
@Title("Проверка результатов поиска в помощи")
public class SearchHelpResultsTest extends SearchHelpTestBase {

    @Test
    @Title("Проверка вероятно пустой выдачи")
    public void probablyEmptyIssuanceTest() {
        String message = String.format("Вероятно пустая выдача: '0' в тексте для слова '%s'", query);
        user.onHelpSearchPage().shouldSeeResults(message, not(endsWith(" 0")));
    }

    @Test
    @Title("Проверка искомой комбинации слов")
    public void combinationOfWordsTest() {
        String message = String.format("'Искомая комбинация слов нигде не встречается' для слова '%s'", query);
        String error = "Искомая комбинация слов нигде не встречается";
        user.onHelpSearchPage().shouldSeeResults(message, not(containsString(error)));
    }
}
