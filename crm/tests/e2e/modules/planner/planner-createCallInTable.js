//создание Звонка с валидациями в календаре в Планировщике

const { assert } = require('chai');
const { PlannerLocators } = require('../../pages/locators/planner');
const callName = `e2e-test316 ${Math.random()}`;

//сегодняшняя дата
const currentDate = new Date();
const month = currentDate.getMonth() + 1;
const day = currentDate.getDate();
const year = currentDate.getFullYear();
const today = String(day).padStart(2, '0') + '.' + String(month).padStart(2, '0') + '.' + year;
const deadlineTime = '23:59';

module.exports = async function() {
  const { browser } = this;
  //
  await browser.setMeta('1', 'нажать кнопку "Запланировать активность"');
  const scheduleActivity = await browser.$(PlannerLocators.SCHEDULE_ACTIVITY);
  await scheduleActivity.waitForDisplayed();
  await scheduleActivity.click();
  //
  await browser.setMeta('2', 'выбрать "Звонок"');
  const callActivity = await browser.$(PlannerLocators.CALL_ACTIVITY);
  await callActivity.waitForClickable();
  await callActivity.click();
  //
  await browser.setMeta('3', 'дождаться появления окна создания звонка');
  const activityCreateForm = await browser.$(PlannerLocators.ACTIVITY_CREATE_FORM);
  await activityCreateForm.waitForDisplayed();
  //
  await browser.setMeta('4', 'проверить, что все нужные поля на форме присутствуют');
  await (await browser.$(PlannerLocators.CREATE_FORM_NAME)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_DEADLINE)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_START_DATETIME)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_END_DATETIME)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_DESCRIPTION)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_DONE)).waitForDisplayed();
  await (await browser.$(PlannerLocators.CREATE_FORM_OPPORTUNITIES)).waitForDisplayed();
  //
  await browser.setMeta('5', 'удалить дату окончания');
  const clearEndDate = await browser.$(PlannerLocators.END_DATE_CLEAR);
  await clearEndDate.waitForClickable();
  await clearEndDate.click();
  //
  await browser.setMeta('6', 'удалить дату начала');
  const clearStartDate = await browser.$(PlannerLocators.START_DATE_CLEAR);
  await clearStartDate.waitForClickable();
  await clearStartDate.click();
  //
  await browser.setMeta('7', 'установить дату дедлайна');
  const setDeadline = await browser.$(PlannerLocators.DEADLINE_DATE_INPUT);
  await setDeadline.waitForClickable();
  await setDeadline.setValue(today);
  //
  await browser.setMeta('8', 'нажать на кнопку Сохранить');
  const saveCall = await browser.$(PlannerLocators.SAVE_ACTIVITY_BUTTON);
  await saveCall.waitForClickable();
  await saveCall.click();
  await browser.pause(1000);

  //
  await browser.setMeta(
    '9',
    'дождаться появления попапа с текстом о том, что дата дедлайна не должна быть меньше даты звонка',
  );
  const saveCallWithWrongDeadlinePopup = await browser.$(PlannerLocators.FIRST_POPUP_IN_LIST);
  await saveCallWithWrongDeadlinePopup.waitForDisplayed({ timeout: 5000, interval: 500 });
  const popupWrongDeadlineText = await saveCallWithWrongDeadlinePopup.getText();
  //
  await browser.setMeta('10', 'проверить, что текст в попапе верный');
  assert.include(popupWrongDeadlineText, 'Дедлайн не может быть в прошлом');
  //
  await browser.setMeta('11', 'очистить время дедлайна и установить его на 23:59 выбранной даты');
  await (await browser.$(PlannerLocators.DEADLINE_TIME_CLEAR)).click();
  await browser.pause(500);
  const setDeadlineTime = await browser.$(PlannerLocators.DEADLINE_TIME_INPUT);
  await setDeadlineTime.setValue(deadlineTime);

  //
  await browser.setMeta('12', 'нажать на кнопку Сохранить');
  await saveCall.waitForClickable();
  await saveCall.click();
  //
  await browser.setMeta(
    '13',
    'дождаться появления попапа с текстом о том, что нельзя сохранить звонок без названия',
  );
  const saveCallWithoutNamePopup = await browser.$(PlannerLocators.SECOND_POPUP_IN_LIST);
  await saveCallWithoutNamePopup.waitForDisplayed({ timeout: 5000, interval: 500 });
  const popupCallWithoutNameText = await saveCallWithoutNamePopup.getText();
  //
  await browser.setMeta('14', 'проверить, что текст в попапе верный');
  assert.include(popupCallWithoutNameText, 'Не заполнено поле Название');
  //
  await browser.setMeta('15', 'вводим название звонка');
  const inputCallName = await browser.$(PlannerLocators.ACTIVITY_NAME_FIELD);
  await inputCallName.setValue(callName);
  //
  await browser.setMeta('16', 'нажать кнопку "Сохранить"');
  await saveCall.waitForClickable();
  await saveCall.click();
  //
  await browser.setMeta('17', 'дождаться, пока окно создания исчезнет');
  await activityCreateForm.waitForDisplayed({
    timeout: 5000,
    interval: 500,
    reverse: true,
  });

  await browser.pause(1000);
  //
  await browser.setMeta('18', 'в таблице найти блоки с названием созданных активностей');
  const tableRows = await browser.$$(PlannerLocators.TABLE_ROW);
  let isCallFound = false;
  for (let i = 0, length = tableRows.length; i < length; i++) {
    const tableRow = tableRows[i];
    const textNode = await tableRow.$(PlannerLocators.NAME_IN_TABLE_ROW);
    const text = await textNode.getText();
    if (text === callName) {
      isCallFound = true;
      return;
    }
  }

  //
  await browser.setMeta('19', 'найти созданный звонок по названию');
  assert.isTrue(isCallFound, 'звонок не найден');
};
