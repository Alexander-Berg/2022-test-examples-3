const { assert } = require('chai');
const { TicketsLocators } = require('../../pages/locators/tickets');
const ticketName = `e2e-test ${Math.random() * 1000}`;
const ticketQueue = 'Отдел CRM (тест).Autotest';
const ticketNameForPrioritySorting = 'case checkSorting high priority';
const ticketNameForEarlyStartDateSorting = 'case checkSorting early start date';
const ticketNameForEarlyCreateDateSorting = 'case checkSorting early create date';

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'создать новый тикет, переход в него идет автоматом');
  await browser.createNewTicket(ticketName, ticketQueue);
  //
  await browser.setMeta('2', 'выбрать сортировку Дата последнего события ▲');
  await browser.sortTickets('Дата последнего события ▲');
  const LatestCreatedTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  await LatestCreatedTicket.waitForClickable({ timeout: 15000, interval: 1000 });
  //
  await browser.setMeta('3', 'проверить, что тикета нет в видимом списке');
  assert.isFalse(
    await browser.findTicketInList(ticketName),
    'при этой сортировке тикет не должен отображаться в списке',
  );
  //
  await browser.setMeta('4', 'выбрать сортировку Дата последнего события ▼');
  await browser.sortTickets('Дата последнего события ▼');
  await LatestCreatedTicket.waitForClickable({ timeout: 15000, interval: 1000 });
  //
  await browser.setMeta('5', 'найти созданный тикет среди результатов');
  assert.strictEqual(
    await browser.findTicketInList(ticketName),
    ticketName,
    'при этой сортировке тикет должен отображаться в списке',
  );

  // для этой сортировки на тестинге сделан тикет с  названием "case checkSorting high priority" и пустым таймлайном
  // ему проставлен Высокий приоритет, даты создания и последнего события заменены на 2016 год
  //
  await browser.setMeta('6', 'выбрать сортировку Приоритет ▼, дата последнего события ▲');
  await browser.sortTickets('Приоритет ▼, дата последнего события ▲');
  await LatestCreatedTicket.waitForClickable({ timeout: 15000, interval: 1000 });
  //
  await browser.setMeta(
    '7',
    'найти тикет с названием "case checkSorting high priority" среди результатов',
  );
  assert.strictEqual(
    await browser.findTicketInList(ticketNameForPrioritySorting),
    ticketNameForPrioritySorting,
    'при этой сортировке тикет "case checkSorting high priority" должен отображаться в списке',
  );

  // для этой сортировки на тестинге сделан тикет с названием "case checkSorting early start date" и пустым таймлайном
  // ему проставлен Высокий приоритет, даты создания, последнего события и крайний срок начала работы заменены на 2016 год
  //
  await browser.setMeta(
    '8',
    'выбрать сортировку Приоритет ▼, крайний срок начала работы ▲, дата последнего события ▲',
  );
  await browser.sortTickets('Приоритет ▼, крайний срок начала работы ▲, дата последнего события ▲');
  await LatestCreatedTicket.waitForClickable({ timeout: 15000, interval: 1000 });
  //
  await browser.setMeta(
    '9',
    'проверить, что тикет "case checkSorting early start date" есть в видимом списке',
  );
  assert.strictEqual(
    await browser.findTicketInList(ticketNameForEarlyStartDateSorting),
    ticketNameForEarlyStartDateSorting,
    'при этой сортировке тикет "case checkSorting early start date" должен отображаться в списке',
  );
  // для этой сортировки на тестинге сделан тикет с названием "case checkSorting early create date" и пустым таймлайном
  // даты создания и последнего события для него заменены на 2016 год
  //
  await browser.setMeta('10', 'выбрать сортировку Дата создания ▲');
  await browser.sortTickets('Дата создания ▲');
  await LatestCreatedTicket.waitForClickable({ timeout: 15000, interval: 1000 });
  //
  await browser.setMeta(
    '11',
    'проверить, что тикет "case checkSorting early create date" есть в видимом списке',
  );
  assert.strictEqual(
    await browser.findTicketInList(ticketNameForEarlyCreateDateSorting),
    ticketNameForEarlyCreateDateSorting,
    'при этой сортировке тикет "case checkSorting early create date" должен отображаться в списке',
  );

  //
  await browser.setMeta('12', 'выбрать сортировку Дата создания ▼');
  await browser.sortTickets('Дата создания ▼');
  await LatestCreatedTicket.waitForClickable({ timeout: 15000, interval: 1000 });
  //
  await browser.setMeta('13', 'найти созданный тикет среди результатов');
  assert.strictEqual(
    await browser.findTicketInList(ticketName),
    ticketName,
    'при этой сортировке тикет должен отображаться в списке',
  );
};
