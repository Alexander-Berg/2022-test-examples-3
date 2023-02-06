//изменения в активности не должны применяться,
//если форма редактирования закрывается по крестику

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
    'если в таблице на сегодня нет активностей (кнопка не отображается), то создать новую задачу с дедлайном сегодня',
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
  await this.browser.setMeta('6', 'запомнить текущее Название');
  const activityName = await this.browser.$(PlannerLocators.ACTIVITY_NAME_FIELD);
  const namePrevValue = await activityName.getValue();
  //
  await this.browser.setMeta('7', 'запомнить текущее значение Дедлайна');
  const deadlineDate = await this.browser.$(PlannerLocators.DEADLINE_DATE_INPUT);
  const deadlinePrevValue = await deadlineDate.getValue();
  //
  await this.browser.setMeta('8', 'запомнить текущее значение Даты начала');
  const startDateChange = await this.browser.$(PlannerLocators.START_DATE_INPUT);
  const startDatePrevValue = await startDateChange.getValue();
  //
  await this.browser.setMeta('9', 'запомнить текущее значение Даты окончания');
  const endDateChange = await this.browser.$(PlannerLocators.END_DATE_INPUT);
  const endDatePrevValue = await endDateChange.getValue();

  // //
  // await this.browser.setMeta('10', 'удалить название');
  // await activityName.waitForDisplayed();
  // await activityName.setValue('');
  // //
  // await this.browser.setMeta('11', 'нажать на кнопку Сохранить');
  // const saveTask = await this.browser.$(PlannerLocators.SAVE_ACTIVITY_BUTTON);
  // await saveTask.waitForClickable();
  // await saveTask.click();
  // //
  // await this.browser.setMeta(
  //   '12',
  //   'дождаться появления попапа с текстом о том, что нельзя сохранить задачу без названия',
  // );
  // const saveActivityWithoutNamePopup = await this.browser.$(PlannerLocators.FIRST_POPUP_IN_LIST);
  // await saveActivityWithoutNamePopup.waitForDisplayed({ timeout: 5000, interval: 500 });
  // const popupActivityWithoutNameText = await saveActivityWithoutNamePopup.getText();
  // //
  // await this.browser.setMeta('13', 'проверяем, что текст в попапе верный');
  // assert.include(popupActivityWithoutNameText, 'Не заполнено поле Название');

  //
  await this.browser.setMeta('10', 'ввести измененное Название');
  await activityName.waitForDisplayed();
  const taskNameChanged = namePrevValue + ' Changed';
  await activityName.setValue(taskNameChanged);
  //
  await this.browser.setMeta('11', 'очистить дедлайн');
  const clearDeadline = await this.browser.$(PlannerLocators.DEADLINE_DATE_CLEAR);
  await clearDeadline.click();
  //
  await this.browser.setMeta('12', 'установить дату начала');
  await startDateChange.setValue([today, 'Escape']);
  //
  await this.browser.setMeta('13', 'установить дату окончания');
  await endDateChange.setValue([today, 'Escape']);
  //
  await this.browser.setMeta('14', 'закрыть окно редактирования (без сохранения)');
  const closeForm = await this.browser.$(PlannerLocators.CLOSE_CREATE_FORM);
  await closeForm.click();

  //
  await this.browser.setMeta(
    '15',
    'проверить, что название активности в первой строке не изменилось',
  );
  const isNameFound = await (
    await this.browser.$(PlannerLocators.ACTIVITY_NAME_IN_FIRST_TABLE_ROW)
  ).getText();
  assert.strictEqual(isNameFound, namePrevValue, 'Название активности в первой строке изменилось');

  //
  await this.browser.setMeta('16', 'снова нажать Редактировать в блоке с первой активностью');
  await editButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  await editButton.click();
  //
  await this.browser.setMeta('17', 'проверить, что остались предыдущие значения полей');

  //
  await this.browser.setMeta('18', 'Название');
  await activityName.waitForDisplayed({ timeout: 5000, interval: 500 });
  const currentName = await activityName.getValue();
  assert.strictEqual(
    currentName,
    namePrevValue,
    'После закрытия формы по крестику сохранилось измененное Название',
  );
  //
  await this.browser.setMeta('19', 'Дедлайн');
  const currentDeadline = await deadlineDate.getValue();
  assert.strictEqual(
    currentDeadline,
    deadlinePrevValue,
    'После закрытия формы по крестику сохранился измененный Дедлайн',
  );
  //
  await this.browser.setMeta('20', 'Дата начала');
  const currentStartDate = await startDateChange.getValue();
  assert.strictEqual(
    currentStartDate,
    startDatePrevValue,
    'После закрытия формы по крестику сохранилась измененная Дата начала',
  );
  //
  await this.browser.setMeta('21', 'Дата окончания');
  const currentEndDate = await endDateChange.getValue();
  assert.strictEqual(
    currentEndDate,
    endDatePrevValue,
    'После закрытия формы по крестику сохранилась измененная Дата окончания',
  );

  //
  await this.browser.setMeta('22', 'закрыть форму по крестику');
  await closeForm.click();
};
