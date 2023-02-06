const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;

  const editComment = await browser.$(AccountsLocators.EDIT_COMMENT_BUTTON);

  await editComment.waitForDisplayed({ timeout: 5000 });
  await editComment.click();

  const commentInput = await browser.$(AccountsLocators.COMMENT_INPUT_FOR_EDIT);

  await commentInput.waitForDisplayed({ timeout: 5000 });
  await commentInput.setValue(' edited');

  const saveComment = await browser.$(AccountsLocators.SAVE_COMMENT_BUTTON);
  await saveComment.waitForClickable({ timeout: 5000 });
  await saveComment.click();

  await saveComment.waitForClickable({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  const savedComment = await browser.$(AccountsLocators.SAVED_COMMENT);
  await savedComment.waitForDisplayed();

  const isCommentEdited = await savedComment.getText();

  assert.include(isCommentEdited, 'Test comment to account edited', 'comment was not edited');
};
