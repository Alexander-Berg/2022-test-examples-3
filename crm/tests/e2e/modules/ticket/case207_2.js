const { assert } = require('chai');
const { TicketsLocators } = require('../../pages/locators/tickets');
const mailText = `mailtext... ${Math.random() * 1000}`;

module.exports = async function() {
  const { browser } = this;

  //подождать, пока отобразится кнопка "Написать письмо" и нажать на нее
  const writeMailButton = await browser.$(TicketsLocators.WRITE_MAIL_TO_TICKET);
  await writeMailButton.waitForDisplayed({ timeout: 3000 });
  await writeMailButton.click();
  //подождать, пока отобразится форма написания письма
  const mailBody = await browser.$(TicketsLocators.MAIL_BODY);
  await mailBody.waitForDisplayed({ timeout: 10000, interval: 500 });

  //в тело письма написать что-нибудь
  await mailBody.setValue(mailText);
  //здесь нужно подождать, иначе черновик не сохранится
  await browser.pause(1000);

  //не сохраняя письма, нажать на кнопку "Написать комментарий"
  const writeCommentToTicket = await browser.$(TicketsLocators.WRITE_COMMENT_TO_TICKET);
  await writeCommentToTicket.click();

  //дожидаемся попапа с текстом "Черновик сохранен"
  const draftSavedPopup = await browser.$(TicketsLocators.DRAFT_SAVED_POPUP);
  await draftSavedPopup.waitForDisplayed({ timeout: 10000, interval: 500 });

  //находим серую иконку черновика
  const draftGreyButton = await browser.$(TicketsLocators.DRAFT_GREY_BUTTON);
  await draftGreyButton.waitForDisplayed({ timeout: 10000, interval: 500 });
  //берем текст из свернутого контейнера письма
  const isMailTextMatch = await (await browser.$(TicketsLocators.MAIL_PREVIEW)).getText();
  //и сравниваем отправленный текст письма с полученным в черновике
  assert.include(isMailTextMatch, mailText, 'comment was not saved');
};
