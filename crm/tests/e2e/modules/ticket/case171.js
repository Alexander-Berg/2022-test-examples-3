const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //задаем параметры письма
  const mailData = {
    from: '<robot-space-odyssey@yandex-team.ru>',
    to: ['<robot-tcrm-test@yandex-team.ru>'],
    subject: 'case171',
    body: 'case 171 Signature',
    important: undefined,
  };

  //отсылаем письмо с указанными параметрами и переходим в созданный тикет
  await browser.sendMail(mailData);

  //Находим кнопку "Смежникам" на форме письма и нажимаем ее
  const smejnikamButton = await browser.$(TicketsLocators.SMEJNIKAM_BUTTON_ON_MAIL_FORM);
  await smejnikamButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  await smejnikamButton.click();
  //подождать пока откроется форма написания нового письма
  await browser.pause(3000);

  //найти в письме внутреннюю подпись
  const isInternalSignatureFindInMail = await browser.$(TicketsLocators.INTERNAL_SIGNATURE_IN_MAIL);
  await isInternalSignatureFindInMail.waitForDisplayed();
  let isInternalSignatureFoundInMail = await isInternalSignatureFindInMail.getText();
  assert.include(
    isInternalSignatureFoundInMail,
    'http://staff.yandex-team.ru/',
    'Internal signature is absent in the mail',
  );

  //очистить содержимое подписи в письме
  await isInternalSignatureFindInMail.clearValue();

  //найти и нажать кнопку Подпись
  const signatureButton = await browser.$(TicketsLocators.SIGNATURE_BUTTON);
  await signatureButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  await signatureButton.click();
  await browser.pause(1000);

  // Найти внутреннюю подпись в списке
  const isInternalSignatureFind = await browser.$(TicketsLocators.INTERNAL_SIGNATURE_IN_LIST);
  await isInternalSignatureFind.waitForDisplayed();
  const isInternalSignatureFindText = await isInternalSignatureFind.getText();

  // проверяем, что в списке подписей есть внутренняя подпись
  assert.include(
    isInternalSignatureFindText,
    'Внутренняя подпись',
    'Internal signature is absent in the list',
  );
  // нажать на Внутреннюю подпись в списке
  await isInternalSignatureFind.click();

  // Найти внутреннюю подпись в письме
  await isInternalSignatureFindInMail.waitForDisplayed();
  isInternalSignatureFoundInMail = await isInternalSignatureFindInMail.getText();

  //Проверяем, что в письмо подставлена внутренняя подпись (содержит ссылку на стафф)
  assert.include(
    isInternalSignatureFoundInMail,
    'http://staff.yandex-team.ru/',
    'Internal signature is absent in the mail',
  );
};
