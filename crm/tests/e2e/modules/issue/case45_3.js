const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  //устанавливаем дедлайн в задаче
  await browser.setDeadlineInIssues();
  //у первой задачи в списке в кратком описании находим иконку календарика, кликаем
  const deadlineOnFirstIssue = await browser.$(IssuesLocators.DEADLINE_ON_FIRST_ISSUE);
  await deadlineOnFirstIssue.waitForDisplayed();
  await deadlineOnFirstIssue.click();
  //нажимаем кнопку очистки дедлайна
  const clearDeadline = await browser.$(IssuesLocators.CLEAR_DEADLINE_BUTTON);
  await clearDeadline.waitForDisplayed();
  await clearDeadline.click();
  await clearDeadline.waitForDisplayed({
    timeout: 5000,
    interval: 500,
    reverse: true,
  });
  //проверяем, что иконка календарика снова появилась
  const calendarIcon = await browser.$(IssuesLocators.CALENDAR_ICON);
  const isDeadlineCleared = await calendarIcon.isExisting();

  assert.isTrue(isDeadlineCleared, 'deadline was not cleared');
};
