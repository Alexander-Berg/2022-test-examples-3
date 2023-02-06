const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  await browser.linkStartrackTicket('CRM-5318');

  const linkedTickets = await browser.$(TicketsLocators.LINKED_TICKETS);
  await linkedTickets.waitForDisplayed();
  await linkedTickets.click();

  const linkStartrack = await browser.$(TicketsLocators.UNLINK_ST_TICKET);
  await linkStartrack.waitForDisplayed();
  await linkStartrack.click();
  await browser.acceptAlert();

  const noLinks = await browser.$(TicketsLocators.NO_LINKS_MSG);
  const isStTicketUnlinked = await noLinks.waitForDisplayed();
  assert.isTrue(isStTicketUnlinked, 'startrack ticket was not unlinked with CRM ticket');
};
