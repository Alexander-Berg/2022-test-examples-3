const { assert } = require('chai');
const { MassmailLocators } = require('./../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  const mailBody = await browser.$(MassmailLocators.MAIL_BODY);
  await mailBody.waitForDisplayed();

  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();

  const viewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await viewAndSend.isEnabled({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  const fromWhom = await browser.$(MassmailLocators.FROM_WHOM_BLOCK);
  const isNewMassMailOk = await fromWhom.getText();
  assert.include(
    isNewMassMailOk,
    'test-robot-space-odyssey@yandex-team.ru',
    'from whom block doesnt contain robot email',
  );
};
