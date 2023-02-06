const MailLocators = require('../pages/locators/mail');

module.exports = async function(formData) {
  const url = await this.getUrl();
  if (!url.includes('#/mail')) {
    throw new Error('createDraftMail was called outside mail module test');
  }

  await (await this.$(MailLocators.WRITE_MAIL)).waitForDisplayed();
  await (await this.$(MailLocators.WRITE_MAIL)).click();
  await (await this.$(MailLocators.MAIL_FORM)).waitForDisplayed();
  await this.chooseMailAccount(formData.account);
  await (await this.$(MailLocators.FORM_INPUT_TO)).setValue(formData.to);
  await (await this.$(MailLocators.FORM_INPUT_SUBJECT)).setValue(formData.subject);
  await (await this.$(MailLocators.TAG_ADD)).click();
  await (await this.$(MailLocators.TAG_MENU)).waitForDisplayed();
  await (await this.$(MailLocators.TAG_FIRST)).click();
  await (await this.$(MailLocators.FORM_BODY)).setValue(formData.body);
  await this.pause(1000);
  await (await this.$(MailLocators.SAVE_DRAFT_BUTTON)).click();
  await (await this.$(MailLocators.MAIL_NOT_SELECTED)).waitForDisplayed();
};
