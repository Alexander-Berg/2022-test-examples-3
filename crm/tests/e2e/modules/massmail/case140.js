const { assert } = require('chai');
const { MassmailLocators } = require('./../../pages/locators/massmail');
const massMailSender = 'MassmailSender';

module.exports = async function() {
  const { browser } = this;
  //в блоке Добавить нажимаем кнопку Аккаунт
  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();
  await addAccount.click();

  //вызываем процедуру поиска нужного Аккаунта
  await this.browser.chooseAccount(massMailSender);

  await browser.pause(2000);

  //в теме рассылки пишем "Autotest massmail"
  const newMailName = await browser.$(MassmailLocators.MAIL_TOPIC_INPUT);
  await newMailName.waitForDisplayed();
  await newMailName.setValue('Autotest massmail');

  //нажимаем кнопку Макросы
  const macrosList = await browser.$(MassmailLocators.MACROS_LIST_BUTTON);
  await macrosList.waitForClickable();
  await macrosList.click();

  //добавляем макрос {{Contact.FirstName}}
  const macrosFirst = await browser.$(MassmailLocators.MACROS_FIRST_NAME);
  await macrosFirst.waitForClickable();
  await macrosFirst.click();

  //видим кнопку Макросы (это шаг проверки, что после выбора макроса окно с макросами исчезает)
  await macrosList.waitForDisplayed();

  //дожидаемся, пока кнопка Просмотреть и отправить станет кликабельной
  const viewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await viewAndSend.waitForClickable();
  await browser.pause(500);
  await viewAndSend.click();

  //ждем, когда отобразится кнопка Отправить
  const sendMail = await browser.$(MassmailLocators.SEND_MAIL_BUTTON);
  await sendMail.waitForDisplayed();

  //видим, что отображается превью отправляемого письма
  const previewedMail = await browser.$(MassmailLocators.PREVIEWED_MAIL_BODY);
  await previewedMail.waitForDisplayed();

  //ищем в этом письме слово Антон, т.к. это результат работы макроса {{Contact.FirstName}} для первого контакта в Нулевом аккаунте
  const isNewMassMailOk = await previewedMail.getText();
  assert.include(isNewMassMailOk, 'crmtestmail2@yandex-team.ru', 'mail is not previewed');
  assert.include(isNewMassMailOk, 'Леопольд', 'macros is not applied');
};
