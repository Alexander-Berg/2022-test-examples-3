const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function(ticketNameToFind) {
  //делаем массив доступных на странице названий тикетов
  const ticketsSummary = await this.$$(TicketsLocators.TICKET_LIST_BLOCK_NAME);

  //среди них ищем указанный в параметре ticketNameToFind
  for (let i = 0, length = ticketsSummary.length; i < length; i++) {
    const ticketName = await ticketsSummary[i].getText(); //название одного тикета
    //если название найдено, вернуть значение атрибута
    if (ticketName === ticketNameToFind) {
      return ticketName;
    }
  }
  return false;
};
