const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  await browser.linkCrmTicket('10568269');
  const unlickTicket = await browser.$(TicketsLocators.UNLINK_CRM_TICKET);
  await unlickTicket.waitForDisplayed();
  await unlickTicket.click();

  await browser.acceptAlert();
  const noLinks = await browser.$(TicketsLocators.NO_LINKS_MSG);
  const isCrmTicketUnlinked = await noLinks.waitForDisplayed();

  assert.isTrue(isCrmTicketUnlinked, 'crm ticket was not unlinked with CRM ticket');
};
