const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');
const issueName = `e2e-issue ${Math.random()}`;
const findIssueTimeout = 30000;

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'создать новую задачу с указанным названием');
  await browser.createNewIssue(issueName);
  //
  await browser.setMeta('2', 'ввести в поле Поиск название созданной задачи и нажать кнопку Найти');
  const searchInput = await browser.$(IssuesLocators.SEARCH_ISSUE);
  await searchInput.click();
  await searchInput.setValue(issueName);
  const searchButton = await browser.$(IssuesLocators.SEARCH_BUTTON);
  await searchButton.click();

  //
  await browser.setMeta(
    '3',
    'нажимать на Найти в течение указанного таймаута findIssueTimeout, пока не появится список доступных задач',
  );
  //
  await searchButton.waitUntil(
    async () => {
      const firstIssue = await (await browser.$(IssuesLocators.LATEST_CREATED_ISSUE)).isDisplayed();
      await searchButton.click();
      return firstIssue === true;
    },
    {
      timeout: findIssueTimeout,
      interval: 3000,
    },
  );

  //
  await browser.setMeta('4', 'убедиться, что в результатах выдачи только созданная задача');
  const firstIssueInList = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await firstIssueInList.waitForClickable({ timeout: 10000, interval: 1000 });
  //
  const issues = await browser.$$(IssuesLocators.ISSUE_LIST_BLOCK);
  const issuesCount = issues.length;
  assert.strictEqual(
    issuesCount,
    1,
    'в результатах выдачи больше одной задачи, хотя должна быть только одна, т.к. название уникальное',
  );
  //
  await browser.setMeta('5', 'сравнить название первой задачи в списке с ожидаемым');
  assert.include(
    await firstIssueInList.getText(),
    issueName,
    'первая задача в списке не содержит названия искомой задачи',
  );
};
