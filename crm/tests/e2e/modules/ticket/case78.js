const { assert } = require('chai');
const accountByRK = 'Изменяем имя в Директе (serj-kruckow)';

module.exports = async function() {
  //задаем параметры письма
  const mailData = {
    from: '<robot-space-odyssey@yandex-team.ru>',
    to: ['<robot-tcrm-test@yandex-team.ru>'],
    subject: 'case78',
    body: 'case 78 РК 46268615',
    important: undefined,
  };

  //отсылаем письмо с указанными параметрами и переходим в созданный тикет
  await this.browser.sendMail(mailData);

  await this.browser.pause(3000);

  //вытащить значение атрибута Аккаунт
  const accountInTicket = await this.browser.getAttributeValue('Аккаунт');
  //проверяем, что он равен нужному
  assert.equal(accountInTicket, accountByRK);
};
