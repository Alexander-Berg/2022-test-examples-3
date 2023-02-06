const { assert } = require('chai');

module.exports = async function() {
  //задаем параметры письма
  const mailData = {
    from: '<robot-space-odyssey@yandex-team.ru>',
    to: ['<robot-tcrm-test@yandex-team.ru>'],
    subject: 'case77',
    body: 'case 77 simple text',
    important: undefined,
  };
  //
  await this.browser.setMeta(
    '1',
    'послать письмо с указанными параметрами и перейти в созданный тикет',
  );
  await this.browser.sendMail(mailData);
  //
  await this.browser.setMeta('2', 'запомнить значения атрибутов тикета');
  const lineInTicket = await this.browser.getAttributeValue('Линия');
  const authorInTicket = await this.browser.getAttributeValue('Автор');
  const ticketType = await this.browser.getAttributeValue('Тип');
  const priorityInTicket = await this.browser.getAttributeValue('Приоритет');
  const queueInTicket = await this.browser.getAttributeValue('Очередь');
  //
  await this.browser.setMeta('3', 'проверить значения атрибутов');
  //
  await this.browser.setMeta('4', 'Линия = 1');
  assert.equal(String(lineInTicket), '1');
  //
  await this.browser.setMeta('5', 'Автор = CRM');
  assert.equal(authorInTicket, 'CRM');
  //
  await this.browser.setMeta('6', 'Приоритет = нормальный');
  assert.equal(priorityInTicket, 'Нормальный');
  //
  await this.browser.setMeta('7', 'Очередь = Отдел CRM (тест).Autotest');
  assert.equal(queueInTicket, 'Отдел CRM (тест).Autotest');
  //
  await this.browser.setMeta('8', 'Тип = Тикет');
  assert.equal(ticketType, 'Тикет');
};
