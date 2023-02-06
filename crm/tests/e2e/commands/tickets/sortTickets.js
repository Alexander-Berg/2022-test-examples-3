const { IssuesLocators } = require('../../pages/locators/issues');

module.exports = async function(sortName) {
  // нажать кнопку сортировки
  const sorter = await this.$(IssuesLocators.SORT_ISSUES_LIST);
  await sorter.waitForDisplayed();
  await sorter.click();
  await this.pause(500);
  // выбрать конкретную сортировку
  const sortElement = await this.$('//span[text()="' + sortName + '"]');
  await sortElement.waitForDisplayed({ timeout: 5000, interval: 500 });
  await sortElement.click({ x: 5, y: 5 });
  // подождать, пока она применится
  await this.pause(2000);
};
