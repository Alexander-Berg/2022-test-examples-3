const Color = require('color');
const MailLocators = require('../pages/locators/mail');
const { IMPORTANT_FLAG_COLOR } = require('../constants/mail');

module.exports = async function(mail) {
  const url = await this.getUrl();
  if (!url.includes('#/mail')) {
    throw new Error('createDraftMail was called outside mail module test');
  }

  //найти флажок важности в превью письма
  const importantFlag = await mail.$(MailLocators.MAIL_IMPORTANT_FLAG);
  //определить цвет флажка важности
  const color = Color((await importantFlag.getCSSProperty('color')).value);
  const hex = color.hex().toLowerCase();
  //вернуть true, если флажок цвета IMPORTANT_FLAG_COLOR, и false в противном случае
  return hex === IMPORTANT_FLAG_COLOR;
};
