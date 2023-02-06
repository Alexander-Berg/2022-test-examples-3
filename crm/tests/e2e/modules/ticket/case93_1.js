const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  await browser.linkStartrackTicket('CRM-5318');
  const linkedStTicket = await browser.$(TicketsLocators.LINKED_ST_TICKET);
  await linkedStTicket.waitForDisplayed();
  const isStTicketCreated = await linkedStTicket.getText();

  assert.include(isStTicketCreated, 'CRM-', 'startrack ticket was not found on the page');
};
