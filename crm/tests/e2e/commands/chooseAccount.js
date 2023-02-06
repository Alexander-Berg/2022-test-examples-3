const { MailLocators } = require('../pages/locators/mail');

module.exports = async function(accountName) {
  //Предполагаем, что форма поиска аккаунта уже открыта, не важно, в каком окне

  //подождать, пока отобразится поле ввода "Значение" на форме поиска аккаунта
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_INPUT)).waitForDisplayed();
  //в поле поиска ввести accountName
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_INPUT)).setValue(accountName);

  //нажать на кнопку "Искать по"
  const findBySelected = await this.$(MailLocators.CHOOSE_ACCOUNT_TYPE_SELECT_TEXT);
  const findBySelectedValue = await findBySelected.getText();

  if (String(findBySelectedValue) !== 'Любое поле') {
    await (await this.$(MailLocators.CHOOSE_ACCOUNT_TYPE_SELECT)).click();

    //дождаться отображения значения "Любое поле" и выбрать его
    await (await this.$(MailLocators.CHOOSE_ACCOUNT_ANY_TYPE)).waitForDisplayed();
    await (await this.$(MailLocators.CHOOSE_ACCOUNT_ANY_TYPE)).click();
  }

  //нажать кнопку Найти
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_FIND)).waitForEnabled();
  await (await this.$(MailLocators.CHOOSE_ACCOUNT_FIND)).click();

  //находим нужный аккаунт и двойным кликом выбираем его
  await (await this.$(MailLocators.CHOOSE_FIRST_FINDED_ACCOUNT)).waitForDisplayed();

  await (await this.$(MailLocators.CHOOSE_FIRST_FINDED_ACCOUNT)).doubleClick();
  await this.pause(1000);
};
