const { assert } = require('chai');
const { MassmailLocators } = require('./../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  //нажать на Подпись
  const signatureList = await browser.$(MassmailLocators.SIGNATURE_LIST_BUTTON);
  await signatureList.waitForDisplayed();
  await signatureList.click();
  //выбрать первую из списка
  const firstSignature = await browser.$(MassmailLocators.FIRST_SIGNATURE);
  await firstSignature.waitForDisplayed();
  await firstSignature.click();
  //увидеть, что она отобразилась
  const beautifulSignature = await browser.$('//div[text()="Моя красивая подпись"]');
  await beautifulSignature.waitForDisplayed();
  //еще раз нажать на Подпись
  await signatureList.waitForDisplayed();
  await signatureList.click();
  //выбрать вторую из списка
  const secondSignature = await browser.$(MassmailLocators.SECOND_SIGNATURE);
  await secondSignature.waitForDisplayed();
  await secondSignature.click();
  //увидеть, что вторая отобразилась
  const favouriteSignature = await browser.$('//div[text()="MY FAVOURITE SIGNATURE"]');

  const isSignaturePresent = await favouriteSignature.waitForDisplayed();
  assert.isTrue(isSignaturePresent, 'signatures doesnt change');
};
