const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function() {
  //нажать кнопку создания таймера в шапке тикета
  const createTimer = await this.$(TicketsLocators.CREATE_TIMER);
  await createTimer.waitForDisplayed();
  await createTimer.click();
  //в поле Уведомить ввести Crmcrown Robot
  const peopleNotify = await this.$(TicketsLocators.INPUT_FOR_PEOPLE_TO_NOTIFY);
  await peopleNotify.waitForDisplayed();
  await peopleNotify.setValue('Crmcrown Robot');
  //выбрать этого робота из выпадающего списка
  const robotSuggest = await this.$(TicketsLocators.ROBOT_TITLE_IN_SUGGEST);
  await robotSuggest.waitForDisplayed();
  await robotSuggest.click();
  //перейти в поле комментария
  const comment = await this.$('.//span[text()="Комментарий"]');
  await comment.click();
  //записать в комментарии 'Autocomment to timer'
  const timerComment = await this.$(TicketsLocators.TIMER_COMMENT);
  await timerComment.setValue('Autocomment to timer');
  //сохранить таймер
  const saveTimer = await this.$(TicketsLocators.SAVE_TIMER);
  await saveTimer.waitForEnabled();
  await saveTimer.click();
};
