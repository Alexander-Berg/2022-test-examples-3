package ru.yandex.market.tsum.pipe.ui.pipelines.page_objects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 01/07/2019
 */
public class PipelineEditorPage {
    public static final String JOB_FORM_ID = "job-form";

    @Name("Кнопка 'Создать'")
    @FindBy(xpath = "//*[@data-ui-tests-id='menu-create-button']")
    public HtmlElement createButton;

    @Name("Кнопка 'Сохранить'")
    @FindBy(xpath = "//*[@data-ui-tests-id='menu-save-button']")
    public HtmlElement saveButton;

    @Name("Кнопка 'Сохранить'")
    @FindBy(xpath = "//*[@data-ui-tests-id='menu-exit-button']")
    public HtmlElement exitButton;


    @Name("Создание и редактирование джобы")
    @FindBy(id = JOB_FORM_ID)
    public CreateJobDialog createOrEditJobDialog;
}
