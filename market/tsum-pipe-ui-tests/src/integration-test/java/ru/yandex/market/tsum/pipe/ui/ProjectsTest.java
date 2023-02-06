package ru.yandex.market.tsum.pipe.ui;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.ui.common.TsumUrls;
import ru.yandex.market.tsum.pipe.ui.common.WebDriverRule;
import ru.yandex.market.tsum.pipe.ui.page_objects.MainPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectPage;
import ru.yandex.market.tsum.pipe.ui.page_objects.projects.ProjectsPage;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 05.04.2018
 */
@DisplayName("Страницы всех проектов")
public class ProjectsTest {
    @Rule
    public final WebDriverRule webDriver = new WebDriverRule(TsumUrls.mainPage());

    private final MainPage mainPage = webDriver.createPageObject(MainPage::new);
    private final ProjectsPage projectsPage = webDriver.createPageObject(ProjectsPage::new);

    private final ProjectPage projectPage = webDriver.createPageObject(ProjectPage::new);

    @Test
    @DisplayName("Страницы всех проектов загружаются")
    public void allProjectPagesLoad() {
        goToProjectsPage();

        Set<String> projectIds = projectsPage.projectLinks.stream()
            .map(link -> link.getAttribute("data-ui-tests-project-id"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Assert.assertTrue(projectIds.size() > 0);

        projectIds.forEach(this::checkProjectPage);
    }

    @Step("Переходим на страницу со списком проектов")
    private void goToProjectsPage() {
        webDriver.click(mainPage.projectsLink);
        webDriver.assertWaitStep(projectsPage.testProjectsLink, isDisplayed());
    }

    @Step("Проверяем проект '{projectId}'")
    private void checkProjectPage(String projectId) {
        webDriver.get(TsumUrls.projectPage(projectId));
        webDriver.assertWaitStep(projectPage.multitestingsLink, isDisplayed());
    }
}
