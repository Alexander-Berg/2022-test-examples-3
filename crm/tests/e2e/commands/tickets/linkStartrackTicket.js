const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function(ticket) {
  //нажать кнопку "Привязать задачу ST"
  const linkStartrack = await this.$(TicketsLocators.LINK_ST_TICKET_BUTTON);
  await linkStartrack.waitForDisplayed();
  await linkStartrack.click();

  //откроется форма привязки
  const startrackForm = await this.$(TicketsLocators.ST_TICKET_FORM);
  await startrackForm.waitForDisplayed();

  //в поле "ST тикет" записать переданное в процедуру значение ticket
  const startrackInput = await this.$(TicketsLocators.ST_TICKET_TO_LINK_INPUT);
  await startrackInput.waitForDisplayed();
  await startrackInput.setValue(ticket);

  //нажать кнопку "Сохранить"
  const saveStartrack = await this.$(TicketsLocators.SAVE_ST_TICKET_BUTTON);
  await saveStartrack.waitForClickable();
  await saveStartrack.click();

  //дождаться, пока отобразится вкладка Связанные
  const linkedTicket = await this.$(TicketsLocators.LINKED_TICKETS);
  await linkedTicket.waitForClickable({
    timeout: 10000,
    interval: 500,
  });

  await linkedTicket.click();
};
