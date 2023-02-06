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

  //подождать, пока кнопка "Отложить" станет доступной (т.е. кнопка "В работу" полностью отработает)
  await reopenInprogressPostpone.waitForEnabled({ timeout: 3000 });

  //Взять информацию из первого тикета по списку и найти в ней слова "В работе"
  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  const isInProgress = await latestTicket.getText();

  assert.include(isInProgress, 'В работе', 'ticket is not in In Progresss status');
};
