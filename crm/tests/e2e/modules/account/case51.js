const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');
const { IssuesLocators } = require('./../../pages/locators/issues');
const issueName = `case51 test... ${Math.random() * 1000}`;

module.exports = async function() {
  const { browser } = this;

  //
  await browser.setMeta('1', 'ввести имя в поле создания новой задачи, нажать Enter');
  const newIssue = await browser.$(AccountsLocators.NEW_ISSUE_INPUT);
  await newIssue.waitForDisplayed();
  await newIssue.setValue([issueName, 'Enter']);
  //
  await browser.setMeta(
    '2',
    'подождать, пока кнопка Создать перестанет быть активной (т.е. задача сохранится)',
  );
  const saveIssue = await browser.$(AccountsLocators.SAVE_ISSUE_BUTTON);
  await saveIssue.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  //
  await browser.setMeta(
    '3',
    'открыть созданную задачу (она автоматически открывается в новой вкладке)',
  );
  const firstIssue = await browser.$(AccountsLocators.FIRST_OPEN_ISSUE);
  await firstIssue.waitForDisplayed();
  await firstIssue.click();

  //
  await browser.setMeta('4', 'переключить фокус на вкладку с открытой задачей');
  await browser.switchWindow('issues');

  //
  await browser.setMeta('5', 'отсортировать список задач по убыванию даты создания');
  const sorter = await this.browser.$(IssuesLocators.SORT_ISSUES_LIST);
  await sorter.waitForDisplayed();
  await sorter.click();
  await browser.pause(500);
  const dateSorter = await this.browser.$(IssuesLocators.SORT_BY_CREATE_DATE);
  await dateSorter.waitForDisplayed({ timeout: 5000, interval: 500 });
  await dateSorter.click({ x: 5, y: 5 });
  await browser.pause(2000);

  //
  await browser.setMeta('6', 'найти в видимом списке задачу с указанным названием и перейти в неё');
  const issues = await browser.$$(IssuesLocators.ISSUE_LIST_BLOCK);
  for (let i = 0, length = issues.length; i < length; i++) {
    const issue = issues[i];
    const textNode = await issue.$(IssuesLocators.ISSUE_LIST_BLOCK_NAME);
    const text = await textNode.getText();
    if (text === issueName) {
      await issue.click(); //перейти в найденный тикет
      return issue;
    }
  }

  //
  await browser.setMeta('7', 'взять название из задачи и найти в тексте нужные соответствия');
  const openIssueName = await browser.$(IssuesLocators.OPEN_ISSUE_NAME);
  await openIssueName.waitForDisplayed({ timeout: 5000, interval: 500 });
  assert.include(await openIssueName.getText(), issueName, 'issue name does not match');
  //
  await browser.setMeta('8', 'взять аккаунт из задачи и найти в тексте нужные соответствия');
  const openIssueAccount = await browser.$(IssuesLocators.OPEN_ISSUE_ACCOUNT);
  await openIssueAccount.waitForDisplayed();
  assert.include(await openIssueAccount.getText(), 'vfhuj5', 'account in issue was not added');

  //
  await browser.setMeta('9', 'взять статус из задачи и найти в тексте нужные соответствия');
  const issueStatus = await browser.$(IssuesLocators.ISSUE_STATUS);
  const isIssueOpened = await issueStatus.getText();
  assert.include(isIssueOpened, 'Открыта', 'issue was not created');
};
