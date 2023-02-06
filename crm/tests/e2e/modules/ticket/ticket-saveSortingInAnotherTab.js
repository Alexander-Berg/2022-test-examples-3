const { assert } = require('chai');
const { TicketsLocators } = require('../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  // переход в модуль тикетов в очередь 'Отдел CRM (тест).Autotest' происходит
  // автоматически при логине в beforeEach
  //
  await browser.setMeta('1', 'выбрать сортировку Приоритет ▼, дата последнего события ▲');
  const LatestCreatedTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  await LatestCreatedTicket.waitForClickable({ timeout: 15000, interval: 1000 });
  await browser.sortTickets('Приоритет ▼, дата последнего события ▲');

  //
  await browser.setMeta('2', 'скопировать урл');
  const urlToCopy = await browser.getUrl();
  //
  await browser.setMeta(
    '3',
    'в соседней вкладке открыть его же (переход в созданное окно идет автоматически)',
  );
  await browser.newWindow(urlToCopy);
  //
  await browser.setMeta('4', 'забрать идентификаторы обеих вкладок');
  const handles = await browser.getWindowHandles();
  //
  await browser.setMeta('5', 'проверить, что сортировка сохранилась');
  const sortButtonValue = await (
    await browser.$(TicketsLocators.TICKET_SORT_BUTTON_VALUE)
  ).getText();
  assert.strictEqual(
    sortButtonValue,
    'Приоритет ▼, дата последнего события ▲',
    'сортировка не сохранилась в новой вкладке',
  );
  //
  await browser.setMeta('6', 'закрыть вкладку');
  await browser.closeWindow();
  //
  await browser.setMeta('7', 'переключиться на первую вкладку');
  await browser.switchToWindow(handles[0]);
};
