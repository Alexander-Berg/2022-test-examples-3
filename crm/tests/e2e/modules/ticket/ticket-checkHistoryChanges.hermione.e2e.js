const { assert } = require('chai');
const { TicketsLocators } = require('../../pages/locators/tickets');
const { OUT_MAIL_ADDRESS } = require('../../constants/mail');
const ticketName = `e2e-test ${Math.random() * 1000}`;
const ticketQueue = 'Отдел CRM (тест).Autotest';
const commentText = 'comment to ticket ' + ticketName;
const mailText = 'письмо для тикета ' + ticketName;
const mailTheme = 'тема тикета ' + ticketName;

function countRows(historyRecords, expectedCount) {
  const historyRecordsCount = historyRecords.length;
  return assert.strictEqual(
    historyRecordsCount,
    expectedCount,
    'количество строк в истории не совпадает с ожидаемым',
  );
}

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'создать новый тикет, переход в него идет автоматом');
  await browser.createNewTicket(ticketName, ticketQueue);
  //
  await browser.setMeta('2', 'перейти на вкладку История изменений');
  const ticketHistoryTab = await browser.$(TicketsLocators.TICKET_HISTORY_TAB);
  await ticketHistoryTab.waitForDisplayed();
  await ticketHistoryTab.click();
  const ticketHistoryIframe = await browser.$(TicketsLocators.TICKET_HISTORY);
  await ticketHistoryIframe.waitForDisplayed({ timeout: 5000, interval: 1000 });
  //переключиться в iframe
  await browser.switchToFrame(1);
  const ticketHistoryTable = await browser.$(TicketsLocators.TICKET_HISTORY_TABLE);
  await ticketHistoryTable.waitForDisplayed({ timeout: 5000, interval: 1000 });

  //
  await browser.setMeta('3', 'проверить, что запись в истории изменений одна');
  let historyRecords = await browser.$$(TicketsLocators.TICKET_HISTORY_TABLE_ROW);
  countRows(historyRecords, 1);
  //
  await browser.setMeta('4', 'забрать весь текст в истории');
  const fullHistoryText = await ticketHistoryTable.getText();
  //
  await browser.setMeta(
    '5',
    'найти значения полей, сравнить с теми, которые вводились при создании тикета',
  );
  assert.include(fullHistoryText, 'Add', 'в истории нет действия Add');
  assert.include(
    fullHistoryText,
    'Автор CRM Space Odyssey Robot',
    'автор тикета не совпадает с роботом',
  );
  assert.include(
    fullHistoryText,
    'Название ' + ticketName,
    'название тикета не совпадает с ожидаемым',
  );
  assert.include(fullHistoryText, 'Линия 0', 'линия тикета не совпадает с ожидаемой');
  assert.include(fullHistoryText, 'Статус Открыт', 'статус тикета не совпадает с ожидаемым');
  assert.include(fullHistoryText, 'Тип 3', 'тип тикета не совпадает с ожидаемым');
  assert.include(
    fullHistoryText,
    'Воркфлоу Стандартный тикет',
    'воркфлоу тикета не совпадает с ожидаемым',
  );
  assert.include(
    fullHistoryText,
    'Очередь Отдел CRM (тест).Autotest',
    'очередь тикета не совпадает с ожидаемой',
  );
  //
  await browser.setMeta('6', 'выйти из iframe и переключиться на таб Текущий');
  await browser.switchToParentFrame();
  const ticketCurrentTab = await browser.$(TicketsLocators.TICKET_CURRENT_TAB);
  await ticketCurrentTab.waitForDisplayed();
  await ticketCurrentTab.click();
  //
  await browser.setMeta('7', 'написать комментарий в тикете');
  await browser.writeCommentToIssue(commentText);
  //
  await browser.setMeta('8', 'перейти на вкладку История изменений');
  await ticketHistoryTab.waitForDisplayed();
  await ticketHistoryTab.click();
  await ticketHistoryIframe.waitForDisplayed();
  await browser.switchToFrame(1);
  //
  await browser.setMeta(
    '9',
    'проверить значения в верхней строке таблицы и сравнить их с ожидаемыми',
  );
  let firstRowInHistory = await browser.$(TicketsLocators.TICKET_HISTORY_TABLE_FIRST_ROW);
  let firstRowContent = await firstRowInHistory.getText();
  assert.include(
    firstRowContent,
    'Редактирование пользователем',
    'триггер комментария не совпадает с ожидаемым',
  );
  assert.include(firstRowContent, 'Modify', 'действие комментария не совпадает с ожидаемым');
  assert.include(
    firstRowContent,
    'Комментарии ' + commentText,
    'текст комментария не совпадает с ожидаемым',
  );
  //
  await browser.setMeta('10', 'проверить, что записей в истории изменений две');
  historyRecords = await this.browser.$$(TicketsLocators.TICKET_HISTORY_TABLE_ROW);
  countRows(historyRecords, 2);
  //
  await browser.setMeta('11', 'выйти из iframe и переключиться на таб Текущий');
  await browser.switchToParentFrame();
  await ticketCurrentTab.waitForDisplayed();
  await ticketCurrentTab.click();
  //
  await browser.setMeta('12', 'отправить письмо из тикета');
  await browser.writeOutMailFromTicket(mailText, OUT_MAIL_ADDRESS, mailTheme);
  //
  await browser.setMeta('13', 'перейти на вкладку История изменений');
  await ticketHistoryTab.waitForDisplayed();
  await ticketHistoryTab.click();
  await ticketHistoryIframe.waitForDisplayed();
  await browser.switchToFrame(1);
  //
  await browser.setMeta(
    '14',
    'проверить значения в верхней строке таблицы и сравнить их с ожидаемыми',
  );
  firstRowInHistory = await browser.$(TicketsLocators.TICKET_HISTORY_TABLE_FIRST_ROW);
  firstRowContent = await firstRowInHistory.getText();
  assert.include(
    firstRowContent,
    'Отправка письма',
    'нет триггера отправки письма в верхней строке',
  );
  assert.include(
    firstRowContent,
    'Письмо [Письмо отправлено] От кого: Группа тестирования CRM',
    'неверный отправитель',
  );
  assert.include(firstRowContent, 'Тема: ' + mailTheme, 'тема не совпадает с отправленной');
  assert.include(firstRowContent, 'Превью:  ' + mailText, 'текст не совпадает с отправленным');
  //
  await browser.setMeta('15', 'проверить, что записей в истории изменений три');
  historyRecords = await this.browser.$$(TicketsLocators.TICKET_HISTORY_TABLE_ROW);
  countRows(historyRecords, 3);
};
