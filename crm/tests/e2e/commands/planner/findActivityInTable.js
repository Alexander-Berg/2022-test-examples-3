const { PlannerLocators } = require('./../../pages/locators/planner');

//функция находит строку в таблице,
//в которой содержится активность с указанным названием
module.exports = async function(searchText) {
  //если строк нет, то сразу выдать false
  const emptyTable = await this.$(PlannerLocators.EDIT_BUTTON);
  if (!emptyTable.isDisplayed()) {
    return false;
  }

  //если строки есть, то собрать массив из строк таблицы
  const rows = await this.$$(PlannerLocators.TABLE_ROW);
  //для каждой строки
  for (let i = 0, length = rows.length; i < length; i++) {
    const row = rows[i];

    //взять текст названия активности из строки
    const nameTextCell = await row.$(PlannerLocators.NAME_IN_TABLE_ROW);
    const nameText = await nameTextCell.getText();
    //если он совпадает с искомым, вернуть true
    if (nameText === searchText) {
      return true;
    }
  }
  //если активность с заданным текстом не найдена, вернуть false
  return false;
};
