const path = require('path');
const { assert } = require('chai');
const { MassmailLocators } = require('./../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  const filePath = path.join(__dirname, './testFiles/clients.csv'); //файл на локальной машине
  const remotePath = await browser.uploadFile(filePath); //файл загружается на удаленную машину, на которой проходит тест
  const attachFile = await browser.$(MassmailLocators.ADD_ACCOUNT_FROM_FILE); //найти кнопку "Из файла"
  await attachFile.addValue(remotePath); //загрузить файл

  const addContact = await browser.$(MassmailLocators.ADD_CONTACT_ICON);
  await addContact.waitForClickable(); //дождаться, пока станет доступной кнопка добавления контакта
  await addContact.click(); //нажать кнопку добавления контакта

  const massmailContact = await browser.$(MassmailLocators.CRMAUTOTEST_MASSMAIL_CONTACT);
  await massmailContact.waitForDisplayed(); //увидеть в списке нужный контакт
  await massmailContact.click(); //выбрать этот контакт

  const viewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await viewAndSend.waitForEnabled(); //дождаться активности кнопки "Просмотреть и отправить"

  const freelanceeEmployee = await browser.$('//div[text()="Фрилансер/внештатный сотрудник"]');
  const isContactAddedFromFile = await freelanceeEmployee.waitForDisplayed(); //увидеть, что должность сотрудника такая

  assert.isTrue(isContactAddedFromFile, 'account was not added from file');
};
