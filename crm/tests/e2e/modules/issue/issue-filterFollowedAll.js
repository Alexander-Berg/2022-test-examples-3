const { assert } = require('chai');
const { IssuesLocators } = require('../../pages/locators/issues');
const issueName = `e2e-issue ${Math.random()}`;

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'создать новую задачу с указанным названием');
  await browser.createNewIssue(issueName);

  //
  await browser.setMeta('2', 'выбрать фильтр Наблюдаю');
  const filterFollow = await browser.$(IssuesLocators.FILTER_FOLLOWER);
  const isFilterFollowSelected = await filterFollow.getAttribute('checked');
  // если фильтр не выбран, то кликнуть по нему
  if (!isFilterFollowSelected) {
    await filterFollow.waitForExist({ timeout: 10000, interval: 1000 });
    await filterFollow.click();
  }
  //
  await browser.setMeta('3', 'выбрать фильтр Все');
  const filterAll = await browser.$(IssuesLocators.FILTER_ALL);
  const isFilterAllSelected = await filterAll.getAttribute('checked');
  if (!isFilterAllSelected) {
    await filterAll.waitForExist({ timeout: 10000, interval: 1000 });
    await filterAll.click();
  }
  //
  await browser.setMeta('4', 'обновить страницу');
  await browser.refresh();
  const issueListIsVisible = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await issueListIsVisible.waitForClickable();
  //
  await browser.setMeta('5', 'задача не отображается в выбранных фильтрах');
  const issueIsFound = await browser.findTicketInList(issueName);
  assert.isFalse(
    issueIsFound,
    'задачи не должно быть в выбранных фильтрах, но она в них отображается',
  );
  //
  await browser.setMeta('6', 'добавить в наблюдатели робота robot-space-odyssey');
  await browser.addFollowerToTicket('robot-space-odyssey');
  //
  await browser.setMeta('7', 'выбрать фильтр Мои');
  const filterMy = await browser.$(IssuesLocators.FILTER_MY);
  const isFilterMySelected = await filterMy.getAttribute('checked');
  // если фильтр не выбран, то кликнуть по нему
  if (!isFilterMySelected) {
    await filterMy.waitForExist({ timeout: 10000, interval: 1000 });
    await filterMy.click();
  }
  //
  await browser.setMeta('8', 'обновить страницу');
  await browser.refresh();
  await issueListIsVisible.waitForClickable();
  //
  await browser.setMeta('9', 'найти задачу в видимом списке');
  assert.strictEqual(
    await browser.findTicketInList(issueName),
    issueName,
    'созданной задачи нет в выбранных фильтрах, хотя она должна в них отображаться',
  );
  //
  await browser.setMeta('10', 'выбрать фильтр Наблюдаю');
  await (await browser.$(IssuesLocators.FILTER_FOLLOWER)).click();
  //
  await browser.setMeta('11', 'обновить страницу');
  await browser.refresh();
  await issueListIsVisible.waitForClickable();
  //
  await browser.setMeta('12', 'найти задачу в видимом списке');
  assert.strictEqual(
    await browser.findTicketInList(issueName),
    issueName,
    'созданной задачи нет в выбранных фильтрах, хотя она должна в них отображаться',
  );
};
