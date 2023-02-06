const { assert } = require('chai');
const MailLocators = require('../pages/locators/mail');

module.exports = async function() {
  const { browser } = this;

  let mailData = {
    subject: 'test',
    text: `${Math.random() * 1000}`,
    important: true,
  };

  //отправить письмо
  await browser.sendAndFindMail(mailData);
  //открыть первое письмо в списке
  await browser.openFirstInboxMail();

  //дождаться открытия письма
  await (await browser.$(MailLocators.MAIL_PREVIEW)).waitForDisplayed();

  ///////////////////////////////////////////////////////////////////////////////////////////
  //кликнуть на кнопку прочтения внутри письма
  const readMailButton = await browser.$(MailLocators.READ_MAIL_BUTTON);
  await readMailButton.click();
  await browser.pause(1000);
  //если признак прочитанности не поменялся, значит кнопка не сработала
  const firstMail = await browser.$(MailLocators.FIRST_MAIL_IN_LIST);

  let isUnread = await browser.isMailUnread(firstMail);
  assert.strictEqual(
    isUnread,
    false,
    'Состояние прочтения письма не меняется по кнопке "Прочитано"',
  );
  //нажать на кнопку "Не прочитано" в письме
  await readMailButton.click();
  await browser.pause(2000);
  //если признак прочитанности не поменялся, значит кнопка не сработала
  isUnread = await browser.isMailUnread(firstMail);
  assert.strictEqual(
    isUnread,
    true,
    'Состояние прочтения письма не меняется по кнопке "Прочитано"',
  );

  ///////////////////////////////////////////////////////////////////////////////////////////
  //в этом же письме нажать на желтый маркер прочтения письма
  const readMailCircleButton = await browser.$(MailLocators.READ_MAIL_CIRCLE_BUTTON);
  await readMailCircleButton.click();
  await browser.pause(1000);
  //если признак прочитанности не поменялся, значит кнопка не сработала
  isUnread = await browser.isMailUnread(firstMail);
  assert.strictEqual(
    isUnread,
    false,
    'Состояние прочтения письма не меняется по клику по желтому маркеру',
  );
  //еще раз нажать на маркер прочтения письма
  await readMailCircleButton.click();
  await browser.pause(1000);
  //если признак прочитанности не поменялся, значит кнопка не сработала
  isUnread = await browser.isMailUnread(firstMail);
  assert.strictEqual(
    isUnread,
    true,
    'Состояние прочтения письма не меняется по клику по желтому маркеру',
  );

  ///////////////////////////////////////////////////////////////////////////////////////////

  let isImportant = await browser.isMailImportant(firstMail);
  const mailImportance = await browser.$(MailLocators.MAIL_IMPORTANT_FLAG);
  //если у письма стоит признак важности, то снимаем его
  if (isImportant) {
    await mailImportance.click();
    await browser.pause(1000);
  }
  //кликаем на флажок важности, чтобы письмо стало важным
  await mailImportance.click();
  await browser.pause(1000);
  //если письмо осталось неважным, значит флажок не сработал
  isImportant = await browser.isMailImportant(firstMail);
  assert.strictEqual(
    isImportant,
    true,
    'Письмо не меняет состояние важности по клику по красному флажку',
  );
  //нажимаем флажок важности еще раз
  await mailImportance.click();
  await browser.pause(1000);
  //если письмо стало важным, значит флажок не сработал
  isImportant = await browser.isMailImportant(firstMail);
  assert.strictEqual(
    isImportant,
    false,
    'Письмо не меняет состояние важности по клику по красному флажку',
  );

  ///////////////////////////////////////////////////////////////////////////////////////////

  //клик в письме на кнопку "Спам"
  const spamMailButton = await browser.$(MailLocators.SPAM_MAIL_BUTTON);
  await spamMailButton.click();
  await browser.pause(1000);
  //переход в папку "Спам"
  await (await browser.$(MailLocators.SPAM_FOLDER_BUTTON)).click();
  await browser.pause(4000);
  //если письмо не найдено по тексту письма в папке Спам, значит кнопка не сработала
  let mail = await browser.getMailByText(mailData.text);
  assert.notStrictEqual(
    mail,
    undefined,
    'После клика на кнопку "Спам" письмо не переместилось в папку "Спам"',
  );
  //нажать кнопку "Не спам" в письме
  await spamMailButton.click();
  await browser.pause(1000);
  //перейти в папку "Входящие"
  await (await browser.$(MailLocators.INBOX_FOLDER_BUTTON)).click();
  await browser.pause(4000);
  //если во входящих письмо не найдено по тексту, значит кнопка не сработала
  mail = await browser.getMailByText(mailData.text);
  assert.notStrictEqual(
    mail,
    undefined,
    'После клика на кнопку "Не спам" письмо не переместилось в папку "Входящие"',
  );

  ///////////////////////////////////////////////////////////////////////////////////////////

  //нажать в письме на кнопку "Удалить"
  await (await browser.$(MailLocators.DELETE_MAIL_BUTTON)).click();
  await browser.pause(3000);
  //перейти в папку "Удаленные"
  await (await browser.$(MailLocators.DELETED_FOLDER_BUTTON)).click();
  await browser.pause(4000);
  //если в удаленных письмо не найдено по тексту, значит кнопка не сработала
  mail = await browser.getMailByText(mailData.text);
  assert.notStrictEqual(
    mail,
    undefined,
    'После клика на кнопку "Удалить" письмо не переместилось в папку "Удаленные"',
  );

  //после всех манипуляций пометить письмо прочитанным
  return readMailButton.click();
};
