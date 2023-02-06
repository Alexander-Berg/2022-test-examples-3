const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  const latestIssue = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await latestIssue.waitForDisplayed();
  await latestIssue.doubleClick();

  const renameInput = await browser.$(IssuesLocators.INPUT_RENAME_ISSUE);
  await renameInput.waitForDisplayed();
  await renameInput.setValue([' Renamed', 'Enter']);

  await browser.pause(1000);

  const renamedTitle = await browser.$(
    './/span[text()="Issue created with automated test Renamed"]',
  );
  await renamedTitle.waitForDisplayed();

  await latestIssue.click();
  const isNewIssueRenamed = await latestIssue.getText();
  assert.include(
    isNewIssueRenamed,
    'Issue created with automated test Renamed',
    'issue was not renamed',
  );
};
