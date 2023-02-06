package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 03.10.12
 * Time: 18:30
 */
public interface LabelsNavigationBlock extends MailElement {

    @Name("Список пользовательских меток")
    @FindByCss(".qa-LeftColumn-Label")
    ElementsCollection<CustomLabelBlock> userLabels();

    @Name("Список неиспользуемых пользовательских меток")
    @FindByCss(".mail-LabelList-Wrap.mail-LabelList-Wrap_unused .mail-LabelList-Item")
    ElementsCollection<CustomLabelBlock> userUnusedLabels();

    @Name("Список используемых пользовательских меток")
    @FindByCss(".mail-LabelList-Wrap:not(.mail-LabelList-Wrap_unused) .mail-LabelList-Item")
    ElementsCollection<CustomLabelBlock> userUsedLabels();

    @Name("Цвета пользовательских меток")
    @FindByCss("div.b-labels__users .b-label__first-letter")
    ElementsCollection<WebElement> userLabelsColors();
}
