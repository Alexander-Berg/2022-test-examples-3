const { assert } = require('chai');
const { MassmailLocators } = require('./../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  //нажать на кнопку Шаблоны
  const templateList = await browser.$(MassmailLocators.TEMPLATE_LIST_BUTTON);
  await templateList.waitForClickable();
  await templateList.click();
  //нажать на имеющийся шаблон (у робота-Одиссея он один)
  const templateOption = await browser.$(MassmailLocators.TEMPLATE_OPTION);
  await templateOption.waitForClickable();
  await templateOption.click({ x: 5, y: 5 });
  await templateOption.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  //нажать на файл, приложенный к шаблону
  const attachedFile = await browser.$(MassmailLocators.ATTACHED_FILE);
  await attachedFile.waitForClickable();
  await attachedFile.click();

  //увидеть, что файл открылся
  const openedFile = await browser.$(MassmailLocators.OPENED_FILE);
  await openedFile.waitForDisplayed({ timeout: 5000, interval: 500 });

  //проверить, что нет ошибки при открытии файла,
  //и если есть, то свалить тест в ошибку
  let errorByOpeningFile = await browser.$(MassmailLocators.ERROR_BY_OPENING_FILE);
  const errorIsDisplayed = await errorByOpeningFile.isDisplayed();

  if (errorIsDisplayed) {
    assert.isTrue(errorIsDisplayed, 'error opening file in template');
  }

  //закрыть файл
  const closeFile = await browser.$(MassmailLocators.CLOSE_FILE);
  await closeFile.waitForClickable();
  await closeFile.click();

  //увидеть, что файл, приложенный к шаблону, остался на месте
  const isTemplateUsed = await attachedFile.waitForDisplayed();
  assert.isTrue(isTemplateUsed, 'file in template doesnt open');
};
