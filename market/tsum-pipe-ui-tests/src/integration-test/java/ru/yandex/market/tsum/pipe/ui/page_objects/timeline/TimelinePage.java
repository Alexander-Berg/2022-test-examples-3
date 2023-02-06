package ru.yandex.market.tsum.pipe.ui.page_objects.timeline;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 12.10.18
 */
public class TimelinePage {
    @Name("Панель фильтров")
    @FindBy(className = "filterPanel")
    public HtmlElement filterPanel;

    @Name("Чарт таймлайна")
    @FindBy(id = "timeline")
    public HtmlElement timelineChart;

    @Name("Теги")
    @FindBy(id ="tag-filter")
    public HtmlElement tagFilterInput;

    @Name("Типы")
    @FindBy(id ="type-filter")
    public HtmlElement typeFilterInput;

    @Name("Авторы")
    @FindBy(id ="author-filter")
    public HtmlElement authorFilterInput;

    @Name("Проекты")
    @FindBy(id ="project-filter")
    public HtmlElement projectFilterInput;

    @Name("Кнопка сброса фильтров")
    @FindBy(id = "filtersResetButton")
    public HtmlElement resetFiltersButton;
}
