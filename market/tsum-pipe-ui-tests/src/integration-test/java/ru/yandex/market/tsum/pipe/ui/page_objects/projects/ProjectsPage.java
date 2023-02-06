package ru.yandex.market.tsum.pipe.ui.page_objects.projects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.List;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.02.2018
 */
public class ProjectsPage {
    @Name("Ссылка 'Тестовые пайплайны'")
    @FindBy(linkText = "Тестовый проект UI-тестов ЦУМа")
    public HtmlElement testProjectsLink;

    @Name("Ссылка 'Тестовые пайплайны'")
    @FindBy(css = "a.project-list-item")
    public List<HtmlElement> projectLinks;
}
