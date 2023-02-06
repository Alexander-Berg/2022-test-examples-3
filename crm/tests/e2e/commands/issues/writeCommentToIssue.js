const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function(commentText) {
  const writeComment = await this.$(IssuesLocators.WRITE_COMMENT);
  await writeComment.waitForDisplayed();
  await writeComment.click();

  const commentField = await this.$(IssuesLocators.COMMENT_FIELD);
  await commentField.waitForDisplayed();
  await commentField.setValue(commentText);

  const submitComment = await this.$(IssuesLocators.SUBMIT_COMMENT);
  await submitComment.waitForClickable();
  await submitComment.click();

  const commentBody = await this.$(IssuesLocators.COMMENT_DESCRIPTION_BODY);
  await commentBody.waitForDisplayed();
};
