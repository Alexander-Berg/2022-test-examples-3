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

  //добавить таймер в тикет
  await browser.addTimerToTicket();
  //увидеть, что иконка таймера изменилась
  const createdTimer = await browser.$(TicketsLocators.CREATED_TIMER);
  await createdTimer.waitForDisplayed();
  //кликнуть на таймер в атрибутах
  const timerInAttributes = await browser.$(TicketsLocators.TIMER_IN_ATTRIBUTES);
  await timerInAttributes.waitForDisplayed();
  await timerInAttributes.click();
  //нажать на само значение таймера, откроется модалка редактирования
  const editTimer = await browser.$(TicketsLocators.EDIT_TIMER);
  await editTimer.waitForDisplayed();
  await editTimer.click();
  //нажать кнопку "Через 2 дня"
  const timerInTwoDays = await browser.$(TicketsLocators.TIMER_IN_TWO_DAYS);
  await timerInTwoDays.click();
  //нажать кнопку Сохранить
  const saveTimer = await browser.$(TicketsLocators.SAVE_TIMER);
  await saveTimer.waitForEnabled();
  await saveTimer.click();
  //дождаться сохранения
  await browser.pause(2000);
  //проверить, что кнопка сохранения скрылась
  await saveTimer.waitForExist({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  //забрать текст из таймера в шапке тикета
  const isTimerCreated = await createdTimer.getText();
  //увидеть, что в нем дата через 2 дня
  assert.include(isTimerCreated, future, 'time was not found on the page');
};
