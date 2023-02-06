const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'нажать кнопку "Создать аккаунт"');
  const createAccount = await browser.$(AccountsLocators.CREATE_ACCOUNT_BUTTON);
  await createAccount.waitForDisplayed();
  await createAccount.click();
  //
  await browser.setMeta('2', 'в поле Название ввести значение "Test lead account"');
  const newAccount = await browser.$(AccountsLocators.NEW_ACCOUNT_NAME_INPUT);
  await newAccount.waitForDisplayed();
  await newAccount.setValue('Test lead account');
  //
  await browser.setMeta('3', 'нажать на "Тип аккаунта"');
  const accountTypeList = await browser.$(AccountsLocators.NEW_ACCOUNT_TYPE_LIST);
  await accountTypeList.waitForDisplayed();
  await accountTypeList.click();
  //
  await browser.setMeta('4', 'выбрать тип Лид');
  const leadType = await browser.$(AccountsLocators.LEAD_TYPE);
  await leadType.waitForDisplayed();
  await browser.pause(1000);
  await leadType.click();
  //
  await browser.setMeta('5', 'нажать кнопку Сохранить');
  const saveAccount = await browser.$(AccountsLocators.SAVE_NEW_ACCOUNT_BUTTON);
  await saveAccount.waitForDisplayed();
  await saveAccount.click();
  //
  await browser.setMeta(
    '6',
    'дождаться, пока форма исчезнет и отобразится карточка созданного аккаунта',
  );
  await saveAccount.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  await browser.pause(1000);
  await browser.refresh();
  //
  await browser.setMeta('7', 'дождаться отображения блока информации об аккаунте');
  const accountInfo = await browser.$(AccountsLocators.ACCOUNT_INFO_BLOCK);
  await accountInfo.waitForDisplayed();
  //
  await browser.setMeta('8', 'нажать кнопку редактирования этого блока');
  const editAccount = await browser.$(AccountsLocators.EDIT_ACCOUNT_BUTTON);
  await editAccount.waitForDisplayed();
  await editAccount.click();
  //
  await browser.setMeta('9', 'отредактировать название аккаунта');
  const inputAccountName = await browser.$(AccountsLocators.ACCOUNT_NAME_INPUT_EDIT);
  await inputAccountName.waitForDisplayed();
  await inputAccountName.setValue('Edited account name');
  //
  await browser.setMeta('10', 'добавить значение в поле Домен');
  const domainInput = await browser.$(AccountsLocators.DOMAIN_INPUT);
  await domainInput.waitForDisplayed();
  await domainInput.setValue('.domain');
  //
  await browser.setMeta(
    '11',
    'проверить, что поле Индустрия не показывается (д.б. только у контрагентов)',
  );
  const selectIndustry = await browser.$(AccountsLocators.INDUSTRY_FIELD);
  assert.isFalse(await selectIndustry.isDisplayed(), 'industry is visible on page');
  //
  await browser.setMeta('12', 'нажать кнопку Сохранить');
  const saveEdits = await browser.$(AccountsLocators.SAVE_ACCOUNT_EDITS);
  await saveEdits.waitForClickable();
  await saveEdits.click();

  await browser.pause(2000);
  //
  await browser.setMeta('13', 'найти значения измененных полей');
  await editAccount.waitForDisplayed();
  await accountInfo.waitForDisplayed();
  const isAccountEdited = await accountInfo.getText();
  const infoBlock = isAccountEdited;
  //
  await browser.setMeta('14', 'сравнить их с ожидаемыми результатами');
  assert.include(infoBlock, 'Edited account name', 'name was not edited');
  assert.include(infoBlock, '.domain', 'domain was not edited');
};
