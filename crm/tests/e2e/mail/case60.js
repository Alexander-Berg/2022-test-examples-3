const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');

module.exports = async function() {
  const formData = {
    account: 'yndx-agroroza',
    to: 'yandcrmtest001@yandex.ru',
    subject: `subject:${Math.random() * 1000}`,
    body: `text... ${Math.random() * 1000}`,
  };

  //создать черновик
  await this.browser.createDraftMail(formData);
  //перейти в папку Черновики
  await (await this.browser.$(MailLocators.DRAFT_FOLDER_BUTTON)).click();
  await this.browser.pause(2000);
  //перейти в первое письмо из списка
  await (await this.browser.$(MailLocators.FIRST_MAIL_IN_LIST)).click();
  //нажать на кнопку "Открыть черновик"
  await (await this.browser.$(MailLocators.OPEN_DRAFT_BUTTON)).waitForDisplayed({ timeout: 3000 });
  await (await this.browser.$(MailLocators.OPEN_DRAFT_BUTTON)).click();
  await this.browser.pause(2000);

  //увидеть ранее выбранный аккаунт
  await (await this.browser.$(MailLocators.CHOOSED_ACCOUNT_NAME)).waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });
  //проверяем, что тэг проставился верно
  await (await this.browser.$(MailLocators.CHOOSED_TAG)).waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  //
  const accountName = await (await this.browser.$(MailLocators.CHOOSED_ACCOUNT_NAME)).getText();
  const to = await (await this.browser.$(MailLocators.FORM_INPUT_TO)).getValue();
  const subject = await (await this.browser.$(MailLocators.FORM_INPUT_SUBJECT)).getValue();
  const body = await (await this.browser.$(MailLocators.FORM_BODY)).getText();

  assert.strictEqual(accountName.includes(formData.account), true, 'Аккаунт не выбран');
  assert.strictEqual(to.includes(formData.to), true, 'Не заполнено поле "Кому"');
  assert.strictEqual(subject, formData.subject, 'Не заполнено поле "Тема"');
  assert.strictEqual(body.includes(formData.body), true, 'Не заполнено тело письма');

  await this.browser;
  await (await this.browser.$(MailLocators.SEND_BUTTON)).click();
  await (await this.browser.$(MailLocators.FORM_BODY)).waitForDisplayed({
    timeout: 10000,
    reverse: true,
  });
  await (await this.browser.$(MailLocators.OUTBOX_FOLDER_BUTTON)).click();
  await this.browser.pause(3000);

  await (await this.browser.$(MailLocators.FIRST_MAIL_IN_LIST)).click();
  await this.browser.pause(500);

  const to2 = await (await this.browser.$(MailLocators.PREVIEW_TO_FIELD)).getText();
  const subject2 = await (await this.browser.$(MailLocators.PREVIEW_SUBJECT_FIELD)).getText();
  const body2 = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();

  assert.strictEqual(
    to2.includes(formData.to),
    true,
    'Поле "Кому" в отправленном письме не соответствует полю "Кому" в черновике этого письма',
  );
  assert.strictEqual(
    subject2.includes(formData.subject),
    true,
    'Поле "Тема" в отправленном письме не соответствует полю "Тема" в черновике этого письма',
  );
  assert.strictEqual(
    body2.includes(formData.body),
    true,
    'Тело письма не соответствует телу письма в черновике этого письма',
  );

  return this.browser;
};
