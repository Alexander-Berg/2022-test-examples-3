const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  const writeCommentToTicket = await browser.$(TicketsLocators.WRITE_COMMENT_TO_TICKET);
  await writeCommentToTicket.waitForDisplayed();
  await writeCommentToTicket.click();
  const commentField = await browser.$(TicketsLocators.COMMENT_FIELD_TO_TICKET);
  await commentField.waitForDisplayed();

  await commentField.setValue('Test comment left with automated test');

  const submitComment = await browser.$(TicketsLocators.SUBMIT_COMMENT_TO_TICKET);
  await submitComment.waitForClickable();
  await submitComment.click();

  const commentBody = await browser.$(TicketsLocators.COMMENT_DESCRIPTION_BODY);
  await commentBody.waitForDisplayed({ timeout: 5000, interval: 1000 });
  await browser.refresh();

  const deleteComment = await browser.$(TicketsLocators.DELETE_COMMENT_TO_TICKET);
  await deleteComment.waitForDisplayed({ timeout: 5000, interval: 1000 });
  await deleteComment.click();

  await browser.acceptAlert();
  await browser.refresh();

  const commentText = await browser.$('.//p[text()="Test comment left with automated test"]');
  const isNewCommentDeleted = await commentText.isExisting();
  assert.isFalse(isNewCommentDeleted, 'comment was not deleted');
};
