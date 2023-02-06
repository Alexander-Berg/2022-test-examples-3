const { assert } = require('chai');

module.exports = async function() {
  const { browser } = this;

  await browser.writeCommentToIssue('Test comment left with automated test');

  const commentText = await browser.$('.//p[text()="Test comment left with automated test"]');
  const isCommentSaved = await commentText.waitForDisplayed();
  assert.isTrue(isCommentSaved, 'comment was not saved');
};
