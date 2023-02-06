const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');
const issueName = `e2e-issue ${Math.random()}`;

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'создать новую задачу с указанным названием');
  await browser.createNewIssue(issueName);

  //
  await browser.setMeta('2', 'выбрать фильтр Мои');
  const filterMy = await browser.$(IssuesLocators.FILTER_MY);
  const isFilterMySelected = await filterMy.getAttribute('checked');
  // если фильтр не выбран, то кликнуть по нему
  if (!isFilterMySelected) {
    await filterMy.waitForExist({ timeout: 10000, interval: 1000 });
    await filterMy.click();
  }
  //
  await browser.setMeta('3', 'выбрать фильтр Открытые');
  const filterOpen = await browser.$(IssuesLocators.FILTER_OPEN);
  const isFilterOpenSelected = await filterOpen.getAttribute('checked');
  if (!isFilterOpenSelected) {
    await filterOpen.waitForExist({ timeout: 10000, interval: 1000 });
    await filterOpen.click();
  }
  //
  await browser.setMeta('4', 'обновить страницу');
  await browser.refresh();
  const issueListIsVisible = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await issueListIsVisible.waitForClickable();
  //
  await browser.setMeta('5', 'найти задачу в видимом списке');
  const issueIsFound = await browser.findTicketInList(issueName);
  assert.strictEqual(
    issueIsFound,
    issueName,
    'созданная задача не отображается в выбранных фильтрах',
  );
};
