const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //добавить таймер в тикет
  await browser.addTimerToTicket();
  //увидеть, что иконка таймера обновилась
  const createdTimer = await browser.$(TicketsLocators.CREATED_TIMER);
  await createdTimer.waitForDisplayed({ timeout: 5000 });
  //кликнуть на таймер в атрибутах
  const timerInAttributes = await browser.$(TicketsLocators.TIMER_IN_ATTRIBUTES);
  await timerInAttributes.waitForDisplayed({ timeout: 5000 });
  await timerInAttributes.click();
  //нажать крестик удаления таймера
  const removeTimer = await browser.$(TicketsLocators.REMOVE_TIMER);
  await removeTimer.waitForDisplayed({ timeout: 5000 });
  await removeTimer.click();

  await browser.refresh();
  //убедиться, что таймер удалился
  const isTimerCreated = await createdTimer.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  assert.isTrue(isTimerCreated, 'time was still found on the page');
};
