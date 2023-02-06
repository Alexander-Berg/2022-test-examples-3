const { assert } = require('chai');
const { MassmailLocators } = require('../../pages/locators/massmail');

module.exports = async function() {
  const { browser } = this;

  //Аккаунт, который будем добавлять в рассылку
  const accountTheSameEmail = 'Одиссей Рассылятор Тест';

  //Форма создания новой рассылки открыта

  //Ищем и выбираем аккаунт
  await browser.setMeta('1', 'нажать кнопку "Добавить Аккаунт"');
  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();
  await addAccount.click();
  await browser.chooseAccount(accountTheSameEmail);

  //Кликаем на список "Тип контакта"
  await browser.setMeta('2', 'нажать на "Выбрать тип контакта"');
  const contactType = await browser.$(MassmailLocators.CONTACT_TYPE_LISTBOX);
  await contactType.waitForDisplayed();
  await contactType.click();

  //Выбираем тип контакта "Все"
  await browser.setMeta('3', 'Выбрать из списка тип "Все"');
  const contactTypeAll = await browser.$(MassmailLocators.CONTACT_TYPE_ALL);
  await contactTypeAll.waitForDisplayed({
    timeout: 5000,
    interval: 500,
  });
  await contactTypeAll.click();
  await browser.pause(2000);

  //Кнопка "Посмотреть и Отправить" неактивна
  await browser.setMeta('4', 'Проверяем, что кнопка "Просмотреть и отправить" неактивна');
  const viewAndSend = await browser.$(
    MassmailLocators.VIEW_AND_SEND_BUTTON + '[aria-disabled="true"]',
  );
  const buttonIsExisting = await viewAndSend.isExisting();
  assert.isTrue(buttonIsExisting, 'Button should not be active');

  //Значок ошибки отображен
  await browser.setMeta('5', 'Проверяем, что иконка ошибки отображена');
  const errorIcon = await browser.$(MassmailLocators.ERROR_ICON_CANT_SEND);
  const iconIsDisplayed = await errorIcon.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });
  assert.isTrue(iconIsDisplayed, 'There is no error icon');

  //Чекбокс неактивен
  await browser.setMeta('6', 'Проверяем, что чекбокс "Разрешить повторение адресатов" не выбран');
  const allowRepeatRecipientsCheckbox = await browser.$(
    MassmailLocators.ALLOW_REPEATE_RECEPIENTS_CHECKBOX + ' input[aria-checked="false"]',
  );
  await allowRepeatRecipientsCheckbox.waitForExist();
  assert.isTrue(
    await allowRepeatRecipientsCheckbox.isExisting(),
    'Checkbox should not be selected',
  );

  //Установить чекбокс "Разрешить повторение адресатов"
  await browser.setMeta('7', 'Выбрать чекбокс "Разрешить повторение адресатов"');
  await allowRepeatRecipientsCheckbox.click();
  await browser.pause(1000);

  //Проверить, что чекбокс выбран, кнопка активна и иконка ошибки не отображается
  await browser.setMeta('8', 'Проверяем, что чекбокс "Разрешить повторение адресантов" выбран');
  const checkboxIsSelected = await browser.$(
    MassmailLocators.ALLOW_REPEATE_RECEPIENTS_CHECKBOX + ' input[aria-checked="true"]',
  );
  await checkboxIsSelected.waitForExist();
  assert.isTrue(await checkboxIsSelected.isExisting(), 'Checkbox should be selected');

  await browser.setMeta('9', 'Проверяем, что кнопка "Просмотреть и отправить" активна');
  const buttonIsActive = await browser.$(
    MassmailLocators.VIEW_AND_SEND_BUTTON + '[aria-disabled="false"]',
  );
  const activeButtonIsExisting = await buttonIsActive.isExisting();
  assert.isTrue(activeButtonIsExisting, 'View and send button is not active');

  await browser.setMeta('10', 'Проверяем, что иконка ошибки не отображена');
  assert.isFalse(await errorIcon.isDisplayed(), 'error icon should not be displayed');
};
