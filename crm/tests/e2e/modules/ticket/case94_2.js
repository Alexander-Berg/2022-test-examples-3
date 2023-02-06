const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  await browser.linkCrmTicket('10568269');
  const linkedStTicket = await browser.$(TicketsLocators.LINKED_ST_TICKET);
  await linkedStTicket.waitForDisplayed();
  const isStTicketCreated = await linkedStTicket.getText();
  assert.include(isStTicketCreated, 'Тикет', 'linked crm ticket was not found on the page');
};
