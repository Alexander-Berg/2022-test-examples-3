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
  //нажать кнопку создания таймера в шапке тикета
  const createTimer = await browser.$(IssuesLocators.CREATE_TIMER);
  await createTimer.waitForClickable();
  await createTimer.click();
  //в поле Уведомить ввести Crmcrown Robot
  const inputNotifyPeople = await browser.$(IssuesLocators.INPUT_FOR_PEOPLE_TO_NOTIFY);
  await inputNotifyPeople.waitForClickable();
  await inputNotifyPeople.setValue('Crmcrown Robot');
  //из полученного списка выбрать этого робота
  const robotSuggest = await browser.$(IssuesLocators.ROBOT_TITLE_IN_SUGGEST);
  await robotSuggest.waitForDisplayed();
  await robotSuggest.click();
  //сохранить таймер
  const saveTimer = await browser.$(IssuesLocators.SAVE_TIMER);
  await saveTimer.waitForClickable();
  await saveTimer.click();
  await browser.pause(1000);

  //нажать кнопку таймера в атрибутах тикета
  const timerInAttributes = await browser.$(IssuesLocators.TIMER_IN_ATTRIBUTES);
  await timerInAttributes.waitForClickable();
  await timerInAttributes.click();
  //нажать на содержание таймера, откроется модалка на редактирование
  const editTimer = await browser.$(IssuesLocators.EDIT_TIMER);
  await editTimer.waitForClickable();
  await editTimer.click();
  await browser.pause(1000);
  //установить таймер на 2 дня позже
  const timerTwoDays = await browser.$(IssuesLocators.TIMER_IN_TWO_DAYS);
  await timerTwoDays.click();
  //нажать кнопку Сохранить, проверить, что кнопка после этого исчезла
  await saveTimer.waitForClickable();
  await saveTimer.click();
  await saveTimer.waitForExist({
    timeout: 5000,
    interval: 500,
    reverse: true,
  });

  await browser.pause(1000);

  const createdTimer = await browser.$(IssuesLocators.CREATED_TIMER);
  await createdTimer.waitForDisplayed();

  const isTimerCreated = await createdTimer.getText();
  assert.include(isTimerCreated, future, 'time was not found on the page');
};
