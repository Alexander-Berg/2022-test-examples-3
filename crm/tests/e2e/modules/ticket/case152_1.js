const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //добавить таймер в тикет
  await browser.addTimerToTicket();
  //навести курсор на таймер
  const createdTimer = await browser.$(TicketsLocators.CREATED_TIMER);
  await createdTimer.moveTo();
  await browser.pause(1000);
  //убедиться, что текст в комментарии соответствует ожиданию
  const commentText = await browser.$('.//div[text()="Autocomment to timer"]');
  const isTimerCreated = await commentText.waitForDisplayed();
  assert.isTrue(isTimerCreated, 'time was not found on the page');
};
