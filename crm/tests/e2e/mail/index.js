const Color = require('color');
const MailLocators = require('../pages/locators/mail');
const { UNREAD_CIRCLE_COLORS } = require('../constants/mail');

const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../constants/envConstants');
const url = 'index.html#/mail';
const ticketsRetpath = CRM_TESTING_URL + url;

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

hermione.config.testTimeout(1000 * 60 * 2);

describe('mail page', function() {
  beforeEach(async function() {
    //const sess = await this.browser.sessionId
    //await console.log(sess)

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await this.browser.getSecrets(
      ODYSSEY_TOKEN,
      ROBOT_ODYSSEY_PASSWORD_VAULT,
    );
    await this.browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);

    //подождать, пока кнопка "Папки" станет видимой
    const folders = await this.browser.$(MailLocators.FOLDERS_BUTTON);
    await folders.waitForDisplayed();

    //если папка "Черновики" не стала видимой, то нажать кнопку "Папки"
    const isDraftFolderButtonVisible = await this.browser.$(MailLocators.DRAFT_FOLDER_BUTTON);
    await isDraftFolderButtonVisible.waitForDisplayed();

    if (!isDraftFolderButtonVisible) {
      await folders.click();
    }

    //await this.browser;

    //папка "Входящие" должна стать видимой
    //нажимаем на папку "Входящие" и ждем 4 секунды
    const isInboxFolderButtonVisible = await this.browser.$(MailLocators.INBOX_FOLDER_BUTTON);
    await isInboxFolderButtonVisible.waitForDisplayed();

    await isInboxFolderButtonVisible.click();
    await this.browser.pause(2000);
  });

  afterEach(async function() {
    //кнопка "Прочитано" в письме
    const buttons = await this.browser.$$(MailLocators.READ_MAIL_CIRCLE_BUTTON);

    //отметить все входящие письма прочитанными
    await Promise.all(
      buttons.map(async (response) => {
        //находим кнопку прочтения письма, берем атрибут color этой кнопки
        const attrColor = await response.getCSSProperty('color');
        //по атрибуту вычисляем цвет кнопки (подключается библиотека color)
        const buttonColor = Color(attrColor.value);
        //вычисляем шестнадцатеричный код этого цвета
        const hexColor = buttonColor.hex().toLowerCase();
        //письмо считается непрочитанным, если кнопка имеет цвет UNREAD_CIRCLE_COLORS
        const isUnread = await UNREAD_CIRCLE_COLORS.includes(hexColor);
        //если письмо не прочитано, то нужно нажать кнопку и сделать его прочитанным
        if (isUnread) {
          await response.click();
        }
      }),
    );
  });

  it('[crmspregr-54] receives mail without account', require('./case54'));

  //раскомментировать, когда на тесте по номеру РК будет определяться аккаунт
  //it('[crmspregr-55] receives mail with account', require('./case55'));

  //it('[crmspregr-56] receives mail with attachments');

  it('[crmspregr-57] receives important mail', require('./case57'));

  it('[crmspregr-58] sends outbox mail', require('./case58'));

  it('[crmspregr-60] saves draft and send it', require('./case60'));

  it('[crmspregr-62] moves mail to another folder', require('./case62'));

  it('[crmspregr-63] edits mail', require('./case63'));

  //it('[crmspregr-65] delays send', require('./case65'));

  //it('[crmspregr-66] filters mail');
});
