const { TicketsLocators } = require('./../../pages/locators/tickets');

module.exports = async function(attributeToGet) {
  //делаем список доступных атрибутов
  const attributesForCheck = await this.$$(TicketsLocators.TICKET_ATTRIBUTE_KEY_VALUE);

  //среди них ищем указанный в параметре
  for (let i = 0, length = attributesForCheck.length; i < length; i++) {
    const attrLine = attributesForCheck[i]; //полная строка атрибута
    //название атрибута
    const attrLabel = await (await attrLine.$(TicketsLocators.TICKET_ATTRIBUTE_LABEL)).getText();
    //значение атрибута
    const attrValue = await (await attrLine.$(TicketsLocators.TICKET_ATTRIBUTE_VALUE)).getText();
    //если название найдено, вернуть значение атрибута
    if (attrLabel === attributeToGet) {
      return attrValue;
    }
  }
};
