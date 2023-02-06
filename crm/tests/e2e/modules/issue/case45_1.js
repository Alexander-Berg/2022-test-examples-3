const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  const calendarIcon = await browser.$(IssuesLocators.CALENDAR_ICON);
  await calendarIcon.waitForDisplayed();
  await calendarIcon.click();

  const deadlineInput = await browser.$(IssuesLocators.DEADLINE_CALENDER);
  await deadlineInput.waitForDisplayed();

  const saveDeadline = await browser.$(IssuesLocators.SAVE_DEADLINE_BUTTON);
  await saveDeadline.click();

  const tasks = await browser.$('a[title="Задачи"]');
  await tasks.click();

  const today = await browser.$('.//span[text()="Сегодня"]');
  await today.click();
  await browser.refresh();

  const inputNewIssue = await browser.$(IssuesLocators.INPUT_NEW_ISSUE);
  await inputNewIssue.waitForDisplayed();

  let latesIssue = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  const isIssueFiltededByDeadline = await latesIssue.waitForDisplayed();
  assert.isTrue(isIssueFiltededByDeadline, 'issue was not filteded by deadline');
};
