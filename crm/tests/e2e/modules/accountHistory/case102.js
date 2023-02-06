const { assert } = require('chai');
const { AccountHistoryLocators } = require('./../../pages/locators/accountHistory');

module.exports = async function() {
  const { browser } = this;

  const accountHistory = await browser.$(AccountHistoryLocators.ACCOUNT_HISTORY_BUTTON);
  await accountHistory.waitForClickable();
  await accountHistory.click();

  const filtersDroplist = await browser.$(AccountHistoryLocators.FILTERS_DROPLIST);
  await filtersDroplist.waitForDisplayed();
  await filtersDroplist.click();

  const body = await browser.$(AccountHistoryLocators.FILTERS_BODY);
  const isTicketInHistory = await body.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  assert.isTrue(isTicketInHistory, 'history filters were not folded');
};
