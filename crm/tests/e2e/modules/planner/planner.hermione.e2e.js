const { PlannerLocators } = require('../../pages/locators/planner');
const {
  ROBOT_ODYSSEY_LOGIN,
  ROBOT_ODYSSEY_PASSWORD_VAULT,
} = require('../../constants/robotOdysseyData');
const { CRM_TESTING_URL } = require('../../constants/envConstants');
const url = 'index.html/#/activities/todos';
const ticketsRetpath = CRM_TESTING_URL + url;

//токен нужного робота из переменных билд-агента
const ODYSSEY_TOKEN = process.env.ROBOT_ODYSSEY_TOKEN;

describe('planner module:', function() {
  beforeEach(async function() {
    const { browser } = this;

    await browser.setMeta(
      'beforeEach',
      'логин, переход в модуль Активности, дождаться отображение таба "Дела на день"',
    );

    //логин в паспорте от робота и переход на нужный урл
    const odysseyPassword = await browser.getSecrets(ODYSSEY_TOKEN, ROBOT_ODYSSEY_PASSWORD_VAULT);
    await browser.passportLogin(ROBOT_ODYSSEY_LOGIN, odysseyPassword, ticketsRetpath);
    //дожидаемся, пока отобразится таблица "Дела на день"
    await (await this.browser.$(PlannerLocators.TAB_DAY_PLANES)).waitForDisplayed({
      timeout: 5000,
      interval: 500,
    });
  });

  afterEach(async function() {
    const { browser } = this;

    await browser.setMeta(
      'afterEach',
      'переход в модуль Активности, дождаться отображение таба "Дела на день"',
    );

    const plannerModule = await browser.$(PlannerLocators.PLANNER_MODULE);
    await plannerModule.waitForDisplayed();
    await plannerModule.click();
    //дожидаемся, пока отобразится таб "Дела на день"
    await (await this.browser.$(PlannerLocators.TAB_DAY_PLANES)).waitForDisplayed({
      timeout: 5000,
      interval: 500,
    });
  });

  it('Создание задачи в календарь', require('./planner-createTaskInCalendar'));

  it('Создание звонка в таблицу Дела на день', require('./planner-createCallInTable'));

  it(
    'Не применять изменения, если форма редактирования закрыта по крестику',
    require('./planner-noChangeOnCloseButton'),
  );
  it(
    'Изменение данных в активности из таблицы "Дела на день"',
    require('./planner-saveTaskChanges'),
  );

  it(
    'Переместить активность из таблицы "Дела на день" в календарь',
    require('./planner-moveTaskFromTableToCalendar'),
  );

  it('Удалить активность из таблицы Дела на день', require('./planner-deleteActivityFromTable'));
});
