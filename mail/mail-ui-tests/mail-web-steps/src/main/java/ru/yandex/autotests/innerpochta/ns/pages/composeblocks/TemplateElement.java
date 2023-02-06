package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author eremin-n-s
 */
public interface TemplateElement extends MailElement {

  @Name("Наименование шаблона")
  @FindByCss(".ComposeTemplatesList-Text")
  MailElement templateElementText();

}
