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

  const likeButton = await browser.$(CategorizatorLocators.LIKE_BUTTON);
  await likeButton.waitForClickable({ timeout: 10000, interval: 500 }); //Увидеть кнопку "Лайк"
  await likeButton.click(); //кликнуть на кнопку "Лайк"

  const tooltipSendComment = await browser.$(CategorizatorLocators.TOOLTIP_SEND_COMMENT);
  await tooltipSendComment.waitForDisplayed(); //Увидеть тултип для отправки комментария

  const isTooltipVisible = await tooltipSendComment.getText(); //Почитать текст в попапе

  assert.include(isTooltipVisible, 'Оставить комментарий', 'Tooltip was not found on page');

  await likeButton.waitForDisplayed();
  await likeButton.click(); //отжать кнопку "Лайк"
};
