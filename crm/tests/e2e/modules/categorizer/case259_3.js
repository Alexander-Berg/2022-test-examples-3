const { assert } = require('chai');

const { TicketsLocators } = require('./../../pages/locators/tickets');
const { CategorizatorLocators } = require('./../../pages/locators/categorizator');

module.exports = async function() {
  const { browser } = this;

  const lastTicket = await browser.$(TicketsLocators.LATEST_CREATED_TICKET); //последний созданный тикет
  await lastTicket.waitForDisplayed();
  await lastTicket.click();

  const openedTab = await browser.$(TicketsLocators.TAB_ATTRIBUTES_OPENED); //Вкладка открыта?  Добавить проверку
  await openedTab.waitForDisplayed();

  const attrCat = await browser.$(TicketsLocators.ATTRIBUTE_CATEGORY);
  await attrCat.waitForDisplayed(); //Увидеть поле "Категория" на панели Атрибуты
  await attrCat.click(); //Нажать на поле "Категория"

  const searchField = await browser.$(CategorizatorLocators.SEARCH_FIELD);
  await searchField.waitForDisplayed(); //Увидеть поисковую строку
  await searchField.click(); //Кликнуть на поисковую строку
  await searchField.setValue('Подсказка 1'); //ввести название категории с подсказкой

  const searchRes = await browser.$(CategorizatorLocators.SEARCH_RESULTS_LIST);
  await searchRes.waitForDisplayed();
  await searchRes.click(); //Выделить найденную категорию

  await browser.pause(2000);

  const expandButton = await browser.$(CategorizatorLocators.EXPAND_BUTTON);
  await expandButton.waitForDisplayed(); //Увидеть кнопку открыть подсказку на весь экран
  await expandButton.click(); //Нажать кнопку открыть подсказку на весь экран

  await browser.pause(2000);

  const dislikeExpandButton = await browser.$(CategorizatorLocators.DISLIKE_EXPAND_BUTTON);
  await dislikeExpandButton.waitForClickable({ timeout: 20000, interval: 1000 }); //Увидеть кнопку "дизЛайк"
  await dislikeExpandButton.click(); //кликнуть на кнопку "дизЛайк"

  const tooltipSendComment = await browser.$(CategorizatorLocators.TOOLTIP_SEND_COMMENT);
  await tooltipSendComment.waitForDisplayed({ timeout: 10000, interval: 500 }); //Увидеть тултип для отправки комментария

  const isTooltipVisible = await tooltipSendComment.getText(); //Почитать текст в попапе

  assert.include(isTooltipVisible, 'Оставить комментарий', 'Tooltip was not found on page');

  await dislikeExpandButton.waitForClickable({ timeout: 10000, interval: 500 });
  await dislikeExpandButton.click(); //отжать кнопку "дизЛайк"
};
