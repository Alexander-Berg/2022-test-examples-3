const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  // receiving date two days from now
  const currentDate = new Date(new Date().getTime() + 48 * 60 * 60 * 1000);
  const month = currentDate.getMonth() + 1;
  const day = currentDate.getDate();
  const year = currentDate.getFullYear();
  const future = String(day).padStart(2, '0') + '.' + String(month).padStart(2, '0') + '.' + year;

  const createTimer = await browser.$(TicketsLocators.CREATE_TIMER);
  await createTimer.waitForDisplayed();
  await createTimer.click();

  const peopleNotify = await browser.$(TicketsLocators.INPUT_FOR_PEOPLE_TO_NOTIFY);
  await peopleNotify.waitForDisplayed();
  await peopleNotify.setValue('Crmcrown Robot');

  const robotSuggest = await browser.$(TicketsLocators.ROBOT_TITLE_IN_SUGGEST);
  await robotSuggest.waitForDisplayed();
  await robotSuggest.click();

  const timerInTwoDays = await browser.$(TicketsLocators.TIMER_IN_TWO_DAYS);
  await timerInTwoDays.click();

  const saveTimer = await browser.$(TicketsLocators.SAVE_TIMER);
  await saveTimer.waitForClickable();
  await saveTimer.click();

  await browser.pause(2000);

  const createdTimer = await browser.$(TicketsLocators.CREATED_TIMER);
  const isTimerCreated = await createdTimer.getText();
  assert.include(isTimerCreated, future, 'time was not found on the page');
};
