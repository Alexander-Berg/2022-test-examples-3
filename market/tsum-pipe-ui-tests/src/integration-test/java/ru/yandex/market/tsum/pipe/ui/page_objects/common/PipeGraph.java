package ru.yandex.market.tsum.pipe.ui.page_objects.common;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.03.2018
 */
public class PipeGraph extends HtmlElement {
    private static final String JOB_1_XPATH = "//*[contains(@class, 'ui-tests-job') and @data-ui-tests-job-id='job1']";

    @Name("Кнопка, запускающая джобу с ручным подтверждением")
    @FindBy(className = "ui-tests-job-launch-button")
    public HtmlElement jobLaunchButton;

    @Name("Джоба с id 'job1'")
    @FindBy(xpath = JOB_1_XPATH)
    public HtmlElement job1;

    @Name("Кнопка рестарта джобы с id 'job1'")
    @FindBy(xpath = JOB_1_XPATH + "//*[contains(@class, 'ui-tests-relaunch-button')]")
    public HtmlElement restartJob1Button;

    @Name("Ссылка на последний запуск джобы с id 'job1'")
    @FindBy(xpath = JOB_1_XPATH + "//*[contains(@class, 'job-details-link')]")
    public HtmlElement job1LastLaunchLink;
}
