const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  const { browser } = this;

  await browser.writeCommentToIssue('Test comment left with automated test');
  const deleteComment = await browser.$(IssuesLocators.DELETE_COMMENT);
  await deleteComment.waitForDisplayed();
  await deleteComment.click();
  await browser.acceptAlert();
  await browser.refresh();

  const commentText = await browser.$('.//p[text()="Test comment left with automated test"]');
  const isNewCommentDeleted = await commentText.isExisting();
  assert.isFalse(isNewCommentDeleted, 'comment was not deleted');
};
