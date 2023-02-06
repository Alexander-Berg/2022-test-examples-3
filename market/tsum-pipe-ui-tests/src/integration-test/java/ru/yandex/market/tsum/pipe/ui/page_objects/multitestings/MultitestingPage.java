package ru.yandex.market.tsum.pipe.ui.page_objects.multitestings;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.03.2018
 */
public class MultitestingPage {
    @Name("Статус")
    @FindBy(className = "ui-tests-status")
    public HtmlElement status;

    @Name("Ссылка на последний запуск")
    @FindBy(className = "ui-tests-last-launch-link")
    public HtmlElement lastLaunchLink;

    @Name("Кнопка 'Запустить выкладку'")
    @FindBy(className = "ui-tests-launch-button")
    public HtmlElement launchButton;

    @Name("Кнопка 'Освободить'")
    @FindBy(className = "ui-tests-cleanup-button")
    public HtmlElement cleanupButton;

    @Name("Кнопка 'Освободить и заархивировать'")
    @FindBy(className = "ui-tests-cleanup-and-archive-button")
    public HtmlElement cleanupAndArchiveButton;
}
