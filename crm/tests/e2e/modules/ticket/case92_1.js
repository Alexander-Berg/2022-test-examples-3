const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');
const taskName = `St task name... ${Math.random() * 1000}`;

module.exports = async function() {
  const { browser } = this;

  //Находим кнопку "Создать тикет в ST и нажимаем её"
  const pressCreateStTicketButton = await browser.$(TicketsLocators.CREATE_ST_TICKET_BUTTON);
  await pressCreateStTicketButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  await pressCreateStTicketButton.click();

  //Находим заголовок модального окна
  const isCreateStTicketWindowOpen = await browser.$(TicketsLocators.ST_CREATE_NEW_TICKET_HEADER);
  await isCreateStTicketWindowOpen.waitForDisplayed({ timeout: 5000, interval: 500 });

  //Заполняем поле Очередь
  const selectQueue = await browser.$(TicketsLocators.ST_TICKET_QUEUE_FIELD);
  await selectQueue.waitForDisplayed();
  await selectQueue.setValue('CRMMY');

  const setCrmQueue = await browser.$(TicketsLocators.ST_TICKET_QUEUE_TO_SELECT_CRMMY);
  await selectQueue.waitForDisplayed();
  await setCrmQueue.click();

  //Выбираем тип задачи - Task
  const selectType = await browser.$(TicketsLocators.ST_TICKET_TYPE);
  await selectType.click();
  const setType = await browser.$(TicketsLocators.ST_TICKET_TYPE_TO_SELECT_TASK);
  await setType.click();

  //Заполняем поле Название задачи
  const selectTaskName = await browser.$(TicketsLocators.ST_TICKET_NAME_INPUT);
  await selectTaskName.click();
  await selectTaskName.setValue(taskName);

  //Кнопка создать должна стать доступной
  const isCreateButtonEnabled = await browser.$(TicketsLocators.SAVE_ST_TICKET_BUTTON_MGT);
  await isCreateButtonEnabled.waitForClickable({ timeout: 5000, interval: 500 });
  await isCreateButtonEnabled.click();

  await browser.refresh();

  //перейди на таб Связанные
  const linkedTicketTab = await browser.$(TicketsLocators.LINKED_TICKETS);
  await linkedTicketTab.click();
  await browser.pause(3000);

  //Найти ИД, название, и статус ST тикета на странице связанных тикетов
  const isTicketCreated = await browser.$(TicketsLocators.LINKED_ST_TICKET_CRMMY);
  const ticketId = await isTicketCreated.getText();

  const titleTicket = await browser.$(TicketsLocators.ST_TICKET_TITLE);
  const isStTicketTitle = await titleTicket.getText();

  assert.include(ticketId, 'CRMMY', 'Ticket id is incorrect');
  //assert.include(StatusTicketOpen, 'Открыт', 'Ticket status is incorrect');
  assert.include(isStTicketTitle, taskName, 'Ticket name is not correct');

  /*
  //Кликнуть на идентификатор тикета в очереди
  const isTicketCreated = await browser.$(TicketsLocators.LINKED_ST_TICKET_CRMMY);
  await isTicketCreated.waitForDisplayed();
  await isTicketCreated.click();
  await browser.pause(1000);

  //Увидеть заголовок тикета в открывщемся окне
  const isHeaderDisplayed = await browser.$(TicketsLocators.HEADER_ST_TICKET);
  await isHeaderDisplayed.waitForDisplayed();

  //Сравнить заголовок тикета с тем, что создавали
  const isStTicketHeaderCorrect = await isHeaderDisplayed.getText();

  assert.include(isStTicketHeaderCorrect, taskName, 'Ticket name is not correct or found');
*/
};
