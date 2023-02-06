const { assert } = require('chai');

module.exports = async function() {
  const { browser } = this;
  await browser.setMeta('1', 'найти значение поля Исполнитель');

  const isAssignee = await browser.getAttributeValue('Исполнитель');
  assert.equal('CRM Space Odyssey Robot', isAssignee, 'assignee is not our current user');
};
