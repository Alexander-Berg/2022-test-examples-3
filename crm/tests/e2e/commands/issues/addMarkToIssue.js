const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function(markName) {
  const addMark = await this.$(IssuesLocators.ADD_MARK_BUTTON);
  await addMark.waitForDisplayed();
  await addMark.click();

  const newMark = await this.$(IssuesLocators.NEW_MARK_BUTTON);
  await newMark.waitForDisplayed();
  await newMark.click();

  const newMarkName = await this.$(IssuesLocators.NEW_MARK_NAME);
  await newMarkName.waitForDisplayed();
  await newMarkName.setValue(markName);

  const markAccess = await this.$(IssuesLocators.NEW_MARK_ACCESS);
  await markAccess.setValue(['sbalzitova', 'Enter']);

  const optionForAccess = await this.$(IssuesLocators.OPTION_FOR_ACCESS);
  await optionForAccess.waitForDisplayed();
  await optionForAccess.click();

  const greenColour = await this.$(IssuesLocators.NEW_MARK_GREEN_COLOUR);
  await greenColour.click();

  const saveMark = await this.$(IssuesLocators.SAVE_NEW_MARK);
  await saveMark.waitForEnabled();
  await saveMark.click();
};
