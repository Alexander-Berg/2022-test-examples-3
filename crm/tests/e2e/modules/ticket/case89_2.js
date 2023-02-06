// этот тест отработает, если фича ifSecondLineThenLater=true
// тикеты https://st.yandex-team.ru/CRM-12181 и https://st.yandex-team.ru/CRM-18459

const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //
  await this.browser.setMeta('1', 'создать тикет на 2 линию');
  await browser.createSecondLineTicket('Ticket to second line created with automated test');
  //
  await this.browser.setMeta('2', 'перейти в верхний тикет в списке');
  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  await latestTicket.waitForDisplayed();
  //
  await this.browser.setMeta('3', 'убедиться, что статус исходного тикета изменился на Отложен');
  const isTicketPostponed = await latestTicket.getText();
  assert.include(isTicketPostponed, 'Отложен', 'time was not found on the page');
};
