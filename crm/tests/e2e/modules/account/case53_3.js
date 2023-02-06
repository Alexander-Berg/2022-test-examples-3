const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;

  const createAccount = await browser.$(AccountsLocators.CREATE_ACCOUNT_BUTTON);
  await createAccount.waitForDisplayed();
  await createAccount.click();

  const newAccount = await browser.$(AccountsLocators.NEW_ACCOUNT_NAME_INPUT);
  await newAccount.waitForDisplayed();
  await newAccount.setValue('Test metrica account');

  const accountTypeList = await browser.$(AccountsLocators.NEW_ACCOUNT_TYPE_LIST);
  await accountTypeList.waitForDisplayed();
  await accountTypeList.click();

  const metricaType = await browser.$(AccountsLocators.METRICA_TYPE);
  await metricaType.waitForDisplayed();
  await browser.pause(1000);
  await metricaType.click({ x: 5, y: 5 });

  const metricaLogin = await browser.$(AccountsLocators.METRICA_LOGIN_INPUT);
  await metricaLogin.waitForDisplayed();
  await metricaLogin.setValue('login');

  const saveAccount = await browser.$(AccountsLocators.SAVE_NEW_ACCOUNT_BUTTON);
  await saveAccount.waitForClickable();
  await saveAccount.click();

  await saveAccount.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  await browser.pause(1000);
  await browser.refresh();

  const accountInfo = await browser.$(AccountsLocators.ACCOUNT_INFO_BLOCK);
  await accountInfo.waitForDisplayed();
  const isAccountCreated = await accountInfo.getText();
  const infoBlock = isAccountCreated;
  assert.include(infoBlock, 'ТИП: Клиент Метрики', 'account was not created');
  assert.include(infoBlock, 'Test metrica account', 'account was not created');
};
