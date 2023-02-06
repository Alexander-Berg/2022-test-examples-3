const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //нажать на кнопку Исполнитель
  const assigneeField = await browser.$(TicketsLocators.ASSIGNEE_FIELD);
  await assigneeField.waitForDisplayed();
  await assigneeField.click();

  //удалить исполнителя крестиком
  const deleteAssignee = await browser.$(TicketsLocators.DELETE_ASSIGNEE_FROM_ISSUE);
  await deleteAssignee.waitForDisplayed();
  await deleteAssignee.click();
  await browser.pause(3000);

  //выйти из режима редактирования поля
  const assigneeInput = await browser.$(TicketsLocators.ASSIGNEE_INPUT);
  await assigneeInput.setValue('Escape');

  const takeNext = await browser.$(TicketsLocators.TAKE_NEXT_TICKET);
  await takeNext.waitForDisplayed();
  await takeNext.click();

  const goInProgress = await browser.$(TicketsLocators.GO_TO_IN_PROGRESS_FILTER);
  await goInProgress.waitForDisplayed();
  await goInProgress.click();

  await browser.refresh();

  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET); // очень тупая проверка, надо переписывать
  await latestTicket.waitForDisplayed();
  const isTicketInProgress = await latestTicket.getText();

  assert.include(isTicketInProgress, 'В работе', 'ticket is not in progress');
};
