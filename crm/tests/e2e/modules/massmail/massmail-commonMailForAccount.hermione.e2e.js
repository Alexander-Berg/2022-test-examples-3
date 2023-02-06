const { assert } = require('chai');
const { MassmailLocators } = require('../../pages/locators/massmail');
const { MailLocators } = require('../../pages/locators/mail');
const findTicketTimeout = 40000; //таймаут нахождения созданного по письму тикета

module.exports = async function() {
  const { browser } = this;

  //Аккаунт, который будем добавлять в рассылку
  const accountCommonEmail = 'Общее письмо на аккаунт';
  const massmailTheme = `e2e-test518 ${Math.random()}`;
  const copyRecipientsList =
    '"robot-tcrm-test-sp@yandex-team.ru" <robot-tcrm-test-sp@yandex-team.ru>, "test-robot-space-odyssey@yandex-team.ru" <test-robot-space-odyssey@yandex-team.ru>, "yandcrmtest001@yandex.ru" <yandcrmtest001@yandex.ru>';

  //Форма создания новой рассылки открыта
  await browser.setMeta('1', 'Добавить уникальную тему');
  const mailTopic = await browser.$(MassmailLocators.MAIL_TOPIC_INPUT);
  await mailTopic.waitForDisplayed();
  await mailTopic.click();
  await mailTopic.setValue(massmailTheme);

  await browser.setMeta('2', 'Добавить тело письма');
  const mailBody = await browser.$(MassmailLocators.MAIL_BODY_INPUT);
  await mailBody.waitForDisplayed();
  await mailBody.click();
  await mailBody.setValue(massmailTheme);

  await browser.setMeta('3', 'нажать кнопку "Добавить Аккаунт"');
  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();
  await addAccount.click();
  await browser.chooseAccount(accountCommonEmail);

  await browser.setMeta('4', 'нажать на "Выбрать тип контакта"');
  const contactType = await browser.$(MassmailLocators.CONTACT_TYPE_LISTBOX);
  await contactType.waitForDisplayed();
  await contactType.click();

  await browser.setMeta('5', 'Выбрать из списка тип "Все"');
  const contactTypeAll = await browser.$(MassmailLocators.CONTACT_TYPE_ALL);
  await contactTypeAll.waitForDisplayed({
    timeout: 5000,
    interval: 500,
  });
  await contactTypeAll.click();

  await browser.setMeta('6', 'Нажать чекбокс "Общее письмо для аккаунта"');
  //Находим чекбокс на странице
  const checkboxCommonMail = await browser.$(MassmailLocators.COMMON_MAIL_FOR_ACCOUNT_CHECKBOX);
  await checkboxCommonMail.click();

  const checkboxIsSelected = await browser.$(
    MassmailLocators.COMMON_MAIL_FOR_ACCOUNT_CHECKBOX + ' input[aria-checked="true"]',
  );

  //Проверяем состояние чекбокса
  await browser.setMeta('7', 'Проверяем, что чекбокс "Разрешить повторение адресантов" выбран');
  await checkboxIsSelected.waitForExist();

  //Если чекбокс не выбран, кликнуть на него
  if (!checkboxIsSelected) {
    await checkboxCommonMail.click();
  }

  await browser.setMeta('8', 'Кликнуть Посмотреть и Отправить');
  const buttonViewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await buttonViewAndSend.waitForDisplayed();
  await buttonViewAndSend.click();

  await browser.setMeta('9', 'Кликнуть Отправить');
  const buttonSend = await browser.$(MassmailLocators.SEND_MAIL_BUTTON_BORDER);
  await buttonSend.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });
  await buttonSend.click();

  await browser.setMeta('10', 'Увидеть сообщение об успешной отправке');
  const messageSent = await browser.$(MassmailLocators.MESSAGE_MASSMAIL_SENT);
  await messageSent.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  await browser.pause(3000);

  await browser.setMeta('11', 'Перейти в модуль Почта');
  const moduleMail = await browser.$(MailLocators.MODULE_MAIL);
  await moduleMail.click();

  //в течение указанного таймаута findTicketTimeout ищем это письмо в списке
  await browser.setMeta('12', 'Найти отправленное письмо в списке Входящие');
  const mailSubject = await browser.$(MailLocators.THEME_IN_FIRST_MAIL_IN_LIST);
  //const updateButton = await browser.$(MailLocators.CHECK_NEW_EMAIL_BUTTON);

  await mailSubject.waitUntil(
    async () => {
      await browser.refresh();
      const text = await mailSubject.getText();
      return text === massmailTheme;
    },
    {
      timeout: findTicketTimeout,
      interval: 5000,
    },
  );

  await browser.setMeta('13', 'Кликнуть на найденное письмо');
  await mailSubject.click();

  await browser.setMeta('14', 'Забрать текст из копии письма и сравнить с ожидаемым');
  const themeReceivedMail = await browser.$(MailLocators.COPY_RECIPIENTS_LIST);
  await themeReceivedMail.waitForDisplayed();
  const recipientsList = await themeReceivedMail.getText();

  assert.equal(recipientsList, copyRecipientsList, 'emails are different');
};
