package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.TemplateElement;

/**
 * @author eremin-n-s
 */
public interface SmartSubjectPopup extends MailElement {

  @Name("Варианты саджеста темы")
  @FindByCss(".ComposeReactEmptySubjectPopup-SuggestItem")
  ElementsCollection<MailElement> suggestItem();

  @Name("Кнопка «Отправить»")
  @FindByCss(".ComposeConfirmPopup-Button_action")
  MailElement sendBtn();

  @Name("Строка ввода темы")
  @FindByCss(".ComposeReactEmptySubjectPopup-Input")
  MailElement themeInput();

  @Name("Кнопка «Вернуться к письму»")
  @FindByCss(".ComposeConfirmPopup-Button_cancel")
  MailElement backToComposeBtn();

  @Name("Кнопка «Отправить без темы»")
  @FindByCss(".ComposeConfirmPopup-Button")
  MailElement sendWithoutSubjectBtn();

  @Name("Чекбокс «Больше не предлагать»")
  @FindByCss("[type='checkbox']")
  MailElement doNotOfferCheckbox();
}
