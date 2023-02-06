const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  await browser.createLinkedIssue('Yet another Linked Issue for autotest');

  const latestLinkedIssue = await browser.$(IssuesLocators.LATEST_CREATED_LINKED_ISSUE);
  await latestLinkedIssue.waitForDisplayed();
  const isSubIssueLinked = await latestLinkedIssue.getText();
  assert.include(
    isSubIssueLinked,
    'Issue created with automated test',
    'link to parent issue is not found on subtask',
  );
};
