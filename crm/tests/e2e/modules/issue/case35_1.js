const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  await browser.createLinkedIssue('Linked Issue for autotest');

  const latesLinkedIssue = await browser.$(IssuesLocators.LATEST_CREATED_LINKED_ISSUE);
  await latesLinkedIssue.waitForDisplayed();
  const isSubIssueCreated = await latesLinkedIssue.getText();
  assert.include(
    isSubIssueCreated,
    'Linked Issue for autotest',
    'newly added subtask is not found on the page',
  );
};
