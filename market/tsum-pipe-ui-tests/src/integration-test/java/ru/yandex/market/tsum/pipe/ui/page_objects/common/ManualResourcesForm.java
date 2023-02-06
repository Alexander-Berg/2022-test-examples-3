package ru.yandex.market.tsum.pipe.ui.page_objects.common;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 19.03.2018
 */
public class ManualResourcesForm extends HtmlElement {
    @Name("Поле для ввода 'Релизная версия'")
    @FindBy(id = "resources.1bc015fa-e621-4986-be8a-5b66fd7251fc.name")
    public HtmlElement simplePipelineFixVersionField;
}
