const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function(name) {
  //перейти на вкладку Связанные в задаче
  const linkedIssue = await this.$(IssuesLocators.LINKED_ISSUES);
  await linkedIssue.waitForDisplayed();
  await linkedIssue.click();
  //ввести название новой задачи, нажать Enter
  const inputLinkedIssue = await this.$(IssuesLocators.INPUT_LINKED_ISSUE);
  await inputLinkedIssue.waitForDisplayed();
  await inputLinkedIssue.setValue([name, 'Enter']);
};
