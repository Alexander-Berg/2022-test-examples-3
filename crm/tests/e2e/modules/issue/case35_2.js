const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  await browser.createLinkedIssue('One more linked issue for autotest');

  await browser.refresh();
  const latesLinkedIssue = await browser.$(IssuesLocators.LATEST_CREATED_LINKED_ISSUE);
  await latesLinkedIssue.waitForDisplayed();
  await latesLinkedIssue.click();
  const isSubIssueLinked = await latesLinkedIssue.waitForDisplayed();
  assert.isTrue(isSubIssueLinked, 'newly added subtask is not found on the page');
};
