const { assert } = require('chai');
const { TicketsLocators } = require('../../pages/locators/tickets');
const { ROBOT_ODYSSEY_LOGIN } = require('../../constants/robotOdysseyData');
const waitAttributesUpdateTimeout = 30000; // таймаут для ожидания обновления атрибутов

const ticketName = `e2e-test... ${Math.random() * 1000}`;
const ticketQueue = 'Отдел CRM (тест).Autotest';

module.exports = async function() {
  const { browser } = this;

  //
  await browser.setMeta('1', 'создать новый тикет, переход в него идет автоматом');
  await browser.createNewTicket(ticketName, ticketQueue);
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
  await browser.setMeta('5', 'не уходя из этой вкладки поменять поля');
  //
  await browser.setMeta('5', 'Дедлайн');
  const deadlineInTicket = await browser.setDeadlineInIssues();
  //
  await browser.setMeta('5', 'Исполнитель');
  const assigneeInTicket = await browser.setAssigneeInTicket('Crmcrown Robot');
  //
  await browser.setMeta('5', 'Метки');
  const markInTicket = await browser.addMarkToTicket('Отметка для автотеста', ROBOT_ODYSSEY_LOGIN);
  //
  await browser.setMeta('5', 'Наблюдатели (добавляем робота Crmcrown Robot по умолчанию)');
  await browser.addFollowerToTicket('robot-crmcrown');

  //
  await browser.setMeta('6', 'закрыть текущую вкладку и переключиться на первую вкладку');
  await browser.closeWindow();
  await browser.switchToWindow(handles[0]);
  //
  await browser.setMeta(
    '7',
    'в течение waitAttributesUpdateTimeout секунд проверять изменение поля Исполнитель',
  );

  //
  await browser.setMeta(
    '8',
    'после обновления Исполнителя сравнить значения остальных полей с ожидаемыми',
  );
  const assigneeValue = await browser.$(TicketsLocators.TICKET_ASSIGNEE_VALUE);
  await assigneeValue.waitUntil(
    async () => {
      const text = await assigneeValue.getText();
      return text === assigneeInTicket;
    },
    {
      timeout: waitAttributesUpdateTimeout,
      interval: 3000,
    },
  );
  //
  assert.strictEqual(
    deadlineInTicket,
    await browser.getAttributeValue('Дедлайн'),
    'deadlines are not equal',
  );
  //
  assert.strictEqual(markInTicket, await browser.getAttributeValue('Метки'), 'tag was not changed');
  //
  assert.strictEqual(
    'Crmcrown Robot',
    await browser.getAttributeValue('Наблюдатели'),
    'follower was not changed',
  );
};
