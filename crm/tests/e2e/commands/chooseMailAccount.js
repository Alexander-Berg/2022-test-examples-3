const MailLocators = require('../pages/locators/mail');

module.exports = async function(accountName) {
  const url = await this.getUrl();
  if (!url.includes('#/mail')) {
    throw new Error('chooseMailAccount was called outside mail module test');
  }

  //Если кнопка "Выбрать" для аккаунта в форме написания письма отображается, то нажать ее
  //если аккаунт уже был выбран, то нажать на кнопку "Изменить"
  const isChooseAccountVisible = await (await this.$(MailLocators.CHOOSE_ACCOUNT)).isDisplayed();
  let clickSelector;
  if (isChooseAccountVisible) {
    clickSelector = MailLocators.CHOOSE_ACCOUNT;
  } else {
    clickSelector = MailLocators.EDIT_ACCOUNT;
  }

  await (await this.$(clickSelector)).click();

  //подождать, пока отобразится поле ввода "Значение" на форме поиска аккаунта
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_INPUT)).waitForDisplayed();
  //в поле поиска ввести accountName
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_INPUT)).setValue(accountName);
  //нажать на кнопку "Искать по"
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_TYPE_SELECT)).click();
  //дождаться отображения значения "Любое поле" и выбрать его
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_ANY_TYPE)).waitForDisplayed();
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_ANY_TYPE)).click();
  //нажать кнопку Найти
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_FIND)).waitForEnabled();
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_FIND)).click();
  //находим нужный аккаунт и двойным кликом выбираем его
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_ACCOUNT_LINK)).waitForDisplayed();
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_ACCOUNT_LINK)).doubleClick();
  await this.pause(1000);
};
