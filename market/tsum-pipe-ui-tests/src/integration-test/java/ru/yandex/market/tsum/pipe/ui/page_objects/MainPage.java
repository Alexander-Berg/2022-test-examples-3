package ru.yandex.market.tsum.pipe.ui.page_objects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.02.2018
 */
public class MainPage {
    @Name("Ссылка 'Инструменты диагностики'")
    @FindBy(linkText = "Инструменты диагностики")
    public HtmlElement diagnosticToolsLink;

    @Name("Ссылка 'Релизы'")
    @FindBy(linkText = "Релизы")
    public HtmlElement projectsLink;
}
