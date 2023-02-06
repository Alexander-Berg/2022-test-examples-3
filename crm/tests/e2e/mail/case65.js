const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');
const { SYSTEM_MESSAGE_DELAYED_SEND } = require('../constants/mail');

function compareMailData(formData) {
  return async function() {
    await (await this.browser.$(MailLocators.MAIL_PREVIEW)).waitForDisplayed({
      timeout: 3000,
    });
    const toText = await (await this.browser.$(MailLocators.PREVIEW_TO_FIELD)).getText();
    assert.strictEqual(
      toText.includes(formData.to),
      true,
      'Поле "Кому" не соответствует изначальному значению',
    );
    const subjectText = await (await this.browser.$(MailLocators.PREVIEW_SUBJECT_FIELD)).getText();
    assert.strictEqual(
      subjectText.includes(formData.subject),
      true,
      'Поле "Тема" не соответствует изначальному значению',
    );
    const bodyText = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();
    assert.strictEqual(
      bodyText.includes(formData.body),
      true,
      'Тело письма не соответствует изначальному значению',
    );
  };
}

module.exports = async function() {
  const formData = {
    to: 'robot-space-odyssey@yandex-team.ru',
    subject: `testcase 65...${Math.random() * 1000}`,
    body: `testcase 65...${Math.random() * 1000}`,
    account: 'yndx-agroroza',
  };

  //открыть первое письмо в списке
  await this.browser.openFirstInboxMail();
  //нажать в письме кнопку Ответить
  await (await this.browser.$(MailLocators.REPLY_MAIL_BUTTON)).click();
  //подождать, пока отобразится поле "Кому" в форме ответа
  await (await this.browser.$(MailLocators.FORM_INPUT_TO)).waitForDisplayed();
  //В поле "Кому" записать formData.to
  await (await this.browser.$(MailLocators.FORM_INPUT_TO)).setValue(formData.to);
  //в поле "Тема" записать formData.subject
  await (await this.browser.$(MailLocators.FORM_INPUT_SUBJECT)).setValue(formData.subject);
  //в тело письма записать formData.body
  await (await this.browser.$(MailLocators.FORM_BODY)).setValue(formData.body);

  //выбрать аккаунт с названием formData.account
  await this.browser.chooseMailAccount(formData.account);

  //нажать на кнопку отложенной отправки письма и дождаться появления календарика
  await (await this.browser.$(MailLocators.DELAYED_SEND_BUTTON)).click();
  await (await this.browser.$(MailLocators.DELAYED_SEND_TODAY_OPTION)).waitForDisplayed();
  //выбрать сегодняшнюю дату в календарике
  await (await this.browser.$(MailLocators.DELAYED_SEND_TODAY_OPTION)).click();
  //увеличить время отправки на +1 минуту
  await (await this.browser.$(MailLocators.DELAYED_SEND_INCREASE_MINUTE)).click();
  //нажать кнопку отправки письма
  await (await this.browser.$(MailLocators.SEND_BUTTON)).click();
  //увидеть сообщение о том. что письмо будет отправлено в такое-то время
  await (await this.browser.$(MailLocators.SYSTEM_MESSAGE)).waitForDisplayed();

  //сравнить текст из сообщения с ожидаемым результатом
  const messageText = await (await this.browser.$(MailLocators.SYSTEM_MESSAGE)).getText();
  assert.strictEqual(
    messageText.includes(SYSTEM_MESSAGE_DELAYED_SEND),
    true,
    'Сообщение об отправке не было показано',
  );

  await this.browser.pause(2000);
  //перейти в папку Исходящие
  await (await this.browser.$(MailLocators.DELAYED_OUTBOX_FOLDER_BUTTON)).click();
  await this.browser.pause(3000);

  //нажать на первое письмо в списке
  const firstMailInList = await this.browser.$(MailLocators.FIRST_MAIL_IN_LIST);
  await firstMailInList.click();
  await this.browser.pause(1000);

  //сравнить данные из этого письма с отправленными
  await compareMailData(formData).bind(this);

  //Найти надпись на письме "Будет отправлено"
  const willSendText = await (await this.browser.$(MailLocators.WILL_SEND_TEXT)).getText();
  assert.include(willSendText, 'Будет отправлено:', 'В письме нет пометики "Будет отправлено:"');

  //перейти в папку Отправленные
  await (await this.browser.$(MailLocators.OUTBOX_FOLDER_BUTTON)).click();

  //ждать, пока в списке появится отправленное письмo
  const firstMailSubject = await this.browser.$(MailLocators.MAIL_SUBJECT_IN_FIRST_MAIL_IN_LIST);
  //В течение 80 секунд с интервалом в 5 секунд проверяем, что в списке появляется отправленное письмо (находим по теме)
  await firstMailSubject.waitUntil(
    async () => {
      await this.browser.refresh();
      await firstMailInList.click();
      const text = await firstMailSubject.getText();
      return text === formData.subject;
    },
    {
      timeout: 120000,
      interval: 5000,
    },
  );

  //сравнить данные из этого письма с отправленными
  return compareMailData(formData).bind(this);
};
