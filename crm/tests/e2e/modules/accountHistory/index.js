const { AccountsLocators } = require('./../../pages/locators/accounts');
const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../../constants/envConstants');
const url = 'index.html#/account/82765444';
const ticketsRetpath = CRM_TESTING_URL + url;

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

describe('account history module:', function() {
  beforeEach(async function() {
    const { browser } = this;

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);

    //a new account with the Lead type is created for each case
    const createAccount = await browser.$(AccountsLocators.CREATE_ACCOUNT_BUTTON);
    await createAccount.waitForClickable();
    await createAccount.click();

    const accountName = await browser.$(AccountsLocators.NEW_ACCOUNT_NAME_INPUT);
    await accountName.waitForDisplayed();
    await accountName.setValue('Test lead account');

    const accountType = await browser.$(AccountsLocators.NEW_ACCOUNT_TYPE_LIST);
    await accountType.waitForClickable();
    await accountType.click();

    const leadType = await browser.$(AccountsLocators.LEAD_TYPE);
    await leadType.waitForClickable();
    await leadType.click();

    const saveAccount = await browser.$(AccountsLocators.SAVE_NEW_ACCOUNT_BUTTON);
    await saveAccount.waitForClickable();
    await saveAccount.click();
    await saveAccount.waitForDisplayed({
      timeout: 10000,
      interval: 500,
      reverse: true,
    });
    await browser.pause(1000);
    await browser.refresh();
  });

  it('[crmspregr-100] should issue creation be seen in history', require('./case100'));

  it(
    '[crmspregr-101] should issue card be unfolded in history and actions not be available',
    require('./case101'),
  );

  it('[crmspregr-102] should history filters be folded', require('./case102'));

  it('[crmspregr-105] should file be seen in account history', require('./case105'));

  it('[crmspregr-99] future screenshot test: history block look');

  it('[crmspregr-102] filters');

  it('[crmspregr-103] search');

  it('[crmspregr-104] filling filters');

  it('[crmspregr-105] should account file be downloaded');
});
