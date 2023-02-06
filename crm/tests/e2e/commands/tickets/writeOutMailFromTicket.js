const { TicketsLocators } = require('../../pages/locators/tickets');

module.exports = async function(mailText, mailTo, mailTheme) {
  // подождать, пока отобразится кнопка "Написать письмо" и нажать на нее
  const writeMailButton = await this.$(TicketsLocators.WRITE_MAIL_TO_TICKET);
  await writeMailButton.waitForDisplayed({ timeout: 3000 });
  await writeMailButton.click();
  // подождать, пока отобразится форма написания письма
  const mailBody = await this.$(TicketsLocators.MAIL_BODY);
  await mailBody.waitForDisplayed({ timeout: 10000, interval: 500 });
  // в поле Кому ввести тестовый адрес
  const writeMailTo = await this.$(TicketsLocators.WRITE_MAIL_FIELD_TO);
  await writeMailTo.waitForDisplayed();
  await writeMailTo.setValue(mailTo);
  // в поле Тема ввести тестовую тему
  const writeMailTheme = await this.$(TicketsLocators.WRITE_MAIL_FIELD_THEME);
  await writeMailTheme.waitForDisplayed();
  await writeMailTheme.setValue(mailTheme);
  // в тело письма написать что-нибудь
  await mailBody.setValue(mailText);
  // здесь нужно подождать, иначе черновик не сохранится
  await this.pause(2000);

  // нажать кнопку Отправить
  const mailSendButton = await this.$(TicketsLocators.WRITE_MAIL_SEND_BUTTON);
  await mailSendButton.waitForDisplayed({ timeout: 10000, interval: 500 });
  await mailSendButton.click();
  // дождаться, пока форма письма закроется
  await mailSendButton.waitForDisplayed({ timeout: 10000, interval: 500, reverse: true });
};
