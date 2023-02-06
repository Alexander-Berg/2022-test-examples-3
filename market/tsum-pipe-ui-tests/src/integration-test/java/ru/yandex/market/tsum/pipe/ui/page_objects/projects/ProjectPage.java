package ru.yandex.market.tsum.pipe.ui.page_objects.projects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.market.tsum.pipe.ui.TestData;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.03.2018
 */
public class ProjectPage {
    @Name("Ссылка 'Простой пайплайн'")
    @FindBy(xpath = "//a[contains(@class, 'ui-tests-pipeline-button') and " +
        "@data-ui-tests-pipeline-id='ui-tests-simple']")
    public HtmlElement testSimpleLink;


    @Name("Ссылка 'Мультитестовые среды'")
    @FindBy(linkText = "Мультитестовые среды")
    public HtmlElement multitestingsLink;

    @Name("Кнопка 'Создание среды'")
    @FindBy(linkText = "Создание среды")
    public HtmlElement createMultitestingButton;

    @Name("Ссылка на долгоживущий МТ")
    @FindBy(linkText = TestData.OLD_MT_NAME)
    public HtmlElement oldMultitestingLink;

    @Name("Ссылка на МТ с долгоживущим запуском")
    @FindBy(linkText = TestData.MT_WITH_OLD_LAUNCH_NAME)
    public HtmlElement multitestingWithOldLaunchLink;

    @Name("Ссылка на долгоживущий релиз")
    @FindBy(xpath = "//*[contains(@class, 'ui-tests-release-link') and contains(., '" + TestData.OLD_RELEASE_NAME + "')]")
    public HtmlElement oldReleaseLink;

    @Name("Ссылка 'Управление пайплайнами'")
    @FindBy(xpath = "//*[@data-ui-tests-id='pipeline-management-link']")
    public HtmlElement pipelineManagementLink;
}
