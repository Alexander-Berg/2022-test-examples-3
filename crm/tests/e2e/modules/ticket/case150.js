const { assert } = require('chai');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  const { browser } = this;

  const categoryField = await browser.$(TicketsLocators.CATEGORY_FIELD);
  await categoryField.waitForDisplayed();
  await categoryField.click();

  const removeCategory = await browser.$(TicketsLocators.REMOVE_CATEGORY); //
  await removeCategory.waitForDisplayed();
  await removeCategory.click();

  const selectNewCategory = await browser.$(TicketsLocators.SELECT_NEW_CATEGORY);
  await selectNewCategory.waitForDisplayed();
  await selectNewCategory.click();

  const categorySearch = await browser.$(TicketsLocators.SEARCH_CATEGORY_INPUT);
  await categorySearch.waitForDisplayed();
  await categorySearch.setValue('Причина autotest');

  await browser.pause(3000);

  const categoryForChange = await browser.$(TicketsLocators.CATEGORY_FOR_CHANGE);
  await categoryForChange.waitForDisplayed();
  await categoryForChange.click();

  const saveCategory = await browser.$(TicketsLocators.SAVE_CATEGORY);
  await saveCategory.waitForDisplayed();
  await saveCategory.click();

  await browser.pause(500);
  await saveCategory.waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  const latestTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET);
  const isCategoryChanged = await latestTicket.getText();

  assert.include(
    isCategoryChanged,
    'Категории для Отдел CRM (тест) autotest - Причина autotest',
    'category was not changed',
  );
};
