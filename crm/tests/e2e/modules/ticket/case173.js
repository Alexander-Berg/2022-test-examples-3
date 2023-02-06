const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  //задаем параметры письма
  const mailData = {
    from: '<robot-space-odyssey@yandex-team.ru>',
    to: ['<robot-tcrm-test@yandex-team.ru>'],
    subject: 'case173',
    body: 'case 173 QueueSignature',
    important: undefined,
  };
  //
  await browser.setMeta(
    '1',
    'отправить письмо с указанными параметрами и перейти в созданный тикет',
  );
  await browser.sendMail(mailData);
  //
  await browser.setMeta('2', 'найти кнопку "Ответить" на форме письма и нажать ее');
  const answerButton = await browser.$(TicketsLocators.ANSWER_BUTTON_ON_MAIL_FORM);
  await answerButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  await answerButton.click();
  //
  await browser.setMeta('3', 'подождать, пока откроется форма написания нового письма');
  await browser.pause(3000);
  //
  await browser.setMeta('4', 'найти в письме подпись для очереди');
  const isQueueSignatureFindInMail = await browser.$(TicketsLocators.INTERNAL_SIGNATURE_IN_MAIL);
  await isQueueSignatureFindInMail.waitForDisplayed();
  let isQueueSignatureFoundInMail = await isQueueSignatureFindInMail.getText();
  assert.include(
    isQueueSignatureFoundInMail,
    'Signature for Autotest queue',
    'Queue signature is absent in the mail',
  );
  //
  await browser.setMeta('5', 'очистить содержимое подписи в письме');
  await isQueueSignatureFindInMail.clearValue();
  //
  await browser.setMeta('6', 'найти кнопку Подпись и нажать ее');
  const signatureButton = await browser.$(TicketsLocators.SIGNATURE_BUTTON);
  await signatureButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  await signatureButton.click();
  await browser.pause(1000);
  //
  await browser.setMeta('7', 'найти подпись для очереди в списке');
  const isQueueSignatureFind = await browser.$(TicketsLocators.QUEUE_SIGNATURE_IN_LIST);
  await isQueueSignatureFind.waitForDisplayed();
  const isQueueSignatureFindText = await isQueueSignatureFind.getText();

  //
  await browser.setMeta('8', 'проверить, что в списке подписей есть подпись для очереди');
  assert.include(
    isQueueSignatureFindText,
    'Подпись для очереди Autotest',
    'Queue signature is absent in the list',
  );
  //
  await browser.setMeta('9', 'нажать на подпись для очереди в списке');
  await isQueueSignatureFind.click();
  //
  await browser.setMeta('10', 'найти внутреннюю подпись в письме');
  await isQueueSignatureFindInMail.waitForDisplayed();
  isQueueSignatureFoundInMail = await isQueueSignatureFindInMail.getText();
  //
  await browser.setMeta('11', 'проверить, что в письмо подставлена подпись для очереди');
  assert.include(
    isQueueSignatureFoundInMail,
    'Signature for Autotest queue',
    'Queue signature is absent in the mail',
  );
};
