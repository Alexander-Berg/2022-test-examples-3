const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');
const { ROBOT_ODYSSEY_LOGIN } = require('../../constants/robotOdysseyData');

module.exports = async function() {
  const { browser } = this;
  await browser.addMarkToTicket('Отметка для автотеста', ROBOT_ODYSSEY_LOGIN);

  const deleteMark = await browser.$(TicketsLocators.DELETE_MARK_FROM_TICKET);
  await deleteMark.waitForDisplayed({ timeout: 5000, interval: 1000 });
  await deleteMark.click();
  // подождать, пока метка исчезнет
  await deleteMark.waitForDisplayed({ timeout: 20000, interval: 1000, reverse: true });

  const isMarkDeleted = await deleteMark.isDisplayed();
  assert.isFalse(isMarkDeleted, 'newly added mark is still on the page');
};
