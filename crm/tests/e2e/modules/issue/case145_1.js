const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  //click on Account in Attributes
  const selectAccount = await browser.$(IssuesLocators.SELECT_ACCOUNT_BUTTON);
  await selectAccount.waitForDisplayed();
  await selectAccount.click();

  //mark checkbox for Null-Account
  const zeroAccount = await browser.$(IssuesLocators.ZERO_ACCOUNT_CHECKBOX);
  await zeroAccount.waitForDisplayed();
  await zeroAccount.click();

  //click button Choose and refresh page
  const saveAccount = await browser.$(IssuesLocators.SAVE_ACCOUNT_BUTTON);
  await saveAccount.waitForClickable();
  await saveAccount.click();
  await browser.refresh();

  const link = await browser.$(IssuesLocators.LINK_TO_ACCOUNT);
  const isAccountAdded = await link.waitForDisplayed();
  assert.isTrue(isAccountAdded, 'account was not found on the first issue');
};
