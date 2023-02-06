const { assert } = require('chai');
const { IssuesLocators } = require('../../pages/locators/issues');
const issueName = `e2e-issue ${Math.random()}`;

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'создать новую задачу с указанным названием');
  await browser.createNewIssue(issueName);
  //
  await browser.setMeta('2', 'в задаче изменить исполнителя на robot-space-oddity');
  const assigneeChange = await browser.setAssigneeInTicket('robot-space-oddity');
  assert.strictEqual(
    assigneeChange,
    'CRM Space Oddity Robot',
    'исполнитель в задаче должен был измениться, но остался прежним',
  );
  //
  await browser.setMeta('3', 'выбрать фильтр Мои');
  const filterMy = await browser.$(IssuesLocators.FILTER_MY);
  const isFilterMySelected = await filterMy.getAttribute('checked');
  // если фильтр не выбран, то кликнуть по нему
  if (!isFilterMySelected) {
    await filterMy.waitForExist({ timeout: 10000, interval: 1000 });
    await filterMy.click();
  }
  //
  await browser.setMeta('4', 'выбрать фильтр Все');
  const filterAll = await browser.$(IssuesLocators.FILTER_ALL);
  const isFilterAllSelected = await filterAll.getAttribute('checked');
  if (!isFilterAllSelected) {
    await filterAll.waitForExist({ timeout: 10000, interval: 1000 });
    await filterAll.click();
  }
  //
  await browser.setMeta('5', 'обновить страницу');
  await browser.refresh();
  const issueListIsVisible = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await issueListIsVisible.waitForClickable();
  //
  await browser.setMeta('6', 'убедиться, что задача не отображается в видимом списке');
  const issueIsFound = await browser.findTicketInList(issueName);
  assert.isFalse(issueIsFound, 'задача не должна отображаться в выбранных фильтрах');
  //
  await browser.setMeta('7', 'выбрать фильтр Делегированы');
  const filterDelegated = await browser.$(IssuesLocators.FILTER_DELEGATED);
  const isFilterDelegatedSelected = await filterDelegated.getAttribute('checked');
  // если фильтр не выбран, то кликнуть по нему
  if (!isFilterDelegatedSelected) {
    await filterDelegated.waitForExist({ timeout: 10000, interval: 1000 });
    await filterDelegated.click();
  }
  //
  await browser.setMeta('8', 'подождать, пока список задач обновится');
  const firstIssueInList = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await firstIssueInList.waitForClickable({ timeout: 10000, interval: 1000 });
  //
  await browser.setMeta('9', 'найти задачу в видимом списке');
  const delegatedIssueIsFound = await browser.findTicketInList(issueName);
  assert.strictEqual(
    delegatedIssueIsFound,
    issueName,
    'созданная задача не отображается в выбранных фильтрах',
  );
};
