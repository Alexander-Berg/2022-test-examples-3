const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function() {
  //Нажать на кнопку Дедлайн в атрибутах
  const deadlineField = await this.$(IssuesLocators.DEADLINE_FIELD);
  await deadlineField.waitForDisplayed();
  await deadlineField.click();

  //Увидеть поле ввода дедлайна
  const deadlineInput = await this.$(IssuesLocators.DEADLINE_INPUT);
  await deadlineInput.waitForDisplayed();
  //await deadlineInput.setValue('Enter');  //ввод даты по умолчанию

  //выбрать дату, последнюю доступную в открывшемся в календаре
  const deadlineDate = await this.$(IssuesLocators.DEADLINE_CALENDER_LAST_DATE);
  await deadlineDate.waitForDisplayed();
  await deadlineDate.click();

  //дождаться, пока в тикете пропадет иконка календарика (в краткой информации в списке тикетов)
  await this.$(IssuesLocators.CALENDAR_ICON).waitForDisplayed({
    timeout: 10000,
    interval: 500,
    reverse: true,
  });

  // забрать установленное значение атрибута Дедлайн
  const deadlineValue = await this.getAttributeValue('Дедлайн');
  // и если оно не пустое, то вернуть это значение
  if (deadlineValue) return deadlineValue;
  return false;
};
