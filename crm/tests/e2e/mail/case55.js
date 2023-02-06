const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');

module.exports = async function() {
  const mailData = {
    subject: 'test',
    text: 'Номер РК 55318925',
  };

  await this.browser.sendAndFindMail(mailData);
  await this.browser.openFirstInboxMail();
  const { strictEqual } = assert;

  const subjectText = await (await this.browser.$(MailLocators.PREVIEW_SUBJECT_FIELD)).getText();
  const previewText = await (await this.browser.$(MailLocators.PREVIEW_BODY)).getText();
  const isAttributesButtonVisible = await (
    await this.browser.$(MailLocators.ATTRIBUTES_BUTTON)
  ).isDisplayed();
  const isHistoryButtonVisible = await (
    await this.browser.$(MailLocators.ACCOUNT_HISTORY_BUTTON)
  ).isDisplayed();
  const isFilesButtonVisible = await (
    await this.browser.$(MailLocators.ACCOUNT_FILES_BUTTON)
  ).isDisplayed();
  const isAccountNameVisible = await (
    await this.browser.$(MailLocators.FIRST_MAIL_ACCOUNT_NAME)
  ).isDisplayed();
  const accountName = await (await this.browser.$(MailLocators.FIRST_MAIL_ACCOUNT_NAME)).getText();

  //проверка того, что параметры письма совпадают с отправленными
  strictEqual(isAttributesButtonVisible, true);
  strictEqual(isHistoryButtonVisible, true);
  strictEqual(isFilesButtonVisible, true);
  strictEqual(subjectText.includes(mailData.subject), true);
  strictEqual(previewText.includes(mailData.text), true);
  strictEqual(isAccountNameVisible, true);
  strictEqual(accountName, 'yapju');

  await this.browser;
  await (await this.browser.$(MailLocators.ATTRIBUTES_BUTTON)).click();
  await (await this.browser.$(MailLocators.ATTRIBUTE_ACCOUNT_BUTTON)).waitForDisplayed();

  const isAttributeAccountNameVisible = await (
    await this.browser.$(MailLocators.ATTRIBUTE_ACCOUNT_NAME)
  ).isDisplayed();

  const attributeAccountNameText = await (
    await this.browser.$(MailLocators.ATTRIBUTE_ACCOUNT_NAME)
  ).getText();

  strictEqual(isAttributeAccountNameVisible, true);
  strictEqual(attributeAccountNameText.includes('Япью Ряженку (yapju)'), true);

  await this.browser;
  await (await this.browser.$(MailLocators.ACCOUNT_HISTORY_BUTTON)).click();
  return this.browser.pause(5000);
};
