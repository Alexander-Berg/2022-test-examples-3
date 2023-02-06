//const { assert } = require('chai');

const { CategorizatorLocators } = require('./../../pages/locators/categorizator');

module.exports = async function() {
  const { browser } = this;

  const reqButton = await browser.$(CategorizatorLocators.REQUEST_BUTTON); //кнопка Заявка
  await reqButton.waitForDisplayed(); //увидеть кнопку Заявка
  await reqButton.click(); //нажать на кнопку Заявка

  const selectCategory = await browser.$(CategorizatorLocators.REQUEST_SELECT_CATEGORY); //ссылка "не выбрана" в Категории тикета
  await selectCategory.waitForDisplayed(); //ждем, пока откроется форма
  await selectCategory.click(); //нажимаем на "не выбрана"

  const categoryList = await browser.$(CategorizatorLocators.REQUEST_CATEGORY_WINDOW); //окно со строкой поиска категории
  await categoryList.waitForDisplayed(); //подождать, пока появится окно со списком доступных категорий
  await categoryList.setValue('Для заявок'); //ввести "для заявок" в поле поиска

  const categorySelect = await browser.$(CategorizatorLocators.REQUEST_CATEGORY_ACCOUNTS);
  await categorySelect.click(); //выбрать категорию Аккаунты

  const saveButton = await browser.$(CategorizatorLocators.REQUEST_SAVE_BUTTON);
  await saveButton.waitForDisplayed();
  await saveButton.click(); //нажать на кнопку Сохранить

  await browser.pause(50000);

  const secondLineForm = await browser.$(CategorizatorLocators.REQUEST_2LINE_FORM);
  await secondLineForm.waitForDisplayed(); //подождать, пока откроется форма создания тикета на вторую линию

  //в commands в  createSecondLineTicket уже есть готовый кусок для создания, скопировать оттуда
  //не открывается форма создания тикета на 2 линию

  await this.switchToFrame(1);
};
