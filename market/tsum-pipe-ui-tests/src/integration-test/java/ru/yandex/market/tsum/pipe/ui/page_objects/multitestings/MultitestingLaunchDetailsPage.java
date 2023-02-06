package ru.yandex.market.tsum.pipe.ui.page_objects.multitestings;

import org.openqa.selenium.support.FindBy;
import ru.yandex.market.tsum.pipe.ui.page_objects.common.PipeGraph;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.03.2018
 */
public class MultitestingLaunchDetailsPage {
    @Name("Ссылка на страницу мультитестинга в хлебных крошках")
    @FindBy(xpath = "//a[starts-with(@href,'/pipe/projects/ui-tests/multitestings/environments/')]")
    public HtmlElement multitestingPageBreadcrumbsLink;

    @Name("Пайплайн")
    @FindBy(className = "ui-tests-pipe-graph")
    public PipeGraph pipeGraph;
}
