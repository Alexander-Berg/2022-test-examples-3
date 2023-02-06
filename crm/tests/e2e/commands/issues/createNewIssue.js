const { IssuesLocators } = require('./../../pages/locators/issues');

module.exports = async function(name) {
  // ввести название задачи в поле Новая задача
  const newIssueInput = await this.$(IssuesLocators.INPUT_NEW_ISSUE);
  await newIssueInput.setValue(name);
  // нажать кнопку Создать
  const createIssue = await this.$(IssuesLocators.CREATE_ISSUE_BUTTON);
  await createIssue.waitForClickable();
  await createIssue.click();
  // подождать, пока список задач обновится
  await this.pause(1000);
  // кликнуть на первую задачу в списке
  const latestIssue = await this.$(IssuesLocators.LATEST_CREATED_ISSUE);
  await latestIssue.click();
  // дождаться отображения поля Исполнитель
  const attributesOpened = await this.$(IssuesLocators.ASSIGNEE_FIELD);
  await attributesOpened.waitForDisplayed();
};
