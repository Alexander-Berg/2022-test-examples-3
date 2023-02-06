const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  const closeOpenButton = await browser.$(IssuesLocators.CLOSE_OPEN_ISSUE_BUTTON);
  await closeOpenButton.waitForDisplayed();
  await closeOpenButton.click();

  const closedOpenIconChecked = await browser.$(IssuesLocators.CLOSE_OPEN_ISSUE_CIRCLE_CHECKED);
  const isOpen = await closedOpenIconChecked.waitForDisplayed();

  const latesIssue = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  const isNewIssueClosed = await latesIssue.getText();
  assert.include(isNewIssueClosed, 'Закрыта', 'issue was not closed');
  assert.isTrue(isOpen, 'circle is not checked when we closed the issue');
};
