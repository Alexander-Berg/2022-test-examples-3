package ru.yandex.market.tsum.pipe.ui.page_objects.common;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 14/05/2019
 */
public class PipeLaunchPage {
    @Name("Пайплайн")
    @FindBy(className = "ui-tests-pipe-graph")
    public PipeGraph pipeGraph;
}
