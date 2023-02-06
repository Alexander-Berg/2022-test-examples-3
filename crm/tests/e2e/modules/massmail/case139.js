const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');
const { MassmailLocators } = require('./../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  // receiving today's date
  const rawDate = new Date(new Date().getTime());
  const month = rawDate.getMonth() + 1;
  const day = rawDate.getDate();
  const year = rawDate.getFullYear();
  const currentDate =
    String(day).padStart(2, '0') + '.' + String(month).padStart(2, '0') + '.' + year;

  //
  await browser.setMeta('1', 'нажать кнопку "Аккаунт"');
  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();
  await addAccount.click();
  //
  await browser.setMeta('2', 'выбрать чекбоксом Нулевой аккаунт');
  const zeroAccount = await browser.$(IssuesLocators.ZERO_ACCOUNT_CHECKBOX);
  await zeroAccount.waitForDisplayed();
  await zeroAccount.click();
  //
  await browser.setMeta('3', 'нажать кнопку "Выбрать"');
  const saveAccount = await browser.$(IssuesLocators.SAVE_ACCOUNT_BUTTON);
  await saveAccount.waitForClickable();
  await saveAccount.click();
  //
  await browser.setMeta('4', 'подождать, пока окно выбора аккаунта закроется');
  await zeroAccount.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  //
  await browser.setMeta('5', 'заполнить поле "Тема" в рассылке');
  const newMailName = await browser.$(MassmailLocators.MAIL_TOPIC_INPUT);
  await newMailName.waitForDisplayed();
  await newMailName.setValue('Autotest massmail');
  //
  await browser.setMeta('6', 'нажать кнопку "Сохранить как черновик"');
  const saveDraft = await browser.$(MassmailLocators.SAVE_DRAFT_BUTTON);
  await saveDraft.waitForDisplayed({ timeout: 5000, interval: 500 });
  await saveDraft.click();
  await browser.pause(500);
  //
  await browser.setMeta('7', 'нажать на первую рассылку в списке в Черновиках');
  const latestMassMail = await browser.$(MassmailLocators.LATEST_CREATED_MASSMAIL);
  await latestMassMail.waitForDisplayed();
  const isNewMassMailOk = await latestMassMail.getText();
  //
  await browser.setMeta('8', 'проверить, что название и дата рассылки совпадают с ожидаемыми');
  assert.include(isNewMassMailOk, 'Autotest massmail', 'massmail topic differs from expected');
  assert.include(isNewMassMailOk, currentDate, 'massmail date differs from expected');
};
