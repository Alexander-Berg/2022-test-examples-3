const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  await browser.createLinkedIssue('Yet another Issue to link for autotest');

  const linkToParent = await browser.$(IssuesLocators.LINK_TO_PARENT_ISSUE);
  await linkToParent.waitForDisplayed();
  await browser.refresh();

  const linkedIssues = await browser.$(IssuesLocators.LINKED_ISSUES);

  await linkedIssues.waitForDisplayed();
  await linkedIssues.click();

  const unkinkIssues = await browser.$(IssuesLocators.UNLINK_ISSUES);
  await unkinkIssues.waitForEnabled();
  await unkinkIssues.click();
  await browser.acceptAlert();
  await browser.refresh();

  const latesLinkedIssue = await browser.$(IssuesLocators.LATEST_CREATED_LINKED_ISSUE);
  const isSubIssueUnLinked = await latesLinkedIssue.isExisting();
  assert.isFalse(
    isSubIssueUnLinked,
    'link to parent issue is still on subtask after we unlinked them',
  );
};
