const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //нажать кнопку Исполнитель
  const assigneeField = await browser.$(TicketsLocators.ASSIGNEE_FIELD);
  await assigneeField.waitForDisplayed();
  await assigneeField.click();
  await browser.setMeta('Шаг 1', 'Нажали кнопку Исполнитель');
  //не удаляя предыдущего исполнителя,
  //в поле ввода записать 'Crmcrown Robot'
  const assigneeInput = await browser.$(TicketsLocators.ASSIGNEE_INPUT);
  await assigneeInput.waitForDisplayed();
  await assigneeInput.setValue('Crmcrown Robot');
  await browser.pause(1000);

  //выбрать его из получившегося списка
  const optionAssignee = await browser.$(TicketsLocators.ROBOT_TITLE_IN_SUGGEST);
  await optionAssignee.waitForDisplayed();
  await optionAssignee.click();
  await browser.pause(2000);

  //вытащить значение атрибута Исполнитель
  const assigneeAttribute = await browser.getAttributeValue('Исполнитель');
  //и сравнить его с Crmcrown Robot
  assert.equal(assigneeAttribute, 'Crmcrown Robot', 'assignee was not changed to Crmcrown Robot');
};
