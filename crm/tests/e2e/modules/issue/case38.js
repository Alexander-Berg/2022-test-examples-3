const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  const latestIssue = await this.browser.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await latestIssue.waitForDisplayed();
  await latestIssue.doubleClick();

  const renameInput = await browser.$(IssuesLocators.INPUT_RENAME_ISSUE);
  await renameInput.waitForDisplayed();
  await renameInput.setValue([' to be deleted', 'Enter']);

  const renamedTitle = await browser.$(
    './/span[text()="Issue created with automated test to be deleted"]',
  );
  await renamedTitle.waitForDisplayed();

  const actions = await browser.$(IssuesLocators.ACTIONS_IN_ISSUE_ACTIVATED);
  await actions.waitForDisplayed();
  await actions.click();

  //нажать на кнопку Действия
  const deleteIssue = await browser.$(IssuesLocators.DELETE_ISSUE);
  await deleteIssue.waitForDisplayed();

  // нажать на кнопку Удалить в списке доступных действий
  await deleteIssue.click();

  await browser.acceptAlert();

  // подтвердить удаление во всплывашке
  const isNewIssueDeleted = await renamedTitle.isExisting();

  assert.isTrue(isNewIssueDeleted, 'issue was not deleted');
};
