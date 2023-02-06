const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;

  const savedComment = await browser.$(AccountsLocators.SAVED_COMMENT);
  const isCommentAdded = await savedComment.getText();
  assert.include(isCommentAdded, 'Test comment to account', 'comment was not saved');
};
