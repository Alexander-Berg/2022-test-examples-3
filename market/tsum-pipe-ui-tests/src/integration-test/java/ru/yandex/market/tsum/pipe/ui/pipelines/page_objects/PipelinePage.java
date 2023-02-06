package ru.yandex.market.tsum.pipe.ui.pipelines.page_objects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.List;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 01/07/2019
 */
public class PipelinePage {
    @Name("Кнопка 'Создать новую версию'")
    @FindBy(xpath = "//*[@data-ui-tests-id='create_pipeline_draft']")
    public HtmlElement createDraftButton;


    @Name("Драфты")
    @FindBy(className = "ui-tests-configuration-status-DRAFT")
    public List<PipelineConfigurationRow> draftRows;

    @Name("Активные версии")
    @FindBy(className = "ui-tests-configuration-status-PUBLIC")
    public List<PipelineConfigurationRow> publicRows;


}
