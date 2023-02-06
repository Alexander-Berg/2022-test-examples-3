const { assert } = require('chai');
const { MassmailLocators } = require('./../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  const templateList = await browser.$(MassmailLocators.TEMPLATE_LIST_BUTTON);
  await templateList.waitForDisplayed();
  await templateList.click();

  const templateOption = await browser.$(MassmailLocators.TEMPLATE_OPTION);
  await templateOption.waitForClickable();
  await templateOption.click({ x: 5, y: 5 });
  await templateOption.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  const templateText = await browser.$('//div[text()="«the time is now»"]');
  await templateText.waitForDisplayed();

  const templateFile = await browser.$(MassmailLocators.ATTACHED_FILE);
  await templateFile.waitForDisplayed();

  const isTemplateUsed = await templateFile.getText();
  assert.include(isTemplateUsed, 'image.png', 'template is not applied');
};
