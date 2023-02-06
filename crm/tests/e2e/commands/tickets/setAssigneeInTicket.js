// функция назначает исполнителя тикету, переданного в параметре assignee
const { TicketsLocators } = require('../../pages/locators/tickets');

module.exports = async function(assignee) {
  // кликнуть на Исполнителя
  const assigneeAttribute = await this.$(TicketsLocators.TICKET_ASSIGNEE_ATTRIBUTE);
  await assigneeAttribute.waitForDisplayed();
  await assigneeAttribute.click();
  // дождаться, пока попап со списком пользователей станет видимым
  await (await this.$(TicketsLocators.TICKET_ASSIGNEE_POPUP)).waitForDisplayed({
    timeout: 5000,
    interval: 500,
  });
  // ввести в него имя исполнителя
  const assigneeInput = await this.$(TicketsLocators.TICKET_ASSIGNEE_INPUT);
  await assigneeInput.setValue(assignee);

  // дождаться загрузки результатов в попапе
  this.pause(2000);

  // дождаться, пока этот исполнитель отобразится в первой строке, и выбрать его
  const assigneeInList = await this.$(TicketsLocators.TICKET_ASSIGNEE_LIST_POPUP).$(
    '//span[text()="' + assignee + '"]',
  );
  await assigneeInList.waitForDisplayed({
    timeout: 5000,
    interval: 500,
  });
  await assigneeInList.click();

  // подождать, пока исполнитель отобразится на странице, и вернуть его значение в результат функции
  await assigneeAttribute.waitForClickable({ timeout: 10000, interval: 1000 });
  //await this.pause(5000);
  const ticketAssignee = await this.getAttributeValue('Исполнитель');
  // если значение исполнителя не пустое, то вернуть его
  if (ticketAssignee) return ticketAssignee;
  return false;
};
