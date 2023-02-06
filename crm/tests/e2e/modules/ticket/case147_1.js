const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //перейти в последний созданный тикет
  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  await latestTicket.click();
  // добавить наблюдателем робота Crmcrown Robot
  await browser.addFollowerToTicket('robot-crmcrown');

  await browser.pause(2000);

  const robotFollower = await browser.$(TicketsLocators.ROBOT_FOLLOWER);
  await robotFollower.waitForDisplayed({ timeout: 5000, interval: 1000 });
  assert.isTrue(await robotFollower.isDisplayed(), 'follower was not found on the page');
};
