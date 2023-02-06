const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //предварительно создан новый тикет в статусе Открыт

  //нажать кнопку "В работу"
  const reopenInprogressPostpone = await browser.$(
    TicketsLocators.REOPEN_INPROGRESS_POSTPONE_TICKET,
  );
  await reopenInprogressPostpone.waitForDisplayed();
  await reopenInprogressPostpone.click();

  //обновить страницу
  await browser.refresh();

  //нажать кнопку "Отложить"
  await reopenInprogressPostpone.waitForDisplayed();
  await reopenInprogressPostpone.click();

  //подождать, пока кнопка "Открыть" станет доступной
  await reopenInprogressPostpone.waitForEnabled({ timeout: 3000 });

  //Взять информацию из первого тикета по списку и найти в ней слово Отложен
  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  const isPostponed = await latestTicket.getText();

  assert.include(isPostponed, 'Отложен', 'ticket is not in Postponed status');
};
