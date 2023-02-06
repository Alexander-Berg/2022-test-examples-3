const { assert } = require('chai');
const { MassmailLocators } = require('./../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  //
  await browser.setMeta('1', 'нажать кнопку Макросы');
  const macrosList = await browser.$(MassmailLocators.MACROS_LIST_BUTTON);

  //
  await browser.setMeta('2', 'добавить макрос {{Contact.FirstName}}');
  await macrosList.waitForDisplayed();
  await macrosList.click();
  const macrosFirst = await browser.$(MassmailLocators.MACROS_FIRST_NAME);
  await macrosFirst.waitForDisplayed();
  await macrosFirst.click();

  //
  await browser.setMeta('3', 'добавить макрос {{Contact.LastName}}');
  await macrosList.waitForDisplayed();
  await macrosList.click();
  const macrosLast = await browser.$(MassmailLocators.MACROS_LAST_NAME);
  await macrosLast.waitForDisplayed();
  await macrosLast.click();

  //
  await browser.setMeta('4', 'добавить макрос {{Contact.MiddleName}}');
  await macrosList.waitForDisplayed();
  await macrosList.click();
  const macrosMiddle = await browser.$(MassmailLocators.MACROS_MIDDLE_NAME);
  await macrosMiddle.waitForDisplayed();
  await macrosMiddle.click();

  //
  await browser.setMeta('5', 'добавить макрос {{Contact.AccountName}}');
  await macrosList.waitForDisplayed();
  await macrosList.click();
  const macrosAccount = await browser.$(MassmailLocators.MACROS_ACCOUNT_NAME);
  await macrosAccount.waitForDisplayed();
  await macrosAccount.click();

  //
  await browser.setMeta('6', 'увидеть, что отображается тело письма');
  const mailBody = await browser.$(MassmailLocators.MAIL_BODY);
  await mailBody.waitForDisplayed();

  //
  await browser.setMeta('7', 'в теле письма отображаются добавленные макросы');
  const isMacrosUsed = await mailBody.getText();
  assert.include(isMacrosUsed, '{{Contact.MiddleName}}', 'middle name macros is not used');
  assert.include(isMacrosUsed, '{{Contact.FirstName}}', 'first name macros is not used');
  assert.include(isMacrosUsed, '{{Contact.LastName}}', 'last name macros is not used');
  assert.include(isMacrosUsed, '{{Contact.AccountName}}', 'account name macros is not used');
};
