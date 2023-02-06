const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  //если кнопка сохранения активна
  const saveTicketButton = await this.$(TicketsLocators.SAVE_TICKET);
  await saveTicketButton.waitForEnabled({ timeout: 5000, interval: 1000 });

  //то нажать ее
  await saveTicketButton.click();

  //здесь нужна пауза (или рефреш), чтобы созданный тикет успел отобразиться вверху списка
  //также можно подождать, пока модальное окно закроется, но на практике это получилось очень долго

  //await this.pause(2000);
  await this.refresh();

  //перейти в последний созданный тикет
  const LatestCreatedTicket = await this.$(TicketsLocators.LATEST_CREATED_TICKET);
  await LatestCreatedTicket.waitForClickable({ timeout: 5000, interval: 500 });
  await LatestCreatedTicket.click();
};
