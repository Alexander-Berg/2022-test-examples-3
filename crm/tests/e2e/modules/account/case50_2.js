const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;

  const editContact = await browser.$(AccountsLocators.EDIT_CONTACT_BUTTON);
  await editContact.waitForDisplayed();
  await editContact.click();

  const nameInput = await browser.$(AccountsLocators.NAME_INPUT);
  await nameInput.waitForDisplayed();
  //очистить поле (двойной клик и очистить)
  await nameInput.doubleClick();
  await browser.clearTextField(nameInput);
  //установить в поле другое значение
  await nameInput.setValue('Robot');

  const emailInput = await browser.$(AccountsLocators.EMAIL_INPUT);
  await emailInput.waitForDisplayed();
  //очистить поле
  await emailInput.doubleClick();
  await browser.clearTextField(emailInput);
  //установить в поле другое значение
  await emailInput.setValue('robot-space-odyssey@yandex-team.ru');

  const phoneInput = await browser.$(AccountsLocators.PHONE_INPUT);
  await phoneInput.waitForDisplayed();
  //очистить поле
  await phoneInput.doubleClick();
  await browser.clearTextField(phoneInput);
  //установить в поле другое значение
  await phoneInput.setValue('+79991111111');

  const saveContact = await browser.$(AccountsLocators.SAVE_CONTACT_BUTTON);
  await saveContact.waitForClickable();
  await saveContact.click();

  await nameInput.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  const savedContact = await browser.$(AccountsLocators.SAVED_CONTACT);
  await savedContact.waitForDisplayed();

  const isContactEdited = await savedContact.getText();
  assert.include(isContactEdited, 'Robot', 'contact name was not edited');
  assert.include(isContactEdited, '+79991111111', 'contact phone was not edited');
  assert.include(
    isContactEdited,
    'robot-space-odyssey@yandex-team.ru',
    'contact email was not edited',
  );
};
