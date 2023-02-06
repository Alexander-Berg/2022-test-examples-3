package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.TemplateElement;

/**
 * @author eremin-n-s
 */
public interface TemplatePopup extends MailElement {

  @Name("Cписок «Шаблоны»")
  @FindByCss(".ComposeTemplatesList-Item")
  ElementsCollection<TemplateElement> templateList();

  @Name("Опция «Обновить текущий шаблон»")
  @FindByCss(".ComposeTemplatesOptions-Action_update")
  MailElement updateBtn();

  @Name("Опция «Сохранить как шаблон»")
  @FindByCss(".ComposeTemplatesOptions-Action_save")
  MailElement saveBtn();

  @Name("Кнопка «Сохранить как новый шаблон»")
  @FindByCss(".ComposeTemplatesOptions-Action_save_as")
  MailElement saveAsNewTemplateBtn();
}
