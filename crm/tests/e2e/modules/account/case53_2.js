const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;

  //нажать кнопку "Создать аккаунт"
  const createAccount = await browser.$(AccountsLocators.CREATE_ACCOUNT_BUTTON);
  await createAccount.waitForDisplayed();
  await createAccount.click();
  //ввести название "Test counteragent account"
  const newAccount = await browser.$(AccountsLocators.NEW_ACCOUNT_NAME_INPUT);
  await newAccount.waitForDisplayed();
  await newAccount.setValue('Test counteragent account');
  //нажать на список доступных типов аккаунта
  const accountTypeList = await browser.$(AccountsLocators.NEW_ACCOUNT_TYPE_LIST);
  await accountTypeList.waitForDisplayed();
  await accountTypeList.click();
  //выбрать тип "Контрагент"
  const counterAgentType = await browser.$(AccountsLocators.COUNTERAGENT_TYPE);
  await counterAgentType.waitForDisplayed();
  await counterAgentType.click({ x: 5, y: 5 });
  //дождаться появления и активности поля "Организация"
  const counterAgentOrg = await browser.$(AccountsLocators.COUNTERAGENT_ORG);
  await counterAgentOrg.waitForEnabled({
    timeout: 10000,
    interval: 500,
  });
  await counterAgentOrg.click();
  //выбрать первую строчку в списке доступных организаций
  const orgOption = await browser.$(AccountsLocators.ORG_OPTION);
  await orgOption.waitForDisplayed();
  await orgOption.click({ x: 5, y: 5 });
  await orgOption.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

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
  assert.include(infoBlock, 'ТИП: Контрагент', 'type is not counteragent');
  assert.include(infoBlock, 'Test counteragent account', 'account was not created');
};
