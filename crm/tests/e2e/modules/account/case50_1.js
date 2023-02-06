const { assert } = require('chai');
const { AccountsLocators } = require('./../../pages/locators/accounts');

module.exports = async function() {
  const { browser } = this;

  //предварительно в блоке BeforeEach создается новый контакт

  //найти блок Контакты на карточке аккаунта
  const savedContact = await browser.$(AccountsLocators.SAVED_CONTACT);
  await savedContact.waitForDisplayed();
  const isContactAdded = await savedContact.getText();
  assert.include(isContactAdded, 'robot-space-odyssey@yandex-team.ru', 'contact was not saved');
  assert.include(isContactAdded, '+79999999999', 'contact was not saved');
};
