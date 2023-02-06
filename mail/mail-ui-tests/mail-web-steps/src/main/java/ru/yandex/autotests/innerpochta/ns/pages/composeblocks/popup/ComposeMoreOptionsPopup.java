package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author eremin-n-s
 */
public interface ComposeMoreOptionsPopup extends MailElement {

  @Name("Опция Автокомплит")
  @FindByCss(".qa-Compose-MoreOptions-Autocomplete")
  MailElement autocompleteOption();

  @Name("Тоггл Автокомплит")
  @FindByCss(".qa-Compose-MoreOptions-Autocomplete .ComposeOptionItem-Toggle")
  MailElement autocompleteToggle();

  @Name("Тоггл Смартсабджект")
  @FindByCss(".qa-Compose-MoreOptions-SmartSubject .ComposeOptionItem-Toggle")
  MailElement smartsubjectToggle();

  @Name("Опция Добавить метки")
  @FindByCss(".qa-Compose-MoreOptions-Labels")
  MailElement addLabelsOption();

}
