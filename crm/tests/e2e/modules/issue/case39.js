const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  //нажать на Воркфлоу
  const workflow = await browser.$(IssuesLocators.WORKFLOW_FIELD);
  await workflow.waitForDisplayed();
  await workflow.click();
  await browser.pause(500);
  //выбрать "Стандартный тикет"
  const standartWorkflow = await browser.$(IssuesLocators.STANDART_WORKFLOW_OPTION);
  await standartWorkflow.click();
  await browser.pause(500);
  //вытащить значение атрибута Воркфлоу
  const workflowValue = await browser.getAttributeValue('Воркфлоу');
  //и сравнить его с 'Стандартный тикет'
  assert.equal(workflowValue, 'Стандартный тикет', 'workflow was not changed');

  //увидеть стандартную для этого воркфлоу кнопку "В работу"
  const acceptStandartWorkflow = await browser.$(IssuesLocators.STANDART_WORKFLOW_BUTTON_ACCEPT);
  const isWorkflowChanged = await acceptStandartWorkflow.waitForDisplayed();

  assert.isTrue(
    isWorkflowChanged,
    'button в работу specific to standart workflow was not found on the page',
  );
};
