const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //нажать Закрыть
  const closeTicket = await browser.$(TicketsLocators.CLOSE_TICKET);
  await closeTicket.waitForDisplayed();
  await closeTicket.click();

  //выбрать резолюцию Решен
  const resolved = await browser.$(TicketsLocators.RESOLUTION_RESOLVED);
  await resolved.waitForDisplayed();
  await resolved.click();

  //нажать Открыть
  const reopenInprogressPostpone = await browser.$(
    TicketsLocators.REOPEN_INPROGRESS_POSTPONE_TICKET,
  );
  await reopenInprogressPostpone.waitForClickable({ timeout: 20000, interval: 1000 });
  await reopenInprogressPostpone.click();

  //дождаться, пока кнопка Закрыть станет кликабельной
  await closeTicket.waitForClickable({ timeout: 5000, interval: 500 });

  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  const isReopened = await latestTicket.getText();
  assert.include(isReopened, 'Открыт', 'ticket is not in Open status');
};
