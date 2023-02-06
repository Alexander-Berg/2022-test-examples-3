const { assert } = require('chai');
const { TicketsLocators } = require('../../pages/locators/tickets');
//const { MailLocators } = require('../../pages/locators/mail');
const mailText = `mailtext... ${Math.random() * 1000}`;
const mailToText = 'robot-space-odyssey@yandex-team.ru';

module.exports = async function() {
  const { browser } = this;

  //подождать, пока отобразится кнопка "Написать письмо" и нажать на нее
  const writeMailButton = await browser.$(TicketsLocators.WRITE_MAIL_TO_TICKET);
  await writeMailButton.waitForDisplayed({
    timeout: 8000,
  });
  await writeMailButton.click();

  const mailTo = await browser.$(TicketsLocators.FORM_INPUT_TO);
  await mailTo.waitForDisplayed();
  await mailTo.setValue(mailToText);

  await browser.pause(20000);

  //подождать, пока отобразится форма написания письма
  const mailBody = await browser.$(TicketsLocators.MAIL_BODY);
  await mailBody.waitForDisplayed({
    timeout: 10000,
    interval: 500,
  });

  //в тело письма написать что-нибудь
  await mailBody.setValue(mailText);
  //здесь нужно подождать, иначе черновик не сохранится
  await browser.pause(1000);

  //Ждем в течение одной минуты, что появится надпись "сохранено в "

  const saveDraftButton = await browser.$(TicketsLocators.SAVE_DRAFT_BUTTON);

  /*await browser.pause(20000);

  await saveDraftButton.waitUntil(
    async () => {
      if (saveDraftButton.isEnabled()) {
        await mailBody.setValue(mailText);
      }
    },
    { timeout: 50000, interval: 5000 },
  );

  await browser.pause(5000);
*/

  //await saveDraftButton.waitForEnabled({ timeout: 30000, interval: 5000 });

  await saveDraftButton.waitForClickable({ timeout: 50000, reverse: true, interval: 5000 });

  await browser.pause(10000);

  //дожидаемся попапа с текстом "Черновик сохранен"
  // const draftSavedPopup = await browser.$(TicketsLocators.DRAFT_SAVED_POPUP);
  //await draftSavedPopup.waitForDisplayed({ timeout: 10000, interval: 500 });

  //находим надпись Сохранено в
  const draftSavedMessage = await browser.$(TicketsLocators.DRAFT_SAVED_MESSAGE);
  await draftSavedMessage.waitForDisplayed();
  const iSMessageDisplayed = await draftSavedMessage.getText();

  //и сравниваем отправленный текст письма с полученным в черновике
  assert.include(iSMessageDisplayed, 'Сохранено в', 'draft was not saved');
};
