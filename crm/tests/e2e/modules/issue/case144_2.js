const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  // receiving date two days from now
  const currentDate = new Date(new Date().getTime() + 48 * 60 * 60 * 1000);
  const month = currentDate.getMonth() + 1;
  const day = currentDate.getDate();
  const year = currentDate.getFullYear();
  const future = String(day).padStart(2, '0') + '.' + String(month).padStart(2, '0') + '.' + year;

  const createTimer = await browser.$(IssuesLocators.CREATE_TIMER);
  await createTimer.waitForClickable();
  await createTimer.click();

  const inputNotifyPeople = await browser.$(IssuesLocators.INPUT_FOR_PEOPLE_TO_NOTIFY);
  await inputNotifyPeople.waitForDisplayed();
  await inputNotifyPeople.setValue('Crmcrown Robot');

  const robotSuggest = await browser.$(IssuesLocators.ROBOT_TITLE_IN_SUGGEST);
  await robotSuggest.waitForDisplayed();
  await robotSuggest.click();

  const timerTwoDays = await browser.$(IssuesLocators.TIMER_IN_TWO_DAYS);
  await timerTwoDays.click();

  const saveTimer = await browser.$(IssuesLocators.SAVE_TIMER);
  await saveTimer.waitForClickable();
  await saveTimer.click();

  const createdTimer = await browser.$(IssuesLocators.CREATED_TIMER);
  await createdTimer.waitForClickable();
  const isTimerCreated = await createdTimer.getText();
  assert.include(isTimerCreated, future, 'time was not found on the page');
};
