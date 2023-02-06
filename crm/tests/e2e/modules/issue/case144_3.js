const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  const createTimer = await browser.$(IssuesLocators.CREATE_TIMER);
  await createTimer.waitForClickable();
  await createTimer.click();

  const inputNotifyPeople = await browser.$(IssuesLocators.INPUT_FOR_PEOPLE_TO_NOTIFY);
  await inputNotifyPeople.waitForClickable();
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
  await createdTimer.waitForClickable();

  const timerInAttributes = await browser.$(IssuesLocators.TIMER_IN_ATTRIBUTES);
  await timerInAttributes.waitForClickable();
  await timerInAttributes.click();

  const removeTimer = await browser.$(IssuesLocators.REMOVE_TIMER);
  await removeTimer.waitForClickable();
  await removeTimer.click();

  const isTimerCreated = await createdTimer.waitForExist({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  assert.isTrue(isTimerCreated, 'time was still found on the page');
};
