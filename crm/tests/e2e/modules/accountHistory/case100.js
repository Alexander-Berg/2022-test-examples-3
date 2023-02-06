const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');
const { AccountHistoryLocators } = require('./../../pages/locators/accountHistory');

module.exports = async function() {
  const { browser } = this;

  // receiving today's date
  const currentDate = new Date(new Date().getTime());
  const month = currentDate.getMonth() + 1;
  const day = currentDate.getDate();
  const year = currentDate.getFullYear();
  const today = String(day).padStart(2, '0') + '.' + String(month).padStart(2, '0') + '.' + year;

  //create a new issue in Account-card
  const newIssueName = await browser.$(AccountsLocators.NEW_ISSUE_INPUT);
  await newIssueName.waitForDisplayed();
  await newIssueName.setValue(['Test issue', 'Enter']);

  await browser.pause(1000);

  await browser.refresh();
  await newIssueName.waitForDisplayed();

  //click on button "Account history"
  const accountHistory = await browser.$(AccountHistoryLocators.ACCOUNT_HISTORY_BUTTON);
  await accountHistory.waitForClickable();
  await accountHistory.click();

  //waiting for activity list
  const historyWindow = await browser.$(AccountHistoryLocators.HISTORY_WINDOW);
  await historyWindow.waitForDisplayed();
  //pause is needed here to load history window content
  await browser.pause(1000);
  const isTicketInHistory = await historyWindow.getText();

  assert.include(isTicketInHistory, 'Задача', 'issue is not in history');
  assert.include(isTicketInHistory, 'Test issue', 'issue title is not in history');
  assert.include(isTicketInHistory, 'CRM Space Odyssey Robot', 'issue creator is not in history');
  assert.include(isTicketInHistory, 'Открыта', 'issue status is not in history');
  assert.include(isTicketInHistory, today, 'issue date is not in history');
};
