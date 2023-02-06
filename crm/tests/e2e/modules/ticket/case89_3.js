const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //
  await this.browser.setMeta('1', 'создать тикет на 2 линию');
  await browser.createSecondLineTicket('Ticket to second line created with automated test');
  //
  await this.browser.setMeta('2', 'перейти на вкладку Связанные');
  const linkedTickets = await browser.$(TicketsLocators.LINKED_TICKETS);
  await linkedTickets.waitForDisplayed();
  await linkedTickets.click();
  //
  await this.browser.setMeta('3', 'нажать на связанный тикет');
  const openSecondTicket = await browser.$(TicketsLocators.OPEN_2ND_TICKET);
  await openSecondTicket.waitForDisplayed();
  await openSecondTicket.click();
  //
  await this.browser.setMeta('4', 'взять содержимое связанного тикета');
  const content = await browser.$(TicketsLocators.CONTENT_IN_2ND_TICKET);
  await content.waitForDisplayed();
  //
  await this.browser.setMeta(
    '5',
    'проверить, что в содержимом есть текст, который вводили при создании тикета',
  );
  const does2ndLineTicketHaveCorrectContent = await content.getText();
  assert.include(
    does2ndLineTicketHaveCorrectContent,
    'Суть вопроса: Ticket to second line created with automated test',
    'ticket in second line does not have content which was submitted',
  );
};
