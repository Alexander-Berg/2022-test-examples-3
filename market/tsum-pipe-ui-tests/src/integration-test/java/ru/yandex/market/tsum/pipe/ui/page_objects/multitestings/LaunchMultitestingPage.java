package ru.yandex.market.tsum.pipe.ui.page_objects.multitestings;

import org.openqa.selenium.support.FindBy;
import ru.yandex.market.tsum.pipe.ui.page_objects.common.ManualResourcesForm;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.03.2018
 */
public class LaunchMultitestingPage {
    @Name("Ручные ресурсы")
    @FindBy(id = "environment-form")
    public ManualResourcesForm manualResourcesForm;

    @Name("Кнопка 'Создать и запустить'")
    @FindBy(className = "ui-tests-launch-button")
    public HtmlElement launchButton;
}
