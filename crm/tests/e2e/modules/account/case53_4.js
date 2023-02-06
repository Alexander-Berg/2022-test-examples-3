const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;

  const createAccount = await browser.$(AccountsLocators.CREATE_ACCOUNT_BUTTON);
  await createAccount.waitForDisplayed({ timeout: 5000, interval: 500 });
  await createAccount.click();

  const newAccount = await browser.$(AccountsLocators.NEW_ACCOUNT_NAME_INPUT);
  await newAccount.waitForDisplayed({ timeout: 5000, interval: 500 });
  await newAccount.setValue('Test metrica account');

  const accountTypeList = await browser.$(AccountsLocators.NEW_ACCOUNT_TYPE_LIST);
  await accountTypeList.waitForDisplayed({ timeout: 5000, interval: 500 });
  await accountTypeList.click();

  const mobileApp = await browser.$(AccountsLocators.MOBILE_APP_TYPE);
  await mobileApp.waitForDisplayed({ timeout: 5000, interval: 500 });
  await browser.pause(1000);
  await mobileApp.click({ x: 5, y: 5 });

  const saveAccount = await browser.$(AccountsLocators.SAVE_NEW_ACCOUNT_BUTTON);
  await saveAccount.waitForClickable({ timeout: 5000, interval: 500 });
  await saveAccount.click();

  await saveAccount.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  await browser.refresh();

  const accountInfo = await browser.$(AccountsLocators.ACCOUNT_INFO_BLOCK);
  await accountInfo.waitForDisplayed({ timeout: 5000, interval: 500 });
  const infoBlock = await accountInfo.getText();

  assert.include(infoBlock, 'ТИП:\nРедактировать', 'account was not created');
};
