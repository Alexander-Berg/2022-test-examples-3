package ru.yandex.autotests.directmonitoring.tests.search;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.directmonitoring.tests.BaseDirectMonitoringTest;

import java.util.Arrays;
import java.util.Collection;

/**
* User: buhter
* Date: 28.10.12
* Time: 0:12
*/
@RunWith(Parameterized.class)
public abstract class SearchHelpTestBase extends BaseDirectMonitoringTest {

    @Parameterized.Parameter(value = 0)
    public String query;

    @Parameterized.Parameters(name = "запрос: {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
                {"реклама"},
                {"объявление"},
                {"платеж"},
                {"контекстный"},
                {"поисковая"},
                {"ctr"}
        };

        return Arrays.asList(data);
    }

    @Override
    public void additionalActions() {
        user.inBrowserAddressBar().openHelpSearchPage();
        user.onHelpSearchPage().search(query);
    }
}