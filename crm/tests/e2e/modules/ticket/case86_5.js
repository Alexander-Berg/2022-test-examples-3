const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //нажать на кнопку Закрыть
  const closeTicket = await browser.$(TicketsLocators.CLOSE_TICKET);
  await closeTicket.waitForDisplayed();
  await closeTicket.click();
  //выбрать резолюцию Спам
  const spam = await browser.$(TicketsLocators.RESOLUTION_SPAM);
  await spam.waitForDisplayed();
  await spam.click();

  await closeTicket.waitForExist({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  const isSpammed = await latestTicket.getText();

  assert.include(isSpammed, 'Закрыт (Спам)', 'ticket is not in Closed status');
};
