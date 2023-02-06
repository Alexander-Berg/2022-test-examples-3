const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //
  await this.browser.setMeta('1', 'создать тикет на 2 линию');
  await browser.createSecondLineTicket('Ticket to second line created with automated test');
  //
  await this.browser.setMeta('2', 'перейти на вкладку Связанные');
  const linkedTicket = await browser.$(TicketsLocators.LINKED_TICKETS);
  await linkedTicket.waitForDisplayed();
  await linkedTicket.click();

  await browser.pause(2000);
  //
  await this.browser.setMeta('3', 'увидеть блок Заблокирован');
  const blocked = await browser.$(TicketsLocators.BLOCKED_BY_2ND_TICKET);
  //
  await this.browser.setMeta('4', 'проверить, что текст в блоке именно "Заблокирован"');
  await blocked.waitForDisplayed();
  const is2ndLineABlocker = await blocked.getText();
  assert.include(is2ndLineABlocker, 'Заблокирован', 'ticket to second line was not created');
};
