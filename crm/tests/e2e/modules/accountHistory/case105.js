const path = require('path');
const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');
const { AccountHistoryLocators } = require('./../../pages/locators/accountHistory');

module.exports = async function() {
  const { browser } = this;

  // https://webdriver.io/docs/api/browser/uploadFile/
  const filePath = path.join(__dirname, './testFiles/image.png');
  const remotePath = await browser.uploadFile(filePath);
  const attachFile = await browser.$(AccountsLocators.ATTACH_FILE_TO_COMMENTS);
  await attachFile.addValue(remotePath);

  const saveComment = await browser.$(AccountsLocators.SAVE_COMMENT_BUTTON);
  await saveComment.waitForDisplayed();
  await saveComment.click();

  const savedComment = await browser.$(AccountsLocators.SAVED_COMMENT);
  await savedComment.waitForDisplayed();
  await browser.refresh();
  await savedComment.waitForDisplayed();

  const accountFiles = await browser.$(AccountHistoryLocators.ACCOUNT_FILES_BUTTON);
  await accountFiles.waitForDisplayed();
  await accountFiles.click();

  const downloadFile = await browser.$(AccountHistoryLocators.DOWNLOAD_FILE);
  await downloadFile.waitForDisplayed();

  const fileSize = await browser.$(AccountHistoryLocators.FILE_SIZE);
  await fileSize.waitForDisplayed();

  const openFile = await browser.$(AccountHistoryLocators.OPEN_FILE);
  await openFile.waitForDisplayed();
  await openFile.click();

  const openedFile = await browser.$(AccountHistoryLocators.OPENED_FILE);
  await openedFile.waitForDisplayed();

  const closeFile = await browser.$(AccountHistoryLocators.CLOSE_FILE);
  await closeFile.waitForDisplayed();

  const fileBody = await browser.$(AccountHistoryLocators.ACCOUNT_FILES_BODY);

  const isFileSeen = await fileBody.getText();
  assert.include(
    isFileSeen,
    'Комментарий к аккаунту',
    'file source was not seen in account history',
  );
  assert.include(isFileSeen, 'image.png', 'file name was not seen in account history');
  assert.include(isFileSeen, '(1,8 Мб)', 'file size was not seen in account history');
};
