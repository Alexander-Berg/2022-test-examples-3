const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //перейти в последний созданный тикет
  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  await latestTicket.click();
  //добавить наблюдателя Crmcrown Robot в тикет
  await browser.addFollowerToTicket('robot-crmcrown');
  //нажать на поле Наблюдатели (пауза в конце нужна для отрисовки списка)
  const followersField = await browser.$(TicketsLocators.FOLLOWERS_FIELD);
  await followersField.click();
  await browser.pause(1000);
  //нажать на крестик удаления наблюдателя (пауза в конце нужна)
  const deleteFollower = await browser.$(TicketsLocators.DELETE_FOLLOWER_FROM_ISSUE);
  await deleteFollower.waitForDisplayed({ timeout: 10000, interval: 500 });
  await deleteFollower.click();
  await browser.pause(1000);
  //выйти из режима редактирования поля (пауза нужна для сохранения изменений)
  const followersInput = await browser.$(TicketsLocators.FOLLOWERS_INPUT);
  await followersInput.setValue('Escape');
  // обновить страницу
  await browser.refresh();
  await latestTicket.waitForDisplayed({ timeout: 5000, interval: 500 });

  const robotFollower = await browser.$(TicketsLocators.ROBOT_FOLLOWER);
  assert.isFalse(await robotFollower.isDisplayed(), 'follower was still found on the page');
};
