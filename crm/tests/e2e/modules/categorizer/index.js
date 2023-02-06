//const { AccountsLocators } = require('./../../pages/locators/accounts');
const { TicketsLocators } = require('./../../pages/locators/tickets');
const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../../constants/envConstants');
const urlTicket = 'index.html#/cases/13374429?filterId=3407'; //тикет для категорий
const ticketsRetpath = CRM_TESTING_URL + urlTicket;

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

//const accountForRequest = 'index.html#/account/82765444'; //аккаунт, для которого делается заявка на вторую линию

/*
describe('categorizer in account module:', function() {
  beforeEach(async function() {
    const { browser } = this;

    await browser.url(accountForRequest);
    await browser.setCookies(cookie);
    await browser.url(accountForRequest);
    await browser.refresh();

    const addContacts = await browser.$(AccountsLocators.ACCOUNT_MODULE);
    await addContacts.waitForDisplayed();
  });

  afterEach(async function() {
    //эта секция необязательная, возможно ее нужно удалить, зависит от тестов, которые войдут в этот describe
    const { browser } = this;

    await browser.refresh();
    const accountModule = await browser.$(AccountsLocators.ACCOUNT_MODULE);
    await accountModule.waitForEnabled();
    await accountModule.click();
  });

  it(
    '[crmspregr-258] should create 2-line ticket from Account-form with the specified category',
    require('./case258'),
  );
});
*/

describe('categorizer in tickets module:', function() {
  beforeEach(async function() {
    const { browser } = this;

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);
  });

  afterEach(async function() {
    const { browser } = this;

    await browser.refresh(); //этот рефреш нужен, т.к. тесты останавливаются на открытом окне категоризатора
    const ticketsModule = await browser.$(TicketsLocators.TICKET_MODULE);
    await ticketsModule.waitForEnabled();
    await ticketsModule.click();
  });

  it('[crmspregr-259_1] tooltip appears if press dislike', require('./case259_1'));

  it('[crmspregr-259_2] tooltip appears if press like', require('./case259_2'));

  it('[crmspregr-259_3] tooltip appears if press dislike on fullscreen', require('./case259_3'));

  it('[crmspregr-259_4] tooltip appears if press like on fullscreen', require('./case259_4'));
});
