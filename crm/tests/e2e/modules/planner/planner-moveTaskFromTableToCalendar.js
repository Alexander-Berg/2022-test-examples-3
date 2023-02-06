//перемещение задачи из таблицы в календарь через смену дат

const { assert } = require('chai');
const { PlannerLocators } = require('../../pages/locators/planner');
const taskName = `e2e-test322 ${Math.random()}`;
const deadlineTime = '23:59';

//сегодняшняя дата
const currentDate = new Date();
const month = currentDate.getMonth() + 1;
const day = currentDate.getDate();
const year = currentDate.getFullYear();
const today = String(day).padStart(2, '0') + '.' + String(month).padStart(2, '0') + '.' + year;

module.exports = async function() {
  //
  await this.browser.setMeta('1', 'ожидать появления на странице заголовка таблицы "Дела на день"');
  const tableTitle = await this.browser.$(PlannerLocators.TABLE_TITLE);
  await tableTitle.waitForDisplayed({ timeout: 5000, interval: 500 });
  //
  await this.browser.setMeta('2', 'ожидать отображения кнопки редактирования активности в таблице');
  const editButton = await this.browser.$(PlannerLocators.EDIT_BUTTON);
  await this.browser.pause(1000);
  const editButtonIsDisplayed = await editButton.isDisplayed();
  //
  await this.browser.setMeta(
    '3',
    'если в таблице на сегодня нет активностей, то создать новую задачу с дедлайном сегодня',
  );
  if (!editButtonIsDisplayed) {
    await this.browser.createActivityInTable(taskName, today, deadlineTime);
  }
  //
  await this.browser.setMeta('4', 'нажать Редактировать в блоке с первой активностью');
  await editButton.waitForDisplayed();
  await editButton.click();
  //
  await this.browser.setMeta('5', 'ожидать появления модального окна редактирования');
  const editActivityForm = await this.browser.$(PlannerLocators.EDIT_ACTIVITY_FORM);
  await editActivityForm.waitForDisplayed({ timeout: 5000, interval: 500 });
  //
  await this.browser.setMeta('6', 'запомнить старое значение поля Название');
  const activityName = await this.browser.$(PlannerLocators.ACTIVITY_NAME_FIELD);
  await activityName.waitForDisplayed();
  const namePrevValue = await activityName.getValue();
  //
  await this.browser.setMeta('7', 'очистить Дедлайн');
  const clearDeadline = await this.browser.$(PlannerLocators.DEADLINE_DATE_CLEAR);
  await clearDeadline.click();
  //
  await this.browser.setMeta('8', 'установить Дату начала');
  const startDateChange = await this.browser.$(PlannerLocators.START_DATE_INPUT);
  await startDateChange.setValue(today); //сегодняшняя дата
  //
  await this.browser.setMeta('9', 'установить время для Даты начала через выбор из списка');
  const startDateChangeHour = await this.browser.$(PlannerLocators.START_TIME_INPUT);
  await startDateChangeHour.click();
  await (await this.browser.$('[data-key="item-2"]')).click(); //выбрать 01:00
  //
  await this.browser.setMeta('10', 'установить дату окончания');
  const endDateChange = await this.browser.$(PlannerLocators.END_DATE_INPUT);
  await endDateChange.setValue(today); //сегодняшняя дата
  //
  await this.browser.setMeta('11', 'установить время для Даты окончания');
  const endDateChangeHour = await this.browser.$(PlannerLocators.END_TIME_INPUT);
  await endDateChangeHour.click();
  await (await this.browser.$(PlannerLocators.END_TIME_CLEAR)).click();
  await endDateChangeHour.setValue('02:00'); //выбрать 02:00

  //
  await this.browser.setMeta('12', 'нажать Сохранить');
  const saveTask = await this.browser.$(PlannerLocators.SAVE_ACTIVITY_BUTTON);
  await saveTask.waitForClickable();
  await saveTask.click();
  //
  await this.browser.setMeta('13', 'подождать перемещения в календарь');
  await this.browser.pause(2000);
  //
  await this.browser.setMeta('14', 'проверить, что активность теперь отображается в календаре');
  const calenderEvent = await this.browser.$(
    '//div[@class="rbc-events-container"] //span[contains(text(), "' + namePrevValue + '")]',
  );
  await calenderEvent.waitForExist({ timeout: 5000, interval: 500 });
  //
  assert.isTrue(
    await calenderEvent.isExisting(),
    'Активность с указанным названием отсутствует в календаре',
  );
};
