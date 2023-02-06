const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;
  //нажать на Очередь в атрибутах
  const queueField = await browser.$(TicketsLocators.QUEUE_FIELD);
  await queueField.waitForDisplayed();
  await queueField.click();
  //не удаляя имеющееся значение, ввести 'МКС: Смарт-баннеры'
  const queueInput = await browser.$(TicketsLocators.QUEUE_INPUT);
  await queueInput.waitForDisplayed();
  await queueInput.setValue('МКС: Смарт-баннеры');
  //выбрать эту очередь из выпадающего списка, она сохранится
  const queueForChange = await browser.$(TicketsLocators.QUEUE_FOR_CHANGE);
  await queueForChange.waitForDisplayed();
  await queueForChange.click();
  await browser.pause(2000);

  //вытащить значение атрибута Очередь
  const queueAttribute = await browser.getAttributeValue('Очередь');
  //сравнить его с МКС: Смарт-баннеры
  assert.equal(queueAttribute, 'МКС: Смарт-баннеры', 'queue is not in changed');
};
