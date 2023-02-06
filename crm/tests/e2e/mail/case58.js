const path = require('path');
const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');

module.exports = async function() {
  const mailData = {
    subject: 'test',
    text: 'hermione testing',
  };

  const formData = {
    to: 'yandcrmtest001@yandex.ru',
    body: 'test case 58',
    filePath: path.join(__dirname, '../modules/accountHistory/testFiles/image.png'),
    account: 'yndx-agroroza',
  };
  //отправить письмо со сформированными данными
  await this.browser.sendAndFindMail(mailData);
  //открыть первое письмо в списке
  await this.browser.openFirstInboxMail();

  await (await this.browser.$(MailLocators.MAIL_PREVIEW)).waitForDisplayed();
  const from = await (await this.browser.$(MailLocators.PREVIEW_FROM_FIELD)).getText();
  const subject = await (await this.browser.$(MailLocators.PREVIEW_SUBJECT_FIELD)).getText();
  const body = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();

  await (await this.browser.$(MailLocators.REPLY_MAIL_BUTTON)).click();
  await (await this.browser.$(MailLocators.FORM_INPUT_TO)).waitForDisplayed();
  await (await this.browser.$(MailLocators.FORM_BODY)).waitForDisplayed();

  const toValue = await (await this.browser.$(MailLocators.FORM_INPUT_TO)).getValue();
  assert.strictEqual(toValue.includes(from), true, 'Поле "Кому" не предзаполнено отправителем');

  const subjectValue = await (await this.browser.$(MailLocators.FORM_INPUT_SUBJECT)).getValue();
  assert.strictEqual(
    subjectValue.includes(subject),
    true,
    'Тема не совпадает с изначальной темой письма',
  );

  const bodyValue = await (await this.browser.$(MailLocators.FORM_BODY)).getText();
  assert.strictEqual(
    bodyValue.includes(body),
    true,
    'Тело письма не совпадает с телом в полученном письме',
  );
  await (await this.browser.$(MailLocators.FORM_INPUT_TO)).setValue(formData.to);
  await (await this.browser.$(MailLocators.FORM_BODY)).setValue(formData.body);
  //приложить файл
  const remotePath = await this.browser.uploadFile(formData.filePath);
  await (await this.browser.$(MailLocators.CHOOSE_FILES_INPUT)).addValue(remotePath);

  await this.browser.pause(3000);

  await (await this.browser.$(MailLocators.SEND_BUTTON)).click();
  await (await this.browser.$(MailLocators.SYSTEM_MESSAGE)).waitForDisplayed();
  const messageText = await (await this.browser.$(MailLocators.SYSTEM_MESSAGE)).getText();
  assert.strictEqual(
    messageText.includes('Необходимо заполнить поле Аккаунт'),
    true,
    'Отсутствует ошибка об отправке письма без аккаунта',
  );
  await this.browser.chooseMailAccount(formData.account);
  await (await this.browser.$(MailLocators.SEND_BUTTON)).click();
  await this.browser.pause(3000);
  await (await this.browser.$(MailLocators.SYSTEM_MESSAGE)).waitForDisplayed();

  const messageText2 = await (await this.browser.$(MailLocators.SYSTEM_MESSAGE)).getText();
  assert.strictEqual(
    messageText2.includes('Письмо отправлено'),
    true,
    'Отсутствует уведомление об успешной отправке письма после выбора аккаунта',
  );
  await (await this.browser.$(MailLocators.OUTBOX_FOLDER_BUTTON)).click();
  await this.browser.pause(3000);
  await (await this.browser.$(MailLocators.FIRST_MAIL_IN_LIST)).click();
  await this.browser.pause(3000);
  await (await this.browser.$(MailLocators.MAIL_PREVIEW)).waitForDisplayed();

  const to = await (await this.browser.$(MailLocators.PREVIEW_TO_FIELD)).getText();
  const body2 = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();

  assert.strictEqual(to.includes(formData.to), true, 'Письмо не соответствует отправленному');
  assert.strictEqual(body2.includes(formData.body), true, 'Письмо не соответствует отправленному');
};
