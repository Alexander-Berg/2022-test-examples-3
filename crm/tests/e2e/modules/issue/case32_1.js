const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  //тест нужно переписать:
  //задачу нужно создавать с уникальным названием
  //искать задачу нужно по этому названию,
  //а не просто переходить в последнюю созданную

  const latestIssue = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await latestIssue.waitForDisplayed();
  const isNewIssueOpen = await latestIssue.getText();
  assert.include(isNewIssueOpen, 'Открыта', 'issue is not in Open status');
};
