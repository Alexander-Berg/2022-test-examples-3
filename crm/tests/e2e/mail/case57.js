const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');

module.exports = async function() {
  const mailData = {
    subject: 'test',
    text: 'hermione testing',
    important: true,
  };

  await this.browser.sendAndFindMail(mailData);
  await this.browser.openFirstInboxMail();

  const mailImp = await this.browser.$(MailLocators.FIRST_MAIL_IN_LIST);
  let isMailImportant = await this.browser.isMailImportant(mailImp);

  assert.strictEqual(
    isMailImportant,
    true,
    'В списке писем отсутствует флаг о том, что письмо важное',
  );
  const subjectText = await (await this.browser.$(MailLocators.PREVIEW_SUBJECT_FIELD)).getText();
  const previewText = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();

  assert.strictEqual(
    subjectText.includes(mailData.subject),
    true,
    'Поле "Тема" не соответствует отправленному',
  );
  assert.strictEqual(
    previewText.includes(mailData.text),
    true,
    'Тело письма не соответствует отправленному',
  );
};
