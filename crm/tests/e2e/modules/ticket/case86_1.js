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

  //дождаться, пока кнопка Открыть станет кликабельной
  const reopenInprogressPostpone = await browser.$(
    TicketsLocators.REOPEN_INPROGRESS_POSTPONE_TICKET,
  );
  await reopenInprogressPostpone.waitForClickable({ timeout: 10000, interval: 500 });

  //взять статус тикета из первого тикета в списке, он должен быть равен "Закрыт (Решен)"
  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  const isClosed = await latestTicket.getText();

  assert.include(isClosed, 'Закрыт (Решен)', 'ticket is not in Closed status');
};
