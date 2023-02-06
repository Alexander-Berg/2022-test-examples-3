const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'выбрать верхнюю задачу в списке');
  const latestIssue = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await latestIssue.waitForDisplayed();
  await latestIssue.click();
  //
  await browser.setMeta('2', 'добавить наблюдателем робота Crmcrown Robot');
  await browser.addFollowerToTicket('Crmcrown Robot');
  //
  await browser.setMeta('3', 'нажать на атрибут Наблюдатели');
  const followers = await browser.$(TicketsLocators.FOLLOWERS_FIELD);
  await followers.waitForDisplayed();
  await followers.click();
  await browser.pause(1000);
  //
  await browser.setMeta('4', 'нажать крестик удаления наблюдателя');
  const deleteFollower = await browser.$(TicketsLocators.DELETE_FOLLOWER_FROM_ISSUE);
  await deleteFollower.waitForDisplayed();
  await deleteFollower.click();
  await browser.pause(1000);
  //
  await browser.setMeta('5', 'нажать Esc, чтобы выйти из режима заполнения поля');
  const followersInput = await browser.$(TicketsLocators.FOLLOWERS_INPUT);
  await followersInput.setValue('Escape');
  await browser.pause(1000);
  //
  await browser.setMeta('6', 'убедиться, что робот не отображается в атрибутах');
  const robotFollower = await browser.$(IssuesLocators.ROBOT_FOLLOWER);
  const isFollowerDeleted = await robotFollower.isExisting();
  assert.isFalse(isFollowerDeleted, 'follower was still found on the page');
};
