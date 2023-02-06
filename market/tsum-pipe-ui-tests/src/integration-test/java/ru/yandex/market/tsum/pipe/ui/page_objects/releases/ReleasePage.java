package ru.yandex.market.tsum.pipe.ui.page_objects.releases;

import org.openqa.selenium.support.FindBy;
import ru.yandex.market.tsum.pipe.ui.page_objects.common.PipeGraph;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.03.2018
 */
public class ReleasePage {
    @Name("Пометка 'Завершён'")
    @FindBy(className = "ui-tests-label-finished")
    public HtmlElement finishedLabel;

    @Name("Пайплайн")
    @FindBy(className = "ui-tests-pipe-graph")
    public PipeGraph pipeGraph;
}
