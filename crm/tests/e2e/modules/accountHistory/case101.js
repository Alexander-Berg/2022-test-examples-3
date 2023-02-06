const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');
const { AccountHistoryLocators } = require('./../../pages/locators/accountHistory');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

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

  await browser.pause(5000);

  //click on created issue in account history
  const testIssue = await browser.$('//div[text()="Test issue"]');
  await testIssue.waitForDisplayed();
  await testIssue.click();

  //open issue card
  const issueCard = await browser.$(AccountHistoryLocators.ISSUE_CARD);
  await issueCard.waitForDisplayed();

  //check that timer is disabled
  const createTimer = await browser.$(IssuesLocators.CREATE_TIMER);
  await createTimer.waitForEnabled({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  //check that button "Actions" is disabled
  const actionInIssue = await browser.$(IssuesLocators.ACTIONS_IN_ISSUE_DISABLED);

  await actionInIssue.waitForEnabled({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  //check that button "Close" is disabled
  const closeOpenButton = await browser.$(IssuesLocators.CLOSE_OPEN_ISSUE_BUTTON);
  const isTicketInHistory = await closeOpenButton.waitForEnabled({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  assert.isTrue(isTicketInHistory, 'issue actions were not disabled');
};
