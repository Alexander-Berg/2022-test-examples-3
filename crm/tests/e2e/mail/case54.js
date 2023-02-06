const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');
const { NULL_ACCOUNT_ATTRUBUTE_TEXT } = require('../constants/mail');

module.exports = async function() {
  //задаем тему и текст письма
  const mailData = {
    subject: 'test',
    text: `hermione testing ${Math.random()}`,
  };

  //отсылаем письмо с указанными темой и текстом
  await this.browser.sendAndFindMail(mailData);

  //открываем первое письмо из списка
  await this.browser.openFirstInboxMail();

  //открываем плашку Атрибуты справа
  const attrButton = await this.browser.$(MailLocators.ATTRIBUTES_BUTTON);
  await attrButton.click();

  //берем значение с кнопки "выбрать" из поля аккаунта в атрибутах
  const attrAccButton = await this.browser.$(MailLocators.ATTRIBUTE_ACCOUNT_BUTTON);
  await attrAccButton.waitForDisplayed();
  const buttonText = await attrAccButton.getText();

  //берем тему письма
  const subjectText = await (await this.browser.$(MailLocators.PREVIEW_SUBJECT_FIELD)).getText();

  //берем тело письма
  const previewText = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();

  //проверяем, что текст кнопки, а также тема и тело письма совпадают с отправленными
  assert.strictEqual(buttonText, NULL_ACCOUNT_ATTRUBUTE_TEXT);
  assert.strictEqual(subjectText.includes(mailData.subject), true);
  assert.strictEqual(previewText.includes(mailData.text), true);
};
