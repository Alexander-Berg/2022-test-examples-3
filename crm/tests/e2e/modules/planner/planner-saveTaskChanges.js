//измененные данные в активности должны сохраняться по кнопке Сохранить

const { assert } = require('chai');
const { PlannerLocators } = require('../../pages/locators/planner');
const taskName = `111e2e-test322 ${Math.random()}`;

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
    const deadlineTime = '23:59';
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
  await activityName.waitForDisplayed();
  const namePrevValue = await activityName.getValue();
  //
  await this.browser.setMeta('7', 'запомнить текущее Описание');
  const activityDescription = await this.browser.$(PlannerLocators.DESCRIPTION_INPUT);
  await activityDescription.waitForDisplayed();
  const descriptionPrevValue = await activityDescription.getValue();
  //
  await this.browser.setMeta('8', 'запомнить текущее значение даты Дедлайна');
  const deadlineDate = await this.browser.$(PlannerLocators.DEADLINE_DATE_INPUT);
  const deadlineDatePrevValue = await deadlineDate.getValue();
  //
  await this.browser.setMeta('9', 'ввести измененное Название');
  const taskNameChanged = namePrevValue + ' Changed';
  await activityName.setValue(taskNameChanged);
  //
  await this.browser.setMeta('10', 'очистить и изменить время Дедлайна');
  await (await this.browser.$(PlannerLocators.DEADLINE_TIME_CLEAR)).click();
  await this.browser.pause(500);
  const deadlineTimeChanged = '22:40';
  const deadlineChangeTime = await this.browser.$(PlannerLocators.DEADLINE_TIME_INPUT);
  await deadlineChangeTime.setValue(deadlineTimeChanged);
  //
  await this.browser.setMeta('11', 'изменить Описание');
  const taskDescriptionChanged = descriptionPrevValue + ' Changed';
  await activityDescription.setValue(taskDescriptionChanged);
  //
  await this.browser.setMeta('12', 'нажать Сохранить');
  const saveTask = await this.browser.$(PlannerLocators.SAVE_ACTIVITY_BUTTON);
  await saveTask.waitForClickable();
  await saveTask.click();
  this.browser.refresh();

  //
  await this.browser.setMeta('13', 'проверить, что название отобразилось в таблице');
  //массив отображаемых в таблице активностей
  const rows = await this.browser.$$(PlannerLocators.TABLE_ROW);
  //для каждой строки
  for (let i = 0, length = rows.length; i < length; i++) {
    const row = rows[i];
    //достать текст из названия активности в строке
    const text = await (
      await row.$(
        'div[data-unstable-testid="Cell"]:nth-of-type(2) span[data-unstable-testid="Text"]',
      )
    ).getText();
    //если он совпадает с новым значением, то убедиться, что остальные значения были изменены
    if (text === taskNameChanged) {
      //
      await this.browser.setMeta('14', 'нажать на кнопку редактирования в строке');
      await (
        await row.$('div[data-unstable-testid="Cell"] div[data-unstable-testid="EditButton"]')
      ).click();
      //
      await this.browser.setMeta('15', 'проверить, что значения полей изменились');
      //название
      await activityName.waitForDisplayed({ timeout: 5000, interval: 500 });
      const currentName = await activityName.getValue();
      assert.strictEqual(currentName, taskNameChanged, 'Не сохранилось измененное Название');
      //дата Дедлайна
      const currentDeadline = await deadlineDate.getValue();
      assert.strictEqual(
        currentDeadline,
        deadlineDatePrevValue,
        'Дата Дедлайна изменилась, хотя ее не меняли',
      );
      //время Дедлайна
      const currentDeadlineTime = await deadlineChangeTime.getValue();
      assert.equal(
        currentDeadlineTime,
        deadlineTimeChanged,
        'Не сохранилось измененное время Дедлайна',
      );
      //Описание
      const currentDescription = await activityDescription.getValue();
      assert.strictEqual(
        currentDescription,
        taskDescriptionChanged,
        'Не сохранилось измененное Описание',
      );

      //
      await this.browser.setMeta('16', 'закрыть форму по крестику');
      const closeForm = await this.browser.$(PlannerLocators.CLOSE_CREATE_FORM);
      await closeForm.click();
    }
  }
};
