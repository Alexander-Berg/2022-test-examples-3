//создание Задачи с валидациями в календаре в Планировщике

const { assert } = require('chai');
const { PlannerLocators } = require('../../pages/locators/planner');
const taskName = `e2e-test314 ${Math.random()}`;

module.exports = async function() {
  const { browser } = this;
  //
  await this.browser.setMeta('1', 'нажать кнопку "Запланировать активность"');
  const scheduleActivity = await browser.$(PlannerLocators.SCHEDULE_ACTIVITY);
  await scheduleActivity.waitForDisplayed();
  await scheduleActivity.click();
  //
  await this.browser.setMeta('2', 'выбрать "Задача"');
  const taskActivity = await browser.$(PlannerLocators.TASK_ACTIVITY);
  await taskActivity.waitForClickable();
  await taskActivity.click();
  //
  await this.browser.setMeta('3', 'дождаться появления окна создания задачи');
  const activityCreateForm = await browser.$(PlannerLocators.ACTIVITY_CREATE_FORM);
  await activityCreateForm.waitForDisplayed();
  //
  await this.browser.setMeta('4', 'проверить, что все нужные поля на форме присутствуют');
  await (await browser.$(PlannerLocators.CREATE_FORM_NAME)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_DEADLINE)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_START_DATETIME)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_END_DATETIME)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_DESCRIPTION)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_DONE)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_OPPORTUNITIES)).waitForDisplayed();
  //
  await this.browser.setMeta('5', 'нажать на кнопку Сохранить');
  const saveEmptyTask = await browser.$(PlannerLocators.SAVE_ACTIVITY_BUTTON);
  await saveEmptyTask.waitForClickable();
  await saveEmptyTask.click();
  //
  await this.browser.setMeta(
    '6',
    'дождаться появления попапа с текстом о том, что нельзя сохранить задачу без названия',
  );
  const saveEmptyTaskPopup = await browser.$(PlannerLocators.FIRST_POPUP_IN_LIST);
  await saveEmptyTaskPopup.waitForDisplayed({ timeout: 5000, interval: 500 });
  const popupText = await saveEmptyTaskPopup.getText();
  //
  await this.browser.setMeta('7', 'проверяем, что текст в попапе верный');
  assert.include(popupText, 'Не заполнено поле Название');
  //
  await this.browser.setMeta('8', 'вводим название задачи');
  const inputTaskName = await browser.$(PlannerLocators.ACTIVITY_NAME_FIELD);
  await inputTaskName.setValue(taskName);
  //
  await this.browser.setMeta('9', 'нажимаем кнопку "Сохранить"');
  await saveEmptyTask.waitForClickable();
  await saveEmptyTask.click();
  //
  await this.browser.setMeta('10', 'дождаться, пока окно создания исчезнет');
  await activityCreateForm.waitForDisplayed({
    timeout: 5000,
    interval: 500,
    reverse: true,
  });

  //
  await this.browser.setMeta('11', 'проверить, что активность теперь отображается в календаре');
  const calenderEvent = await this.browser.$(
    '//div[@class="rbc-events-container"] //span[contains(text(), "' + taskName + '")]',
  );
  await calenderEvent.waitForExist({ timeout: 5000, interval: 500 });
  //
  assert.isTrue(
    await calenderEvent.isExisting(),
    'Активность с указанным названием отсутствует в календаре',
  );
};
