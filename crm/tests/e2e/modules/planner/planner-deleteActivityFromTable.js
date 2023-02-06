//удаление активности из таблицы

const { assert } = require('chai');
const { PlannerLocators } = require('../../pages/locators/planner');
const taskName = `e2e-test320 ${Math.random()}`;
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
  await this.browser.setMeta('2', 'создать новую задачу с дедлайном сегодня');
  await this.browser.createActivityInTable(taskName, today, deadlineTime);
  //
  await this.browser.setMeta('3', 'нажать Удалить в строке с созданной задачей');
  const deleteButton = await this.browser.$(PlannerLocators.DELETE_BUTTON);

  const rows = await this.browser.$$(PlannerLocators.TABLE_ROW);
  for (let i = 0, length = rows.length; i < length; i++) {
    const row = rows[i];
    //взять название активности из строки
    const nameTextCell = await row.$(PlannerLocators.NAME_IN_TABLE_ROW);
    const nameText = await nameTextCell.getText();
    //если оно совпадает с искомым
    if (nameText === taskName) {
      //нажать Удалить в этой строке
      const removeButton = await this.browser.$(
        'div[data-testid="Table"] div[class="M3qOGNtZ_a1-fJs47nj3_"] div[data-unstable-testid="Row"]:nth-of-type(' +
          (i + 1) +
          ') div[data-unstable-testid="Cell"] div[data-testid="removeRowButton"]',
      );
      await removeButton.click();
      //отклонить алерт
      await this.browser.pause(500);
      await this.browser.setMeta('4', 'отклонить удаление в появившемся алерте');
      await this.browser.dismissAlert();
    }
  }

  //
  await this.browser.setMeta('5', 'перезагрузить страницу');
  await this.browser.refresh();
  await deleteButton.waitForDisplayed({ timeout: 5000, interval: 500 });
  //
  await this.browser.setMeta('6', 'проверить, что задача с названием из пункта 4 есть в списке');
  const activityIsFound = await this.browser.findActivityInTable(taskName);
  assert.isTrue(activityIsFound, 'activity was deleted on dismissing alert');

  //
  await this.browser.setMeta('7', 'нажать Удалить в блоке с задачей');

  for (let i = 0, length = rows.length; i < length; i++) {
    const row = rows[i];
    //взять название активности из строки
    const nameTextCell = await row.$(PlannerLocators.NAME_IN_TABLE_ROW);
    const nameText = await nameTextCell.getText();
    //если он совпадает с искомым
    if (nameText === taskName) {
      //нажать Удалить в этой строке
      const removeButton = await this.browser.$(
        'div[data-testid="Table"] div[class="M3qOGNtZ_a1-fJs47nj3_"] div[data-unstable-testid="Row"]:nth-of-type(' +
          (i + 1) +
          ') div[data-unstable-testid="Cell"] div[data-testid="removeRowButton"]',
      );
      await removeButton.click();
      //принять алерт
      await this.browser.pause(500);
      await this.browser.setMeta('8', 'принять удаление в появившемся алерте');
      await this.browser.acceptAlert();
    }
  }

  //
  await this.browser.setMeta('9', 'перезагрузить страницу');
  await this.browser.refresh();
  await tableTitle.waitForDisplayed({ timeout: 5000, interval: 500 });
  await this.browser.pause(1000);

  await this.browser.setMeta('10', 'проверить, что задачи с таким названием нет в таблице');
  const taskIsFound = await this.browser.findActivityInTable(taskName);
  assert.isFalse(taskIsFound, 'задача не была удалена из таблицы');
};
