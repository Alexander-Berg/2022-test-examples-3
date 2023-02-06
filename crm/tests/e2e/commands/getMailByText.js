const MailLocators = require('../pages/locators/mail');

module.exports = async function(searchText) {
  const url = await this.getUrl();
  if (!url.includes('#/mail')) {
    throw new Error('createDraftMail was called outside mail module test');
  }

  const mails = await this.$$(MailLocators.LIST_MAIL);
  for (let i = 0, length = mails.length; i < length; i++) {
    const mail = mails[i];
    const textNode = await mail.$(MailLocators.LIST_MAIL_PREVIEW_TEXT);
    const text = await textNode.getText();
    if (text === searchText) {
      return mail;
    }
  }
  return undefined;
};
