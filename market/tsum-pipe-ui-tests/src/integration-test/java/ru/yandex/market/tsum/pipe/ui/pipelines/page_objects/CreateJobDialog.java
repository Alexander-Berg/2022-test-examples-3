package ru.yandex.market.tsum.pipe.ui.pipelines.page_objects;

import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.Select;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 01/07/2019
 */

public class CreateJobDialog extends HtmlElement {
    @Name("Поле для ввода 'Title'")
    @FindBy(id = "job-form-title")
    public HtmlElement title;

    @Name("Поле 'Пайплайн'")
    @FindBy(xpath = "//*[@data-ui-tests-id='job-form-executor-select']//input[starts-with(@id, 'react-select')]")
    public Select executorSelect;

    @Name("Кнопка 'Создать пайплайн'")
    @FindBy(xpath = "//*[@data-ui-tests-id='submit-button']")
    public HtmlElement saveButton;
}

