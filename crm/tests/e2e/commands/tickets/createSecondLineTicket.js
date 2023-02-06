const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function(ticketText) {
  const actions = await this.$(TicketsLocators.ACTIONS_IN_TICKET);
  await actions.waitForDisplayed();
  await actions.click();

  const createSecondTicket = await this.$(TicketsLocators.ACTION_CREATE_2ND_TICKET);
  await createSecondTicket.waitForClickable();
  await createSecondTicket.click();
  await this.pause(1000);

  const selectCategory = await this.$(TicketsLocators.SELECT_CATEGORY_2ND_TICKET);
  await selectCategory.waitForDisplayed({
    timeout: 20000,
    interval: 500,
    reverse: true,
  });
  await this.switchToFrame(1);

  const mainIdea = await this.$(TicketsLocators.MAIN_IDEA_FOR_2ND_TICKET);
  await mainIdea.waitForDisplayed();
  await mainIdea.setValue(ticketText);

  const saveTicket = await this.$(TicketsLocators.SAVE_2ND_TICKET);
  await saveTicket.waitForClickable();
  await saveTicket.click();

  await this.refresh();
};
