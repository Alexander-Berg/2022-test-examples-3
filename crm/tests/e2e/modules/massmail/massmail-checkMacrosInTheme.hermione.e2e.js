const { assert } = require('chai');
const { MassmailLocators } = require('../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;
  const macroses =
    '{{Contact.LastName}} {{Contact.FirstName}} {{Contact.MiddleName}} {{Contact.AccountName}}';
  const accountTheSameEmail = 'тестовый аккаунт с N почтами'; //Аккаунт, который будем добавлять в рассылку

  await browser.setMeta('1', 'Кликнуть в поле Тема');
  const feildTheme = await browser.$(MassmailLocators.MAIL_TOPIC_INPUT);
  await feildTheme.waitForDisplayed();
  await feildTheme.click();

  await browser.setMeta('2', 'Вставить макросы в поле Тема');
  await feildTheme.setValue(macroses);
  await browser.pause(1000);

  await browser.setMeta('3', 'нажать кнопку "Добавить Аккаунт"');
  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();
  await addAccount.click();
  await browser.chooseAccount(accountTheSameEmail);

  await browser.setMeta('4', 'нажать на "Выбрать тип контакта"');
  const contactType = await browser.$(MassmailLocators.CONTACT_TYPE_LISTBOX);
  await contactType.waitForDisplayed();
  await contactType.click();

  await browser.setMeta('5', 'Выбрать из списка тип "Все"');
  const contactTypeAll = await browser.$(MassmailLocators.CONTACT_TYPE_ALL);
  await contactTypeAll.waitForDisplayed({
    timeout: 5000,
    interval: 500,
  });
  await contactTypeAll.click();
  await browser.pause(2000);

  await browser.setMeta('6', 'Нажать "Просмотреть и отправить"');
  const buttonViewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await buttonViewAndSend.waitForDisplayed();
  await buttonViewAndSend.click();

  await browser.setMeta('7', 'Найти список контактов аккаунта');
  const contactsList = await browser.$$(MassmailLocators.CONTACT_NAME);

  await browser.setMeta('8', 'Найти и прочитать название выбранного аккаунта');
  const selectedAccount = await browser.$(MassmailLocators.SELECLED_ACCOUNT);
  const selectedAccountText = await selectedAccount.getText();

  await browser.setMeta('9', 'Найти на черновике тему рассылки');
  const mailTopicDraft = await browser.$(MassmailLocators.MAIL_TOPIC_DRAFT);

  let i = 0;
  while (i < contactsList.length) {
    await browser.setMeta('10', 'Кликнуть на контакт в списке и прочитать его название');
    await contactsList[i].click();
    let itemNameText = await contactsList[i].getText();
    await browser.pause(500);

    await browser.setMeta('11', 'Прочитать тему в рассылке');
    let mailTopicDraftText = await mailTopicDraft.getText();

    await browser.setMeta('12', 'Проверить, что имя контакта содержится в теме рассылки');
    assert.include(mailTopicDraftText, itemNameText, 'contact name macros is not used');

    await browser.setMeta('10', 'Проверить, что название аккаунта содержится в теме рассылки');
    assert.include(mailTopicDraftText, selectedAccountText, 'account name macros is not used');

    i = i + 1;
  }
};
