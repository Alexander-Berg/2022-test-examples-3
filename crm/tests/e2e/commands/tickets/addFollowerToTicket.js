//const { browserName } = require('react-device-detect');
const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function(followerName) {
  // предполагается, что все действия происходят уже внутри какого-то тикета
  //нажать кнопку Наблюдатели
  const followersField = await this.$(TicketsLocators.FOLLOWERS_FIELD);
  await followersField.waitForDisplayed();
  await followersField.click();
  //в поле ввода ввести followerName и подождать, пока список обновится
  const followersInput = await this.$(TicketsLocators.FOLLOWERS_INPUT);
  await followersInput.waitForDisplayed({ timeout: 5000, interval: 500 });

  await followersInput.setValue(followerName);
  await this.pause(1000);
  //найти робота в списке и кликнуть на него
  const robotSuggest = await this.$('.//span[text()="' + followerName + '"]');
  await robotSuggest.waitForDisplayed({ timeout: 5000, interval: 500 });
  await robotSuggest.click();
  //подождать, пока робот отобразится в выбранных наблюдателях
  await robotSuggest.waitForDisplayed({ timeout: 5000, interval: 500 });
  //выйти из режима редактирования поля
  await followersInput.setValue('Escape');
  await this.pause(2000);
};
