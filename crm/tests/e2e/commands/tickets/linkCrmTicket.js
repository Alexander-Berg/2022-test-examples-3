const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function(ticket) {
  // нажать кнопку Действия в старой шапке тикета
  const actions = await this.$(TicketsLocators.ACTIONS_IN_TICKET);
  await actions.waitForDisplayed();
  await actions.click();
  // выбрать пункт "Добавить связь"
  const addLink = await this.$(TicketsLocators.ACTION_ADD_LINK);
  await addLink.waitForDisplayed();
  await addLink.click();
  // в открывшейся форме указать в поле тикет значение из параметра ticket
  const crmTicketLink = await this.$(TicketsLocators.CRM_TICKET_KEY_TO_LINK);
  await crmTicketLink.waitForDisplayed();
  await crmTicketLink.setValue(ticket);
  // нажать кнопку Сохранить
  const saveTicket = await this.$(TicketsLocators.SAVE_LINK_TO_CRM_TICKET);
  await saveTicket.waitForClickable();
  await saveTicket.click();
  // дождаться, пока форма закроется
  await saveTicket.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  // перейти в тикете на таб Связанные
  const linkedTicket = await this.$(TicketsLocators.LINKED_TICKETS);
  await linkedTicket.waitForDisplayed();
  await linkedTicket.click();
};
