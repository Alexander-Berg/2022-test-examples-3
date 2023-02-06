export class ChatsSelectors {
  // Кнопка создания нового чата
  static newChatBtn = '[data-testid="chats__new-chat-btn"]'
  // Кликабельная строка в таблице чатов
  static tableRow(i: string | number) {
    return `[data-testid="chats__table-row-${i}"]`
  }
}
