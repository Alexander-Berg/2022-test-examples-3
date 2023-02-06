const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //нажать на Исполнителя
  const assigneeField = await browser.$(TicketsLocators.ASSIGNEE_FIELD);
  await assigneeField.waitForDisplayed();
  await assigneeField.click();
  //нажать крестик удаления исполнителя
  const deleteAssignee = await browser.$(TicketsLocators.DELETE_ASSIGNEE_FROM_ISSUE);
  await deleteAssignee.waitForDisplayed();
  await deleteAssignee.click();
  //выйти из режима редактирования поля
  const assigneeInput = await browser.$(TicketsLocators.ASSIGNEE_INPUT);
  await assigneeInput.waitForDisplayed();
  await assigneeInput.setValue('Escape');
  await browser.pause(3000);

  //вытащить значение атрибута Исполнитель
  const assigneeAttribute = await browser.getAttributeValue('Исполнитель');
  //проверить, что этот атрибут равен -
  assert.equal(assigneeAttribute, '–', 'assignee was not left empty');
};
