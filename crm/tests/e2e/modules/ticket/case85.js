const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  await latestTicket.waitForDisplayed({ timeout: 5000 });
  const isNewTicketOpen = await latestTicket.getText();
  assert.include(isNewTicketOpen, 'Открыт', 'ticket is not in Open status');
};
