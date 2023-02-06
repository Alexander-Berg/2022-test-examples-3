const { assert } = require('chai');
const { MassmailLocators } = require('../../pages/locators/massmail');
const massmailTheme = `e2e-test518 ${Math.random()}`;
const accountNmail = 'тестовый аккаунт с N почтами';

module.exports = async function() {
  const { browser } = this;

  //Форма создания новой рассылки открыта

  await browser.setMeta('1', 'Добавить уникальную тему');
  const mailTopic = await browser.$(MassmailLocators.MAIL_TOPIC_INPUT);
  await mailTopic.waitForDisplayed();
  await mailTopic.click();
  await mailTopic.setValue(massmailTheme);

  await browser.setMeta('2', 'Добавить аккаунт');
  const addAccount = await browser.$(MassmailLocators.ADD_ACCOUNT_LINK);
  await addAccount.waitForDisplayed();
  await addAccount.click();
  await browser.chooseAccount(accountNmail);

  await browser.setMeta('3', 'нажать на "Выбрать тип контакта"');
  const contactType = await browser.$(MassmailLocators.CONTACT_TYPE_LISTBOX);
  await contactType.waitForDisplayed();
  await contactType.click();

  await browser.setMeta('4', 'Выбрать из списка тип "Для рассылок"');
  const contactTypeMassmail = await browser.$(MassmailLocators.CONTACT_TYPE_MASSMAIL);
  await contactTypeMassmail.waitForDisplayed({
    timeout: 5000,
    interval: 500,
  });
  await contactTypeMassmail.click();
  await browser.pause(2000);

  await browser.setMeta('5', 'Кликнуть на "Сохранить как черновик"');
  const buttonSaveDraft = await browser.$(MassmailLocators.SAVE_DRAFT_BUTTON);
  await buttonSaveDraft.waitForDisplayed();
  await buttonSaveDraft.click();

  await browser.setMeta('6', 'Проверить, что кнопка "Повторить рассылку" отсутсвует');
  const buttonRepeatMassmail = await browser.$(MassmailLocators.REPEAT_MASSMAIL);
  assert.isFalse(
    await buttonRepeatMassmail.isDisplayed(),
    'there is repeat massail button for draft mode',
  );

  await browser.setMeta('7', 'Кликнуть на "Посмотреть и отправить"');
  const buttonViewAndSend = await browser.$(MassmailLocators.VIEW_AND_SEND_BUTTON);
  await buttonViewAndSend.waitForDisplayed();
  await buttonViewAndSend.click();

  await browser.setMeta('8', 'Проверить, что кнопка "Повторить рассылку" отсутсвует');
  assert.isFalse(
    await buttonRepeatMassmail.isDisplayed(),
    'there is repeat massail button for view mode',
  );

  await browser.setMeta('9', 'Кликнуть на "Отправить"');
  const buttonSend = await browser.$(MassmailLocators.SEND_MAIL_BUTTON_BORDER);
  await buttonSend.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });
  await buttonSend.click();

  await browser.setMeta('10', 'Увидеть сообщение об отправке');
  const messageIsMassmailSent = await browser.$(MassmailLocators.MESSAGE_MASSMAIL_SENT);
  await messageIsMassmailSent.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  await browser.setMeta('11', 'Проверить, что кнопка "Повторить рассылку" отображается');
  assert.isTrue(
    await buttonRepeatMassmail.isDisplayed(),
    'there is no repeat massail button for sent massmail',
  );
};
