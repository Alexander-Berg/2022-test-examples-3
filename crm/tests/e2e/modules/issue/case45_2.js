const { assert } = require('chai');

module.exports = async function() {
  const { browser } = this;

  //предварительно:
  //список задач отсортирован по дате создания
  //создана новая задача
  //переход на эту задачу

  // процедура setDeadlineInIssues (возвращает дату дедлайна)
  // устанавливает дату дедлайна и проверяет,
  // что в первой по списку задаче пропала иконка календарика

  // если возвращенная дата null или undefined, значит, она не была изменена
  const isDeadlineEdited = await browser.setDeadlineInIssues();
  assert.isTrue(Boolean(isDeadlineEdited), 'deadline was not edited');
};
