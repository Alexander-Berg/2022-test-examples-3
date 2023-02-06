const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'в шапке задачи нажать на кнопку редактирования аккаунта');
  const selectAccount = await browser.$(IssuesLocators.SELECT_ACCOUNT_BUTTON);
  await selectAccount.waitForDisplayed();
  await selectAccount.click();
  //
  await browser.setMeta('2', 'выбрать чекбокс Нулевой аккаунт');
  const zeroAccount = await browser.$(IssuesLocators.ZERO_ACCOUNT_CHECKBOX);
  await zeroAccount.waitForDisplayed();
  await zeroAccount.click();
  //
  await browser.setMeta('3', 'нажать кнопку Сохранить');
  const saveAccount = await browser.$(IssuesLocators.SAVE_ACCOUNT_BUTTON);
  await saveAccount.waitForClickable();
  await saveAccount.click();
  //
  await browser.setMeta('4', 'обновить страницу');
  await browser.refresh();
  //
  await browser.setMeta('5', 'удалить аккаунт кнопкой в шапке задачи');
  const removeAccount = await browser.$(IssuesLocators.REMOVE_ACCOUNT);
  await removeAccount.waitForDisplayed();
  await removeAccount.click();
  await browser.pause(1000);
  //
  await browser.setMeta('6', 'убедиться, что ссылка на аккаунт в шапке исчезла');
  const accountLink = await browser.$(IssuesLocators.LINK_TO_ACCOUNT);
  const isAccountDeleted = await accountLink.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  assert.isTrue(isAccountDeleted, 'account was still found on the first issue');
};
