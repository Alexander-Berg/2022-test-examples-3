const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //перейти в последнюю созданную задачу
  const latestIssue = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await latestIssue.waitForDisplayed();
  await latestIssue.click();
  //нажать на Наблюдатели
  const followers = await browser.$(TicketsLocators.FOLLOWERS_FIELD);
  await followers.waitForDisplayed();
  await followers.click();
  //ввести в поле поиска "Crmcrown Robot"
  const followersInput = await browser.$(TicketsLocators.FOLLOWERS_INPUT);
  await followersInput.waitForDisplayed();
  await followersInput.setValue('Crmcrown Robot');
  await browser.pause(1000);
  //дождаться, пока в саджесте отобразится найденный робот, и выбрать его
  const robotSuggest = await browser.$(IssuesLocators.ROBOT_TITLE_IN_SUGGEST);
  await robotSuggest.waitForDisplayed({ timeout: 5000, interval: 500 });
  await robotSuggest.click();
  await browser.pause(1000);
  //нажать Esc, чтобы выйти из режима заполнения поля
  await followersInput.setValue('Escape');
  await browser.pause(1000);

  //найти аватарку робота в Наблюдателях
  const robotFollower = await browser.$(IssuesLocators.ROBOT_FOLLOWER);
  const isFollowerAdded = await robotFollower.waitForDisplayed();
  assert.isTrue(isFollowerAdded, 'follower was not found on the page');
};
