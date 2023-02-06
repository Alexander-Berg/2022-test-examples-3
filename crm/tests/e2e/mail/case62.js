const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');

module.exports = async function() {
  const mailData = {
    subject: 'test',
    text: 'case62',
  };
  //
  await this.browser.setMeta(
    '1',
    'отправить письмо в личную почту роботу и дождаться его появления',
  );
  await this.browser.sendAndFindMail(mailData);
  //
  await this.browser.setMeta('2', 'открыть первое письмо в списке');
  await this.browser.openFirstInboxMail();
  //
  await this.browser.setMeta('3', 'нажать на кнопку "Переместить в папку"');
  await (await this.browser.$(MailLocators.MOVE_TO_FOLDER_BUTTON)).click();
  //
  await this.browser.setMeta('4', 'увидеть список доступных папок');
  await (await this.browser.$(MailLocators.FOLDER_POPUP)).waitForDisplayed();
  //
  await this.browser.setMeta('5', 'нажать на Спам');
  await (await this.browser.$(MailLocators.FOLDER_POPUP_SPAM_OPTION)).click();
  await this.browser.pause(4000);
  //
  await this.browser.setMeta('6', 'перейти в папку Спам');
  await (await this.browser.$(MailLocators.SPAM_FOLDER_BUTTON)).click();
  await this.browser.pause(5000);
  //
  await this.browser.setMeta('7', 'выбрать первое письмо в списке папки Спам');
  await (await this.browser.$(MailLocators.FIRST_MAIL_IN_LIST)).click();
  //
  await this.browser.setMeta('8', 'запомнить тему письма');
  const subjectText = await (await this.browser.$(MailLocators.PREVIEW_SUBJECT_FIELD)).getText();
  //
  await this.browser.setMeta('9', 'запомнить текст письма');
  const previewText = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();

  //
  await this.browser.setMeta(
    '10',
    'убедиться, что тема и текст из того письма, которое было перемещено в папку Спам',
  );
  assert.strictEqual(
    subjectText.includes(mailData.subject),
    true,
    'Поле "Тема" не соответствует полю "Тема" после переноса в другую папку',
  );
  assert.strictEqual(
    previewText.includes(mailData.text),
    true,
    'Тело письма не соответствует телу письма после переноса в другую папку',
  );
};
