package ru.yandex.market.tsum.pipe.ui.page_objects.releases;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.03.2018
 */
public class ReleaseLaunchPage {
    @Name("Поле для ввода 'Релизная версия'")
    @FindBy(id = "resources.1bc015fa-e621-4986-be8a-5b66fd7251fc.name")
    public HtmlElement simplePipelineFixVersionField;

    @Name("Кнопка 'Запустить'")
    @FindBy(className = "ui-tests-launch-button")
    public HtmlElement launchButton;
}
