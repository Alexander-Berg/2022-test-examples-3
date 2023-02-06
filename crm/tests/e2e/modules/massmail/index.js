const { MassmailLocators } = require('./../../pages/locators/massmail');
const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../../constants/envConstants');
const url = 'index.html#/massmail';
const ticketsRetpath = CRM_TESTING_URL + url;

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

describe('mass mail module:', function() {
  beforeEach(async function() {
    const { browser } = this;

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);

    //нажать на кнопку "Новая рассылка"
    const newMassMail = await browser.$(MassmailLocators.NEW_MASSMAIL);
    await newMassMail.waitForDisplayed();
    await newMassMail.click();
  });

  afterEach(async function() {
    const { browser } = this;

    await browser.refresh();
    const newMassMail = await browser.$(MassmailLocators.NEW_MASSMAIL);
    await newMassMail.waitForDisplayed();
  });

  it('[crmspregr-134] should new massmail have user email preselected', require('./case134'));

  it('[crmspregr-139] should new massmail be saved as draft', require('./case139'));

  it('[crmspregr-138] should new massmail save signature', require('./case138'));

  it('[crmspregr-137] should new massmail use template with files', require('./case137_1'));

  it('[crmspregr-137] should file in a massmail be opened', require('./case137_2'));

  it('[crmspregr-136] should macros be added to massmail', require('./case136'));

  it('[crmspregr-140] should new massmail be previewed before sending', require('./case140'));

  it('[crmspregr-142] should one contact from file be added to massmail', require('./case142'));

  it('[crmspregr-135] todo: add clients to mass mail');

  it('[crmspregr-137] should file from template be downloaded');

  it('[crmspregr-141] should massmail be sent and received');

  it(
    '[crmspregr-468] allow repeat recipients',
    require('./massmail-allowRepeatRecipients.hermione.e2e'),
  );

  it(
    '[crmspregr-410] macroses in theme of massmail',
    require('./massmail-checkMacrosInTheme.hermione.e2e'),
  );

  it(
    '[crmspregr-518] there is no repeat button on draft',
    require('./massmail-thereIsNoRepeatButtonOnDraft.hermione.e2e'),
  );

  it(
    '[crmspregr-449] there is suggest with managers for Copy field',
    require('./massmail-checkingSuggestedListOfManagers.hermione.e2e'),
  );

  it(
    '[crmspregr-451] not possible to send massmail if email there is no in suggest',
    require('./massmail-sendEmailToNotManagerEmail.hermione.e2e'),
  );
});

describe('mass mail module: 481 case', function() {
  beforeEach(async function() {
    const { browser } = this;

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);

    //нажать на кнопку "Новая рассылка"
    const newMassMail = await browser.$(MassmailLocators.NEW_MASSMAIL);
    await newMassMail.waitForDisplayed();
    await newMassMail.click();
  });

  it(
    '[crmspregr-481] common mail for account',
    require('./massmail-commonMailForAccount.hermione.e2e'),
  );
});
