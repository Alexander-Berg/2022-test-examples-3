package ru.yandex.autotests.directmonitoring.tests.tns;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.directmonitoring.tests.BaseDirectMonitoringTest;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;

/**
 * User: buhter
 * Date: 01.11.12
 * Time: 14:57
 */
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.TNS)
@Title("Проверка наличия счетчика TNS на основных страницах")
public class TNSCounterTest extends BaseDirectMonitoringTest {

    @Override
    public void additionalActions() {}

    @Test
    @Title("Проверка счетчика TNS на главной странице")
    public void TNSCounterOnMainPageTest(){
        user.inBrowserAddressBar().openDirectPage();
        user.onMainPage().shouldSeeTNSCounter("Счетчик TNS не найден на главной странице", exists());
    }

    @Test
    @Title("Проверка счетчика TNS на странице 'Мои кампании'")
    public void TNSCounterOnShowCampsPageTest(){
        user.inBrowserAddressBar().openShowCampsPage();
        user.onMainPage().shouldSeeTNSCounter("Счетчик TNS не найден на странице 'Мои кампании'", exists());
    }

}