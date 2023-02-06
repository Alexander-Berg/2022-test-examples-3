package ru.yandex.market.tsum.pipe.ui.pipelines.page_objects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 01/07/2019
 */
public class PipelinesPage {
    @Name("Ссылка на тестовый пайплайна")
    @FindBy(className = "ui-tests-pipeline-ui-tests-simple")
    public HtmlElement testPipeline;
}
