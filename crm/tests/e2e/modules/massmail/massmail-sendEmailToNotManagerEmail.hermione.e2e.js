const { assert } = require('chai');
const { MassmailLocators } = require('../../pages/locators/massmail');
const accountWithManagers = 'second lid with manager';
const notManagerEmail = 'test@yandex-team.ru; ';
const warningText = 'В поле "копия" есть адреса, не принадлежащие менеджерам: test@yandex-team.ru';

module.exports = async function() {
  const { browser } = this;

  //Форма создания новой рассылки открыта
  await browser.setMeta('2', 'Добавить аккаунт');
  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();
  await addAccount.click();
  await browser.chooseAccount(accountWithManagers);

  await browser.setMeta('3', 'Кликнуть в поле "Копия"');
  const fieldCopy = await browser.$(MassmailLocators.MAIL_COPY_INPUT);
  await fieldCopy.waitForDisplayed();
  await fieldCopy.click();

  await browser.setMeta('4', 'Добавить адрес не из саджеста');
  await fieldCopy.setValue(notManagerEmail);

  await browser.setMeta('5', 'Кликнуть Посмотреть и Отправить');
  const buttonViewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await buttonViewAndSend.waitForDisplayed();
  await buttonViewAndSend.click();

  await browser.setMeta('6', 'Увидеть сообщение о сохранении рассылки');
  const popupSavedDraft = await browser.$(MassmailLocators.MESSAGE_MASSMAIL_SAVED);
  await popupSavedDraft.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  await browser.setMeta('7', 'Подождать, когда сообщение скроется');
  await popupSavedDraft.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  await browser.setMeta('8', 'Кликнуть Отправить');
  const buttonSend = await browser.$(MassmailLocators.SEND_MAIL_BUTTON_BORDER);
  await buttonSend.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });
  await buttonSend.click();

  await browser.setMeta('9', 'Увидеть предупреждение о невозможности отправки');
  const messageIsNotMassmailSent = await browser.$(MassmailLocators.MESSAGE_MASSMAIL_TEXT);
  await messageIsNotMassmailSent.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  const messageText = await messageIsNotMassmailSent.getText();

  await browser.setMeta('10', 'Сравнить текст с ожидаемым');
  assert.equal(messageText, warningText, 'error message wrong or not displayed');
};
