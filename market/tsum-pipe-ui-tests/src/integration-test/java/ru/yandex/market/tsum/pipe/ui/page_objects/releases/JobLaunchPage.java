package ru.yandex.market.tsum.pipe.ui.page_objects.releases;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 19.11.18
 */
public class JobLaunchPage {
    @Name("Ссылка на лог")
    @FindBy(className = "ui-tests-job-logs-link")
    public HtmlElement jobLogsLink;
}
