const { assert } = require('chai');
const { ROBOT_ODYSSEY_LOGIN } = require('../../constants/robotOdysseyData');

module.exports = async function() {
  const { browser } = this;
  await browser.addMarkToTicket('Отметка для автотеста', ROBOT_ODYSSEY_LOGIN);

  await browser.pause(1000);

  const markText = await browser.$('.//span[text()="Отметка для автотеста"]');

  const isMarkCreated = await markText.waitForDisplayed();
  assert.isTrue(isMarkCreated, 'newly added mark is not found in personal marks');
};
