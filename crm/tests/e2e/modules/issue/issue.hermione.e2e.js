const { IssuesLocators } = require('../../pages/locators/issues');
const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../../constants/envConstants');
const url = 'index.html#/issues';
const ticketsRetpath = CRM_TESTING_URL + url;

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

describe('issues module:', function() {
  beforeEach(async function() {
    const { browser } = this;
    //
    await browser.setMeta('beforeEach', 'логин и переход в модуль Задач');
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);
    //
    await browser.setMeta('beforeEach', 'сортировка задач по убыванию даты создания');
    await browser.sortTickets('Дата создания ▼');
    //
    await browser.setMeta('beforeEach', 'создание новой задачи');
    await browser.createNewIssue('Issue created with automated test');
  });

  afterEach(async function() {
    const { browser } = this;

    await browser.setMeta('afterEach', 'переход в модуль Задач');

    const issuesModule = await browser.$(IssuesLocators.ISSUES_MODULE);
    await issuesModule.waitForEnabled();
    await issuesModule.click();
  });

  it('[crmspregr-32] should issue be created with status open', require('./case32_1'));

  it('[crmspregr-32] should issue be created with current user as assignee', require('./case32_2'));

  it('[crmspregr-34] should issue be closed', require('./case34_1'));

  it('[crmspregr-34] should issue be reopened', require('./case34_2'));

  it('[crmspregr-42] should comment to issue be saved', require('./case42_1'));

  it('[crmspregr-42] should comment to issue be deleted', require('./case42_2'));

  it('[crmspregr-33] should description be edited', require('./case33'));

  it(
    '[crmspregr-45] should issue with today as deadline be seen in Today filter',
    require('./case45_1'),
  );

  it('[crmspregr-45] should deadline be set in attributes', require('./case45_2'));

  it('[crmspregr-45] should deadline be cleared', require('./case45_3'));

  it('[crmspregr-40] should new mark be created', require('./case40_1'));

  it('[crmspregr-40] should new mark be deleted from issue', require('./case40_2'));

  it('[crmspregr-35] should new subtask be created', require('./case35_1'));

  it('[crmspregr-35] should new subtask be shown in linked in parent issue', require('./case35_2'));

  it('[crmspregr-35] should new subtask have a link to parent issue', require('./case35_3'));

  it('[crmspregr-37] should subtask be unlinked with parent issue', require('./case37'));

  it('[crmspregr-33] should issue be renamed', require('./case33'));

  it('[crmspregr-38] should issue be deleted', require('./case38'));

  it('[crmspregr-145] should account be added', require('./case145_1'));

  it('[crmspregr-145] should account be deleted', require('./case145_2'));

  it('[crmspregr-41] should follower be added to issue', require('./case41_1'));

  it('[crmspregr-41] should follower be deleted from issue', require('./case41_2'));

  it('[crmspregr-144] should timer be created with comment', require('./case144_1'));

  it('[crmspregr-144] should timer be set in two days', require('./case144_2'));

  it('[crmspregr-144] should timer be removed', require('./case144_3'));

  it('[crmspregr-144] should timer be edited', require('./case144_4'));

  it('[crmspregr-39] should workflow be changed', require('./case39'));

  it('Поиск созданной задачи в списке', require('./issue-findIssueInList'));

  it('Поиск созданной задачи в фильтре Мои-Открытые', require('./issue-filterMyOpen'));

  it('Поиск созданной задачи в фильтре Мои-Закрытые', require('./issue-filterMyClosed'));

  it('Поиск созданной задачи в фильтре Делегированы-Все', require('./issue-filterDelegatedAll'));

  it('Поиск созданной задачи в фильтре Наблюдаю-Все', require('./issue-filterFollowedAll'));

  it('Поиск созданной задачи в фильтре Моя группа-Все', require('./issue-filterMygroupAll'));

  it('Поиск созданной задачи в фильтре Мои-Все', require('./issue-filterMyAll'));

  it('[crmspregr-44] should search filters be applied');
});
