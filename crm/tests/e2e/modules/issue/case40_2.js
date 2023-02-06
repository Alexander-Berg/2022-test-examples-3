const { assert } = require('chai');
const { IssuesLocators } = require('./../../pages/locators/issues');
const markName = `mark ${Math.random() * 1000}`;

module.exports = async function() {
  const { browser } = this;

  //добавить метку с названием markName
  await browser.addMarkToIssue(markName);
  //удалить метку
  const deleteMark = await browser.$(IssuesLocators.DELETE_MARK_FROM_ISSUE);
  await deleteMark.waitForClickable();
  await deleteMark.click();
  //дождаться, пока метка удалится
  const isMarkDeleted = await deleteMark.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });
  assert.isTrue(isMarkDeleted, 'newly added mark is still on the page');

  //снова нажать на кнопку добавления метки
  const addMark = await browser.$(IssuesLocators.ADD_MARK_BUTTON);
  await addMark.waitForDisplayed();
  await addMark.click();
  //дождаться, пока окно отобразится
  const newMark = await browser.$(IssuesLocators.NEW_MARK_BUTTON);
  await newMark.waitForDisplayed();
  //заполнить поле поиска значением markName
  const markNameInput = await browser.$(IssuesLocators.FIND_MARK_BUTTON);
  await markNameInput.waitForDisplayed();
  await markNameInput.setValue(markName);
  //убедиться, что эта метка найдена и отображается в списке
  const firstMarkName = await browser.$(IssuesLocators.FIRST_MARK_IN_LIST);
  await firstMarkName.waitForDisplayed();
  const mName = await firstMarkName.getText();
  assert.include(mName, markName);
};
