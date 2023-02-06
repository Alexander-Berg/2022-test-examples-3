const { TicketsLocators } = require('./../../pages/locators/tickets');
const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../../constants/envConstants');
const url = '#/cases?filterId=3407';
const ticketsRetpath = CRM_TESTING_URL + url; //очередь 'Отдел CRM (тест).Autotest'

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

describe('tickets module:', function() {
  beforeEach(async function() {
    const { browser } = this;
    //установить таймаут по умолчанию
    await browser.setTimeout({ implicit: 5000 });
    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);

    await browser.createNewTicket(
      'Ticket created with automated test',
      'Отдел CRM (тест).Autotest',
    );
  });

  afterEach(async function() {
    const { browser } = this;

    const ticketModule = await browser.$(TicketsLocators.TICKET_MODULE);
    await ticketModule.waitForDisplayed();
    await ticketModule.click();
  });

  it('[crmspregr-85] should ticket be created with status open', require('./case85'));

  it('[crmspregr-80] should comment to ticket be added', require('./case80_1'));

  it('[crmspregr-80] should comment to ticket be deleted', require('./case80_2'));

  it('[crmspregr-86] should ticket change status to solved - closed', require('./case86_1'));

  it('[crmspregr-86] should ticket change status to open', require('./case86_2'));

  it('[crmspregr-86] should ticket change status to in progress', require('./case86_3'));

  it('[crmspregr-86] should ticket change status to postponed', require('./case86_4'));

  it('[crmspregr-86] should ticket change status to closed - spam', require('./case86_5'));

  it('[crmspregr-147] should follower be added to ticket', require('./case147_1'));

  it('[crmspregr-147] should follower be deleted from ticket', require('./case147_2'));

  it('[crmspregr-149] should ticket change queue', require('./case149'));

  //it('[crmspregr-150] should ticket change category', require('./case150'));   --need to be changed

  it('[crmspregr-125] should next ticket be taken from queue', require('./case125'));

  it('[crmspregr-148] should new mark be added to ticket', require('./case148_1'));

  it('[crmspregr-148] should new mark be removed from ticket', require('./case148_2'));

  it('[crmspregr-87] should ticket change assignee', require('./case87_1'));

  it('[crmspregr-87] should ticket be left without assignee', require('./case87_2'));

  it('[crmspregr-92] create new ST ticket via header', require('./case92_1'));

  it('[crmspregr-93] should startrack ticket be linked to crm ticket', require('./case93_1'));

  it(
    '[crmspregr-93] should startrack ticket be linked to crm ticket with full link',
    require('./case93_2'),
  );

  it('[crmspregr-94] should startrack ticket be unlinked with crm ticket', require('./case94_1'));

  it('[crmspregr-94] should crm ticket be linked to crm ticket', require('./case94_2'));

  it('[crmspregr-94] should crm ticket be unlinked with crm ticket', require('./case94_3'));

  it('[crmspregr-152] should timer be created with comment', require('./case152_1'));

  it('[crmspregr-152] should timer be set in two days', require('./case152_2'));

  it('[crmspregr-152] should timer be removed', require('./case152_3'));

  it('[crmspregr-152] should timer be edited', require('./case152_4'));

  it(
    '[crmspregr-89] should ticket in second line block its parent ticket in first line',
    require('./case89_1'),
  );

  it(
    '[crmspregr-89] should ticket change status to postponed when ticket to second line is created',
    require('./case89_2'),
  );

  it('[crmspregr-89] should ticket in second line have correct content', require('./case89_3'));

  it('[crmspregr-207] draft mail saving - go to ticket', require('./case207_1'));

  it('[crmspregr-207] draft mail saving - go to comment', require('./case207_2'));

  /* it(
    '[crmspregr-207] draft mail saving - autosave draft in 1 minute',
    require('./case207_3'),
  );
  */

  it('[crmspregr-79] ticket with incoming letter with file');

  it('[crmspregr-81] reply to incoming letter');

  it('[crmspregr-83] save and use draft');

  it('[crmspregr-95] check ticket history');

  it('[crmspregr-96] check search');
});

describe('tickets module test92:', function() {
  afterEach(async function() {
    const { browser } = this;

    const linkedTicketsTab = await browser.$(TicketsLocators.LINKED_TICKETS);
    await linkedTicketsTab.waitForDisplayed();
    await linkedTicketsTab.click();

    //находим все кнопки с цепочкой и нажимаем на них
    const unlinkedButtonS = await browser.$$(TicketsLocators.UNLINK_ST_TICKET);
    for (let i = 0, length = unlinkedButtonS.length; i < length; i++) {
      const unlinkedButton = unlinkedButtonS[i];
      await unlinkedButton.click();
      await browser.acceptAlert();
    }
    return undefined;
  });

  it('[crmspregr-92] create new ST ticket via button on mail', require('./case92_2'));
});

describe('create ticket by incoming mail', function() {
  beforeEach(async function() {
    const { browser } = this;

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);
  });

  afterEach(async function() {
    const { browser } = this;

    const ticketModule = await browser.$(TicketsLocators.TICKET_MODULE);
    await ticketModule.waitForDisplayed();
    await ticketModule.click();
  });

  it('[crmspregr-77] - ticket with incoming letter without client', require('./case77'));

  it('[crmspregr-78] - ticket with incoming letter with client', require('./case78'));

  it('[crmspregr-171] - internal signature for smejnikam answer', require('./case171'));

  it('[crmspregr-173] - queue signature for mail answer', require('./case173'));

  //it('[crmspregr-79] - mail with attachments', require('./case79'));
});

describe('tickets module - attributes changes', function() {
  beforeEach(async function() {
    const { browser } = this;

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);
  });

  afterEach(async function() {
    const { browser } = this;

    const ticketModule = await browser.$(TicketsLocators.TICKET_MODULE);
    await ticketModule.waitForDisplayed();
    await ticketModule.click();
  });

  it(
    '[crmspregr-238] should updated attributes be seen in another tab after autorefresh',
    require('./ticket-updateAttributesAfterRefreshInAnotherTab.hermione.e2e'),
  );

  // временно отключенный тест, на гридах не находится iframe
  //   it(
  //     '[crmspregr-95] should changes in ticket be saved in ticket history',
  //     require('./ticket-checkHistoryChanges.hermione.e2e'),
  //   );

  it('[crmspregr-97] check sorting', require('./ticket-checkSorting.hermione.e2e'));

  it('[crmspregr-97] save sorting in another tab', require('./ticket-saveSortingInAnotherTab'));
});
