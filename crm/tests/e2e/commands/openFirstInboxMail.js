const MailLocators = require('../pages/locators/mail');

module.exports = async function() {
  //перейти в папку "Входящие"
  const inFolder = await this.$(MailLocators.INBOX_FOLDER_BUTTON);
  await inFolder.click();
  await this.pause(1000);

  //переходим в первое по списку письмо
  const firstMail = await this.$(MailLocators.FIRST_MAIL_IN_LIST);
  await firstMail.click();

  //видим, что письмо открылось
  const mailPrew = await this.$(MailLocators.MAIL_PREVIEW);
  await mailPrew.waitForDisplayed();
};
