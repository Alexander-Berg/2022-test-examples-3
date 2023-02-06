const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  const createTimer = await browser.$(IssuesLocators.CREATE_TIMER);
  await createTimer.waitForDisplayed();
  await createTimer.click();

  const inputNotifyPeople = await browser.$(IssuesLocators.INPUT_FOR_PEOPLE_TO_NOTIFY);
  await inputNotifyPeople.waitForDisplayed();
  await inputNotifyPeople.setValue('Crmcrown Robot');

  const robotSuggest = await browser.$(IssuesLocators.ROBOT_TITLE_IN_SUGGEST);
  await robotSuggest.waitForDisplayed();
  await robotSuggest.click();

  const comment = await browser.$('.//span[text()="Комментарий"]');
  await comment.click();

  const timerComment = await browser.$(IssuesLocators.TIMER_COMMENT);
  await timerComment.setValue('Autocomment to timer');

  const saveTimer = await browser.$(IssuesLocators.SAVE_TIMER);
  await saveTimer.waitForClickable();
  await saveTimer.click();

  const createdTimer = await browser.$(IssuesLocators.CREATED_TIMER);
  const isTimerCreated = await createdTimer.waitForClickable();

  assert.isTrue(isTimerCreated, 'time was not found on the page');
};
