const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  //нажать кнопку Закрыть
  const closeOpenButton = await browser.$(IssuesLocators.CLOSE_OPEN_ISSUE_BUTTON);
  await closeOpenButton.waitForDisplayed();
  await closeOpenButton.click();

  //нажать кнопку Открыть
  await closeOpenButton.waitForEnabled({ timeout: 3000 });
  await closeOpenButton.click();

  //дождаться, пока кнопка Закрыть отобразится и станет активной
  const isClosed = await closeOpenButton.waitForEnabled({ timeout: 3000 });

  //взять текст из первой задачи по списку и найти в нем слово Открыта
  const latestIssue = await browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  const isNewIssueReopened = await latestIssue.getText();
  assert.include(isNewIssueReopened, 'Открыта', 'issue was not reopened');
  assert.isTrue(isClosed, 'circle is checked when we reopened the issue');
};
