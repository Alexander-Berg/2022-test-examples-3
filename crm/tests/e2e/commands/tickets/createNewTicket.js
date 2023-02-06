const { TicketsLocators } = require('../../pages/locators/tickets');

module.exports = async function(name, queue) {
  //нажать кнопку "Создать тикет"
  const createTicket = await this.$(TicketsLocators.CREATE_TICKET);
  await createTicket.waitForDisplayed();
  await createTicket.click();

  try {
    //попробовать сохранить тикет без введенных данных
    await this.saveTicket();
  } catch {
    //если сохранить не удалось, заполнить поля и сохранить
    const ticketSummary = await this.$(TicketsLocators.SUMMARY_FOR_NEW_TICKET);
    await ticketSummary.waitForDisplayed({ timeout: 5000, interval: 1000 });
    await ticketSummary.setValue(name);
    const ticketQueue = await this.$(TicketsLocators.QUEUE_FOR_NEW_TICKET);
    await ticketQueue.setValue(queue);

    const queueOption = await this.$(TicketsLocators.OPTION_FOR_QUEUE);
    await queueOption.waitForDisplayed();
    await queueOption.click();

    const selectAccount = await this.$(TicketsLocators.SELECT_ACCOUNT_FOR_NEW_TICKET);
    await selectAccount.waitForDisplayed();
    await selectAccount.click();

    const zeroAccount = await this.$(TicketsLocators.ZERO_ACCOUNT_CHECKBOX);
    await zeroAccount.waitForDisplayed();
    await zeroAccount.click();

    const saveAccount = await this.$(TicketsLocators.SAVE_ACCOUNT_BUTTON_FOR_NEW_TICKET);
    await saveAccount.waitForClickable();
    await saveAccount.click();

    const selectCategory = await this.$(TicketsLocators.SELECT_CATEGORY_FOR_NEW_TICKET);
    await selectCategory.waitForDisplayed();
    await selectCategory.click();

    const categoryInput = await this.$(TicketsLocators.SEARCH_CATEGORY_INPUT);
    await categoryInput.setValue('Причина 1');

    const testCategory = await this.$(TicketsLocators.TEST_CATEGORY);
    await testCategory.waitForDisplayed();
    await testCategory.click();
    // здесь будет переход в последний созданный тикет
    await this.saveTicket();
  }
};
