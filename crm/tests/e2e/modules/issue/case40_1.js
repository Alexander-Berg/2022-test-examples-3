const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');
const markName = `mark ${Math.random() * 1000}`;

module.exports = async function() {
  const { browser } = this;

  await browser.addMarkToIssue(markName);
  const createdMark = await browser.$(IssuesLocators.DELETE_MARK_FROM_ISSUE);
  const isMarkCreated = await createdMark.waitForDisplayed();
  assert.isTrue(isMarkCreated, 'newly added mark is not found in personal marks');
};
