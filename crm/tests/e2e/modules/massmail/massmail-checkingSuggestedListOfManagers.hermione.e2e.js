const { assert } = require('chai');
const { MassmailLocators } = require('../../pages/locators/massmail');
const accountWithManagers = 'second lid with manager';
const emailManager = 'test-robot-space-odyssey@yandex-team.ru; ';

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

  await browser.setMeta('4', 'Увидеть саджест');
  const listSuggest = await browser.$(MassmailLocators.SUGGEST_OF_MANAGERS);
  await listSuggest.waitForDisplayed();

  await browser.setMeta('5', 'Кликнуть на первый в списке email');
  const suggestListItem = await browser.$(MassmailLocators.ITEM_IN_SUGGEST_OF_MANAGERS);
  await suggestListItem.waitForDisplayed();
  await suggestListItem.click();

  await browser.setMeta('6', 'Почитать значение подставленное в поле Копия');
  const textInCopyField = await fieldCopy.getValue();

  await browser.setMeta('7', 'Сравнить значение из саджеста со значением в поле Копия');
  assert.equal(textInCopyField, emailManager, 'email in suggest list is not valid');

  await browser.setMeta('8', 'Кликнуть Посмотреть и Отправить');
  const buttonViewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await buttonViewAndSend.waitForDisplayed();
  await buttonViewAndSend.click();

  await browser.setMeta('9', 'Увидеть сообщение о сохранении рассылки');
  const popupSavedDraft = await browser.$(MassmailLocators.MESSAGE_MASSMAIL_SAVED);
  await popupSavedDraft.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  await browser.setMeta('10', 'Подождать, когда сообщение скроется');
  await popupSavedDraft.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  await browser.setMeta('11', 'Кликнуть Отправить');
  const buttonSend = await browser.$(MassmailLocators.SEND_MAIL_BUTTON_BORDER);
  await buttonSend.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });
  await buttonSend.click();

  await browser.setMeta('12', 'Увидеть сообщение об успеной отправке');
  const messageSent = await browser.$(MassmailLocators.MESSAGE_MASSMAIL_SENT);
  const messageIsDispalyed = await messageSent.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  assert.isTrue(messageIsDispalyed, 'massmail was not sent');
};
