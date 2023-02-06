const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //задаем параметры письма
  const mailData = {
    from: '<robot-space-odyssey@yandex-team.ru>',
    to: ['<robot-tcrm-test@yandex-team.ru>'],
    subject: 'case92_2',
    body: 'case 92_2',
    important: undefined,
  };

  //
  await browser.setMeta(
    '1',
    'отправить письмо с указанными параметрами и перейти в созданный тикет',
  );
  await this.browser.sendMail(mailData);

  //
  await browser.setMeta('2', 'найти кнопку "Создать тикет в ST" на форме письма и нажать ее');
  const createStTicketButton = await browser.$(TicketsLocators.CREATE_ST_TICKET_ON_MAIL_FORM);
  await createStTicketButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  await createStTicketButton.click();
  await browser.pause(5000); //здесь надо ждать, иначе нажимает кнопку в шапке

  //
  await browser.setMeta('3', 'найти заголовок модального окна');
  const isCreateStTicketWindowOpen = await browser.$(TicketsLocators.ST_CREATE_NEW_TICKET_HEADER);
  await isCreateStTicketWindowOpen.waitForDisplayed({ timeout: 5000, interval: 500 });

  //
  await browser.setMeta('4', 'заполнить поле Очередь');
  const selectQueue = await browser.$(TicketsLocators.ST_TICKET_QUEUE_FIELD);
  await selectQueue.waitForDisplayed();
  await selectQueue.setValue('CRMMY');

  const setCrmQueue = await browser.$(TicketsLocators.ST_TICKET_QUEUE_TO_SELECT_CRMMY);
  await selectQueue.waitForDisplayed();
  await setCrmQueue.click();

  //
  await browser.setMeta('5', 'выбрать тип задачи - Task');
  const selectType = await browser.$(TicketsLocators.ST_TICKET_TYPE);
  await selectType.click();
  await browser.pause(1000);
  const setType = await browser.$(TicketsLocators.ST_TICKET_TYPE_TO_SELECT_TASK);
  await setType.click();

  //
  await browser.setMeta('6', 'запомнить Название задачи');
  const selectTaskName = await browser.$(TicketsLocators.ST_TICKET_NAME_INPUT);
  const taskNameFromMailtitle = await selectTaskName.getValue();

  //Получаем текст описания
  /*const getStDescriptionText = await browser.$(TicketsLocators.ST_TICKET_NEW_BODY);
    await browser.pause(3000);
    const StDescriptionText = await getStDescriptionText.getValue();
    console.log('Текст в поле описание:', StDescriptionText);
    */

  //
  await browser.setMeta('7', 'кнопка Cоздать должна стать доступной');
  const isCreateButtonEnabled = await browser.$(TicketsLocators.SAVE_ST_TICKET_BUTTON_MGT);
  await isCreateButtonEnabled.waitForClickable({
    timeout: 5000,
    interval: 500,
  });
  await isCreateButtonEnabled.click();
  await browser.refresh();

  //
  await browser.setMeta('8', 'перейти на таб Связанные');
  const linkedTicketTab = await browser.$(TicketsLocators.LINKED_TICKETS);
  await linkedTicketTab.click();
  await browser.pause(3000);

  //
  await browser.setMeta(
    '9',
    'найти id, название, и статус ST тикета на странице связанных тикетов',
  );
  const isTicketCreated = await browser.$(TicketsLocators.LINKED_ST_TICKET_CRMMY);
  const ticketId = await isTicketCreated.getText();

  //const statusTicket = await browser.$(TicketsLocators.ST_TICKET_STATUS);
  //const StatusTicketOpen = statusTicket.getText();
  // console.log('StatusTicketOpen=', StatusTicketOpen);

  const titleTicket = await browser.$(TicketsLocators.ST_TICKET_TITLE);
  const isStTicketTitle = await titleTicket.getText();
  //
  await browser.setMeta('10', 'сравнить id и название тикета с ожидаемыми');

  assert.include(ticketId, 'CRMMY', 'Ticket id is incorrect');
  //assert.include(StatusTicketOpen, 'Открыт', 'Ticket status is incorrect');
  assert.include(isStTicketTitle, taskNameFromMailtitle, 'Ticket name is not correct');

  //Кликнуть на идентификатор тикета в очереди
  //const isTicketCreated = await browser.$(TicketsLocators.LINKED_ST_TICKET_CRMMY);
  //await isTicketCreated.waitForDisplayed();
  // await isTicketCreated.click();
  // await console.log('Кликнули по ссылке тикета, ждем когда отобразится инфа в окне справа');
  //await browser.pause(5000);

  //В окне справа показана информация о тикете
  //const testVar = await browser.$(TicketsLocators.ST_CREATED_INFORMATION);
  // await testVar.waitForDisplayed({ timeout: 20000, interval: 500 });
  // const isStInfoDisplayed = await testVar.getText();

  // await console.log('Информация о тикете выведена в окне справа:', isStInfoDisplayed);

  //const stTicketCreatedBody = await browser.$(TicketsLocators.ST_TICKET_CREATED_BODY);
  //const isDecriptionSTTicket = await stTicketCreatedBody.getText();

  //await console.log('isDecriptionSTTicket', isDecriptionSTTicket);

  //const isHeaderDisplayed = await browser.$(TicketsLocators.HEADER_ST_TICKET);
  //await isHeaderDisplayed.waitForDisplayed();

  //Сравнить заголовок тикета с тем, что создавали
  //const isStTicketHeaderCorrect = await isHeaderDisplayed.getText();
  //await console.log('Проверяем, что в инфе о тикете содержится тема и тело письма');
  //assert.include(isStInfoDisplayed, taskNameFromMailtitle, 'Ticket name is not correct or found');
  //assert.include(isStInfoDisplayed, StDescriptionText, 'Ticket body is not correct or found');
};
