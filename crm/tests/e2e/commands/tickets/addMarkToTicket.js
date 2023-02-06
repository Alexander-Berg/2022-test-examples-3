const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function(markName, markOwner) {
  //нажать на "Добавить метку" в тикете
  const addMark = await this.$(TicketsLocators.ADD_MARK_BUTTON);
  await addMark.waitForClickable();
  await addMark.click();

  //нажать на "Новая метка"
  const newMark = await this.$(TicketsLocators.NEW_MARK_BUTTON);
  await newMark.waitForClickable();
  await newMark.click();

  //ввести название метки
  const newMarkName = await this.$(TicketsLocators.NEW_MARK_NAME);
  await newMarkName.waitForDisplayed();
  await newMarkName.setValue(markName);

  //добавить доступ до этой метки для указанного пользователя и нажать Enter
  const newMarkAccess = await this.$(TicketsLocators.NEW_MARK_ACCESS);
  await newMarkAccess.setValue([markOwner, 'Enter']);

  //дождаться появления списка доступных пользователей по введенному тексту
  const optionAccess = await this.$(TicketsLocators.OPTION_FOR_ACCESS);
  await optionAccess.click();

  //установить зеленый цвет для метки
  const greenColour = await this.$(TicketsLocators.NEW_MARK_GREEN_COLOUR);
  await greenColour.click();

  //сохранить метку
  const saveMark = await this.$(TicketsLocators.SAVE_NEW_MARK);
  await saveMark.waitForClickable();
  await saveMark.click();

  //   // выйти из окна редактирования метки
  //   await (await this.$(TicketsLocators.TICKET_TAGS_INPUT)).setValue('Escape');
  await this.pause(500);
  return markName;
};
