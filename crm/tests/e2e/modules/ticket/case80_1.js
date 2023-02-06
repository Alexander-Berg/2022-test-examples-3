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

  const saveComment = await browser.$(TicketsLocators.SUBMIT_COMMENT_TO_TICKET);
  await saveComment.waitForClickable();
  await saveComment.click();

  const commentBody = await browser.$(TicketsLocators.COMMENT_DESCRIPTION_BODY);
  await commentBody.waitForDisplayed();
  await browser.refresh();

  const commentText = await browser.$('.//p[text()="Test comment left with automated test"]');
  const isCommentSaved = await commentText.waitForDisplayed();
  assert.isTrue(isCommentSaved, 'comment was not saved');
};
