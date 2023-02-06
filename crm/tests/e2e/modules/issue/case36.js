const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  const assigneeField = await browser.$(IssuesLocators.ASSIGNEE_FIELD);
  await assigneeField.waitForDisplayed();
  await assigneeField.click();

  const deleteAssignee = await browser.$(IssuesLocators.DELETE_ASSIGNEE_FROM_ISSUE);
  await deleteAssignee.waitForDisplayed();
  await deleteAssignee.click();

  const assigneeInput = await browser.$(IssuesLocators.ASSIGNEE_INPUT);
  await assigneeInput.setValue('Crmcrown Robot');

  const optionAssignee = await browser.$(IssuesLocators.OPTION_FOR_ASSIGNEE);
  await optionAssignee.waitForDisplayed();
  await optionAssignee.click();

  const saveAssignee = await browser.$(IssuesLocators.SAVE_ASSIGNEE_BUTTON);
  await saveAssignee.waitForEnabled();
  await saveAssignee.click();

  await assigneeField.waitForDisplayed();
  await assigneeField.click();

  const assigneeName = await browser.$(IssuesLocators.ASSIGNEE_NAME);
  await assigneeName.waitForDisplayed();
  const isAssignee = await assigneeName.getText();
  assert.equal('Crmcrown Robot', isAssignee, 'assignee was not changed to Crmcrown Robot');
};
