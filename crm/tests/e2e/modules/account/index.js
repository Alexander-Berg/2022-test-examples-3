const { AccountsLocators } = require('./../../pages/locators/accounts');
const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../../constants/envConstants');
const url = '#/account/82765444';
const ticketsRetpath = CRM_TESTING_URL + url;

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

describe('accounts module:', function() {
  beforeEach(async function() {
    const { browser } = this;

    //
    await browser.setMeta(
      'beforeEach',
      'логин, переход в модуль Аккаунтов, проверка доступности кнопки Добавить',
    );
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);

    const addContacts = await browser.$(AccountsLocators.ADD_CONTACT);
    await addContacts.waitForDisplayed();
  });

  it('[crmspregr-51] should issue be added to account', require('./case51'));

  describe('[crmspregr-50] contacts in account', function() {
    beforeEach(async function() {
      const { browser } = this;

      //
      await browser.setMeta('beforeEach', 'заведение нового аккаунта');
      const addContact = await browser.$(AccountsLocators.ADD_CONTACT);
      await addContact.waitForDisplayed();
      await addContact.click();

      const nameInput = await browser.$(AccountsLocators.NAME_INPUT);
      await nameInput.waitForDisplayed();
      await nameInput.setValue('Odyssey');

      const emailInput = await browser.$(AccountsLocators.EMAIL_INPUT);
      await emailInput.waitForDisplayed();
      await emailInput.setValue('robot-space-odyssey@yandex-team.ru');

      const phoneInput = await browser.$(AccountsLocators.PHONE_INPUT);
      await phoneInput.waitForDisplayed();
      await phoneInput.setValue('+79999999999');

      const saveContact = await browser.$(AccountsLocators.SAVE_CONTACT_BUTTON);
      await saveContact.waitForEnabled({ timeout: 5000 });
      await saveContact.click();

      await nameInput.waitForDisplayed({
        timeout: 10000,
        interval: 500,
        reverse: true,
      });
    });

    afterEach(async function() {
      const { browser } = this;

      //
      await browser.setMeta('afterEach', 'удаление заведенного аккаунта');

      const deleteContact = await browser.$(AccountsLocators.DELETE_CONTACT);
      await deleteContact.waitForDisplayed();
      await deleteContact.click();
      await browser.acceptAlert();
    });

    it('should be added', require('./case50_1'));

    it('should be edited', require('./case50_2'));
  });

  describe('[crmspregr-52] comments in account', function() {
    beforeEach(async function() {
      const { browser } = this;

      const newComment = await browser.$(AccountsLocators.NEW_COMMENT_INPUT);
      await newComment.waitForDisplayed();
      await newComment.setValue('Test comment to account');

      const saveComment = await browser.$(AccountsLocators.SAVE_COMMENT_BUTTON);
      await saveComment.waitForEnabled();
      await saveComment.click();

      const savedComment = await browser.$(AccountsLocators.SAVED_COMMENT);
      await savedComment.waitForDisplayed();
    });

    afterEach(async function() {
      const { browser } = this;

      const deleteComment = await browser.$(AccountsLocators.DELETE_COMMENT_BUTTON);
      await deleteComment.waitForDisplayed();
      await deleteComment.click();
      await browser.acceptAlert();
    });

    it('should be added', require('./case52_1'));

    it('should be edited', require('./case52_2'));
  });

  describe('[crmspregr-53] should account be created', function() {
    it('- lead account', require('./case53_1'));

    it('- counteragent account', require('./case53_2'));

    it('- metrica client account', require('./case53_3'));

    it('- mobile app account', require('./case53_4'));
  });

  it('[crmspregr-49] should account be edited', require('./case49'));

  it('[crmspregr-47] todo: save account filters');

  it('[crmspregr-46] future screenshot test - open account module and check that it looks ok');

  it('[crmspregr-48] future screenshot test - open account and check that it looks ok');
});
