package ru.yandex.market.tsum.pipe.ui.page_objects.multitestings;

import org.openqa.selenium.support.FindBy;
import ru.yandex.market.tsum.pipe.ui.page_objects.common.ManualResourcesForm;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.Select;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.03.2018
 */
public class CreateMultitestingPage {
    @Name("Поле 'Имя'")
    @FindBy(xpath = "//input[@name='name']")
    public HtmlElement nameTextInput;

    @Name("Поле 'Заголовок'")
    @FindBy(xpath = "//input[@name='title']")
    public HtmlElement titleTextInput;

    @Name("Поле 'Пайплайн'")
    @FindBy(xpath = "//*[@data-ui-tests-id='environment-form-pipeline-select']//input[starts-with(@id, 'react-select')]")
    public Select pipelineSelect;

    @Name("Ручные ресурсы")
    @FindBy(id = "environment-resources-form")
    public ManualResourcesForm manualResourcesForm;

    @Name("Кнопка 'Создать'")
    @FindBy(xpath = "//button[@type='submit']")
    public HtmlElement createAndLaunchMultitestingButton;
}
