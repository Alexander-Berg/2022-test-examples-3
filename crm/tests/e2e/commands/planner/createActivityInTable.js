const { assert } = require('chai');
const { PlannerLocators } = require('../../pages/locators/planner');
//параметры: название задачи, дата дедлайна, часы и минуты дедлайна
module.exports = async function(taskName, deadlineDate, deadlineTime) {
  //создание задачи в таблице Планировщика (с дедлайном)
  //предполагается, что уже находимся в модуле Планировщик
  //нажать кнопку "Запланировать активность"
  const scheduleActivity = await this.$(PlannerLocators.SCHEDULE_ACTIVITY);
  await scheduleActivity.waitForDisplayed();
  await scheduleActivity.click();

  //выбрать "Задача"
  const taskActivity = await this.$(PlannerLocators.TASK_ACTIVITY);
  await taskActivity.waitForClickable();
  await taskActivity.click();

  //дождаться появления окна создания задачи
  const activityCreateForm = await this.$(PlannerLocators.ACTIVITY_CREATE_FORM);
  await activityCreateForm.waitForDisplayed();
  //проверить, что все нужные поля на форме присутствуют
  await (await this.$(PlannerLocators.CREATE_FORM_NAME)).waitForDisplayed();
  await (await this.$(PlannerLocators.CREATE_FORM_DEADLINE)).waitForDisplayed();
  await (await this.$(PlannerLocators.CREATE_FORM_START_DATETIME)).waitForDisplayed();
  await (await this.$(PlannerLocators.CREATE_FORM_END_DATETIME)).waitForDisplayed();
  await (await this.$(PlannerLocators.CREATE_FORM_DESCRIPTION)).waitForDisplayed();
  await (await this.$(PlannerLocators.CREATE_FORM_DONE)).waitForDisplayed();
  await (await this.$(PlannerLocators.CREATE_FORM_OPPORTUNITIES)).waitForDisplayed();

  //вводим название задачи
  const inputTaskName = await this.$(PlannerLocators.ACTIVITY_NAME_FIELD);
  await inputTaskName.setValue(taskName);
  //удаляем дату окончания
  const clearEndDate = await this.$(PlannerLocators.END_DATE_CLEAR);
  await clearEndDate.waitForClickable();
  await clearEndDate.click();
  //удаляем дату начала
  const clearStartDate = await this.$(PlannerLocators.START_DATE_CLEAR);
  await clearStartDate.waitForClickable();
  await clearStartDate.click();
  //ставим дату дедлайна
  const setDeadline = await this.$(PlannerLocators.DEADLINE_DATE_INPUT);
  await setDeadline.setValue(deadlineDate);
  //ставим время дедлайна
  const setDeadlineTime = await this.$(PlannerLocators.DEADLINE_TIME_INPUT);
  await setDeadlineTime.setValue(deadlineTime);
  //нажимаем кнопку "Сохранить"
  const saveTask = await this.$(PlannerLocators.SAVE_ACTIVITY_BUTTON);
  await saveTask.waitForClickable();
  await saveTask.click();
  //дождаться, пока окно создания исчезнет
  await activityCreateForm.waitForDisplayed({
    timeout: 5000,
    interval: 500,
    reverse: true,
  });

  //обновляем страницу
  await this.refresh();
  //найти в таблице созданную задачу по названию
  const tableRows = await this.$(PlannerLocators.FULL_TABLE);
  const allTableEvents = await tableRows.getText();
  assert.include(allTableEvents, taskName);
};
