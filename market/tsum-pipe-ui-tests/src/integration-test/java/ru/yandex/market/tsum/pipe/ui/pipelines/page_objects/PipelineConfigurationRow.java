package ru.yandex.market.tsum.pipe.ui.pipelines.page_objects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 01/07/2019
 */

public class PipelineConfigurationRow extends HtmlElement {
    @Name("Меню")
    @FindBy(className = "dropdown")
    public HtmlElement menuButton;

    @Name("Кнопка 'Создать пайплайн'")
    @FindBy(xpath = "//a[@data-ui-tests-row-action='delete']")
    public HtmlElement deleteButton;
}

